import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Klasse für einen Client. Verwaltet Verbindungen und ein- und ausgehende Datenpakete.
 * @author Elias Messner, Marius Gerull
 */
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

    /**
     * Konstruktor für einen Client. Sucht zwischen den übergebenen Parametern nach einem freien Port.
     * @param minPort untere Grenze für Portrange
     * @param maxPort obere Grenze für Portrange
     */
    public Client(int minPort, int maxPort) {
        this.knownData = new HashMap<Integer, String>();
        this.isRunning = false;
        this.portRange = new int[(maxPort - minPort) + 1];
        for (int i = minPort; i <= maxPort; i++) {
            System.out.println(i);
            portRange[i - minPort] = i;
        }
    }

    /**
     * Start-Methode für die Kommunikation zwischen Clients. Ruft alle nötigen Methoden auf.
     * @param location Standort des Clients
     * @throws IOException
     * @throws InterruptedException
     */
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

    /**
     * Startet neuen Thread für den Client und ruft start() mit dem Standort des Clients auf.
     * @param location Standort des Clients
     */
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

    /**
     * Erzeugt neuen Thread für Client und lässt ihn auf Anfragen warten.
     */
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

    /**
     *Sendet Anfragen an andere Clients innerhalb der Portrange.
     * @throws IOException
     */
    private void advertise() throws IOException {
        for (int port : portRange) {
            if (this.port == port) continue;
            requestWeatherInfo(port);
        }
    }

    /**
     * Bearbeitet ankommende Pakete anderer Clients und verwaltet diese.
     */
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

    /**
     * Übergibt anderen Clients auf einem bestimmten Port ein eigenes Datenpaket.
     * @param port
     * @throws IOException
     */
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

    /**
     * Ändert das Wetter am Standort des Clients.
     */
    private void changWeather() {
        String loc = this.weather.getLocation();
        this.weather = new WeatherInfo(loc);
        this.lastTimeWeatherChanged = new Date(); //now
    }

    /**
     * Sendet Anfragen für Wetter-Datenpakete an andere Clients am übergebenen Port.
     * @param port des gewünschten Clients.
     * @throws IOException
     */
    private void requestWeatherInfo(int port) throws IOException {
        if (!isRunning) return;
        byte [] req = ("hello".getBytes());
        try {
            DatagramPacket packet = new DatagramPacket(req, req.length, InetAddress.getByName("localhost"), port);
            ds.send(packet);
        }
        catch (Exception e) {
            System.out.println(this.port + ": Failed to send weather request to port "+ port);
        }
    }
    
    /**
     * Sucht nach offenem Port in der Portrange und connected zu diesem.
     * @return  boolean portFound (true, falls freier Port gefunden wurde)
     * @throws IOException
     */
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

    /**
     * Erstellt neuen ServerSocket und DatagramSocket mit übergebenem Port.
     * @param port
     * @return true, wenn das Erstellen der Sockets funktioniert hat.
     */
    private boolean checkPortAndTryConnect(int port) {
         try {
            ssocket = new ServerSocket(port);
            ssocket.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
             e.printStackTrace();
        }

        return false;
    }

    /**
     * Setzt einen boolean auf 'true', wenn die letzte Wetteränderung länger als 5min her ist.
     * @return boolean
     */
    private boolean checkIfTimeToChangeWeather() {
        if (lastTimeWeatherChanged == null)
            return false;
        return getDateDiff(lastTimeWeatherChanged, new Date(), TimeUnit.MINUTES) > 5;
    }

    /**
     * Gibt die Differenz von zwei übergebenen Daten zurück.
     * @param date1 Datum 1
     * @param date2 Datum 2
     * @param timeUnit  Zeit-Einheit der Daten
     * @return long Differenz der Daten in Millisekunden
     */
    private static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    /**
     * Wandelt ein Datenpakete in Strings um.
     * @return String aus ausgelesenen Daten
     */
    public String getKnownDataAsString() {
        StringBuilder result = new StringBuilder("");
        for(Integer i : this.knownData.keySet()) {
            result.append("\n"+i.toString()+knownData.get(i)+"\n");
        }
        return result.toString();
    }
}
