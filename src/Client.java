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
        this.portRange = new int[maxPort - minPort];
        for (int i = minPort; i <= maxPort; i++) {
            portRange[i - minPort] = i;
            knownData.put(new Integer(i), null);
        }
    }

    private void start(String location) throws IOException, InterruptedException {
        this.weather = new WeatherInfo(location);
        if (!findOpenPortAndConnect()) {
            System.out.println("No Port available");
            return;
        }
        isRunning = true;
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
                    start(location);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Client Thread started.");
    }

    private void startListeningThread(String location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                discoverAndReply();
            }
        }).start();
        System.out.println("Listening Thread started.");
    }

    private void advertise() throws IOException {
        /* pseudo:
        while true:
            for client in 1-10:
                send request for weather info
         */

        for (int port : portRange) {
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
                System.out.println("unusual msg: "+msg);
            }
        }
        catch (Exception e) {
            // it's not a request so it should be a status
            try {
                WeatherInfo we = (WeatherInfo)bytesToObject(buffer);
                this.knownData.put(packet.getPort(), we);
            }
            catch (Exception e2) {
                System.out.println("Not a valid Status Packet");
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
            System.out.println("Failed to send weather request to port "+ port);
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
        if (!isRunning) return;
        byte [] req = ("hello".getBytes());
        try {
            DatagramPacket packet = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
        }
        catch (Exception e) {
            System.out.println("Failed to send weather request to port "+ port);
        }
    }

    private boolean findOpenPortAndConnect() throws IOException {
        boolean portFound = false;
        for (int port : portRange) {
            if (checkPortAndTryConnect(port)) {
                this.port = port;
                this.socket = new Socket("localhost", port);
                portFound = true;
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
