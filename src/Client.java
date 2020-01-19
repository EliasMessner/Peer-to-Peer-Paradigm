import sun.awt.windows.ThemeReader;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Client {

    private boolean isRunning;
    private int[] portRange;
    private Socket socket;
    private ServerSocket ssocket;
    private DatagramSocket ds;
    private WeatherInfo weather;
    int port;
    HashMap<Integer, WeatherInfo> knownData;

    public Client(int minPort, int maxPort) {
       /* if (minPort < MIN_PORT_NUMBER || maxPort > MAX_PORT_NUMBER || minPort > maxPort) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }*/
        this.isRunning = false;
        this.portRange = new int[(maxPort - minPort) + 1];
        for (int i = minPort; i <= maxPort; i++) {
            System.out.println(i);
            portRange[i - minPort] = i;
        }
    }

    private void start(String location) throws IOException, InterruptedException {
        this.weather = new WeatherInfo(location);
        isRunning = true;
        startListeningThread();
        while (isRunning) {
            advertise();
            Thread.sleep(3000);
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
                    if (!findOpenPortAndConnect()) {
                        System.out.println("No Port available");
                        return;
                    }
                    start(location);
                    System.out.println(port + ": Client Thread started.");
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
        //TODO

        /* pseudo:
        while true:
            listen for request
                request found -> send answer with weather info
         */
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
            if (msg == "hello") {
                sendWeatherData(packet.getPort());
            }
            else {
                System.out.println(this.port + ": unusual msg: "+msg);
            }
        }
        catch (Exception e) {
            // it's not a request so it should be a status
            try {
                WeatherInfo we = (WeatherInfo)bytesToObject(buffer);
                this.knownData.put(packet.getPort(), we);
            }
            catch (Exception e2) {
                System.out.println(this.port + ": received invalid status packet from "+packet.getPort());
            }
        }

    }

    private void sendWeatherData(int port) throws IOException {
        if (!isRunning) return;
        byte [] req = objectToBytes(this.weather);
        try {
            DatagramPacket packet = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
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

    private void requestWeatherInfo(int port) throws IOException {
        System.out.println(this.port + ": Trying to request Weather from "+port);
        if (!isRunning) return;
        byte [] req = ("hello".getBytes());
        try {
            DatagramPacket packet = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
            System.out.println(this.port + ": Done requesting weather from "+port);
        }
        catch (Exception e) {
            System.out.println(this.port + ": Failed to send weather request to port "+ port);
        }
    }

    private boolean findOpenPortAndConnect() throws IOException {
        System.out.println(this.port + ": trying to find open port...");
        boolean portFound = false;
        for (int port : portRange) {
            System.out.println(port);
            if (checkPortAndTryConnect(port)) {
                this.port = port;
                this.socket = new Socket("localhost", port);
                portFound = true;
                System.out.println(this.port + ": Connected to "+port);
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
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    public String getLocation() {
        return weather.getLocation();
    }


}
