import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {

    private boolean isRunning;
    private int[] portRange;
    private InputStreamReader in;
    private BufferedReader br;
    private Socket socket;
    private ServerSocket ssocket;
    private DatagramSocket ds;
    private PrintWriter pw;
    private WeatherInfo weather;

    public Client(int minPort, int maxPort) {
       /* if (minPort < MIN_PORT_NUMBER || maxPort > MAX_PORT_NUMBER || minPort > maxPort) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }*/
        this.isRunning = false;
        this.portRange = new int[maxPort - minPort];
        for (int i = minPort; i <= maxPort; i++) {
            portRange[i - minPort] = i;
        }
    }

    private void start(String location) throws IOException {
        this.weather = new WeatherInfo(location);
        isRunning = true;
        while (isRunning) {
            advertise();
            discoverAndReply();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Client Thread started.");
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
    }

    private void requestWeatherInfo(int port) throws IOException {
        this.socket = new Socket("localhost", port);
        prepareIOCompounds(socket);
    }

    private void findOpenPortAndConnect() {
        for (int port : portRange) {
            checkPortAndTryConnect(port);
        }
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

    private void prepareIOCompounds(Socket socket) throws IOException {
        if (this.socket == null)
            throw new RuntimeException("initialize socket first");

        this.in = new InputStreamReader(socket.getInputStream());
        this.br = new BufferedReader(in);

        this.pw = new PrintWriter(socket.getOutputStream());
    }

    public String getLocation() {
        return weather.getLocation();
    }


}
