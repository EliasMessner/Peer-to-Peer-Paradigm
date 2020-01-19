import sun.awt.windows.ThemeReader;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Client {

    private boolean isRunning;
    private int[] portRange;
    private Socket socket;
    private ServerSocket ssocket;
    private DatagramSocket ds;
    private WeatherInfo weather;
    Date lastTimeWeatherChanged;
    int port;
    HashMap<Integer, String> knownData;

    public Client(int minPort, int maxPort) {
       /* if (minPort < MIN_PORT_NUMBER || maxPort > MAX_PORT_NUMBER || minPort > maxPort) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }*/
        this.knownData = new HashMap<Integer, String>();
        this.isRunning = false;
        this.portRange = new int[(maxPort - minPort) + 1];
        for (int i = minPort; i <= maxPort; i++) {
            System.out.println(i);
            portRange[i - minPort] = i;
        }
    }

    public void start(String location) throws IOException, InterruptedException {
        if (!findOpenPortAndConnect()) {
            System.out.println("No Port available");
            return;
        }
        this.weather = new WeatherInfo(location);
        isRunning = true;
        startListeningThread();
        while (isRunning) {
            advertise();
            Thread.sleep(3000);
            if (checkIfTimeToChangeWeather())
                changWeather();
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void startInOwnThread(String location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    start(location);
                    System.out.println(port + ": Client connected");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Client Thread started.");
    }

    private void startListeningThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning)
                    discoverAndReply();
            }
        }).start();
        System.out.println(this.port + ": Listening Thread started.");
    }

    private void advertise() throws IOException {
        for (int port : portRange) {
            if (this.port == port) continue;
            requestWeatherInfo(port);
        }
    }

    private void discoverAndReply() {
        byte[] data = new byte[ 1000000 ];
        DatagramPacket packet = new DatagramPacket( data, data.length ); //create packet with buffer size
        try {
            ds.receive(packet);
        }
        catch(Exception e) {
            //timeout or other error
            e.printStackTrace();
            return;
        }
        byte[] buffer = new byte[1024];
        try {
            buffer = packet.getData();
            String msg = new String(buffer, 0, packet.getLength());
            if (msg.equals("hello")) {
                sendWeatherData(packet.getPort());
            }
            else {
                System.out.println(this.port + ": got weather from "+packet.getPort()+" :\n"+msg+"\n");
                this.knownData.put(new Integer(packet.getPort()), msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendWeatherData(int port) throws IOException {
        System.out.println(this.port + ": Trying to send Weather to "+port);
        if (!isRunning) return;
        byte [] msg = this.weather.toString().getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
            System.out.println(this.port + ": Done sending weather to "+port);
        }
        catch (Exception e) {
            System.out.println(this.port + ": Failed to send weather Data to port "+ port);
        }
        System.out.println(weather.toString());
    }

    private void changWeather() {
        String loc = this.weather.getLocation();
        this.weather = new WeatherInfo(loc);
        this.lastTimeWeatherChanged = new Date(); //now
    }

    private void requestWeatherInfo(int port) throws IOException {
        // System.out.println(this.port + ": Trying to request Weather from "+port);
        if (!isRunning) return;
        byte [] req = ("hello".getBytes());
        try {
            DatagramPacket packet = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
            // System.out.println(this.port + ": Done requesting weather from "+port);
        }
        catch (Exception e) {
            System.out.println(this.port + ": Failed to send weather request to port "+ port);
        }
    }

    private byte[] objectToBytes(Object o) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            byte[] yourBytes = bos.toByteArray();
            return yourBytes;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return null;
    }

    private Object bytesToObject(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return o;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return null;
    }

    private boolean findOpenPortAndConnect() throws IOException {
        System.out.println("trying to find open port...");
        boolean portFound = false;
        for (int port : portRange) {
            if (this.port == port) continue;
            System.out.println(port);
            if (checkPortAndTryConnect(port)) {
                this.port = port;
                portFound = true;
                System.out.println(this.port + ": Connected to "+port);
                break;
            }
        }
        return portFound;
    }

    private boolean checkPortAndTryConnect(int port) {
       /* if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }*/

        try {
            ssocket = new ServerSocket(port);
            ssocket.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {

        }

        return false;
    }

    public String getLocation() {
        return weather.getLocation();
    }

    private boolean checkIfTimeToChangeWeather() {
        if (lastTimeWeatherChanged == null)
            return false;
        return getDateDiff(lastTimeWeatherChanged, new Date(), TimeUnit.MINUTES) > 5;
    }

    private static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    public WeatherInfo getWeatherInfo() {
        return this.weather;
    }

    public String getKnownDataAsString() {
        StringBuilder result = new StringBuilder("");
        for(Integer i : this.knownData.keySet()) {
            result.append("\n"+i.toString()+knownData.get(i)+"\n");
        }
        return result.toString();
    }
}
