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
    HashMap<String, WeatherInfo> knownData; //<location, weatherInfo>

    /**
     * Konstruktor für einen Client. Sucht zwischen den übergebenen Parametern nach einem freien Port.
     * @param minPort untere Grenze für Portrange
     * @param maxPort obere Grenze für Portrange
     */
    public Client(int minPort, int maxPort) {
        this.knownData = new HashMap<>();
        this.isRunning = false;
        this.portRange = new int[(maxPort - minPort) + 1];
        for (int i = minPort; i <= maxPort; i++) {
            // System.out.println(i);
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
        this.knownData.put(location, this.weather);
        isRunning = true;
        startCommunicationThread();
        /*while (isRunning) {
            advertise();
            Thread.sleep(3000);
            if (checkIfTimeToChangeWeather())
                changWeather();
        }*/
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
     * erzeugt neuen Thread und hört auf Anfragen von anderen clients,
     * regelnmäßig advertising, also Anfragen an alle ports in portrange
     *
     * */
    private void startCommunicationThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    discoverAndReply();
                    // while(!discoverAndReply());
                    try {
                        advertise();
                        Thread.sleep(3000);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (checkIfTimeToChangeWeather())
                        changWeather();
                }
            }
        }).start();
        System.out.println(this.port + ": Communication Thread started.");
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
        // System.out.println(this.port+": advertised");
    }

    /**
     * Bearbeitet ankommende Pakete anderer Clients und verwaltet diese.
     */
    private boolean discoverAndReply() {
        byte[] data = new byte[ 1000000 ];
        DatagramPacket packet = new DatagramPacket( data, data.length ); //create packet with buffer size
        try {
            int timeoutBefore = ds.getSoTimeout();
            ds.setSoTimeout(5000); //5 sec
            ds.receive(packet);
            ds.setSoTimeout(timeoutBefore);
        }
        catch(java.net.SocketTimeoutException e){
            return false;
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        byte[] buffer = new byte[1024];
        try {
            buffer = packet.getData();
            String msg = new String(buffer, 0, packet.getLength());
            if (msg.equals("hello")) {
                sendAllWeatherData(packet.getPort());
                // System.out.println(this.port+": sent weather data to "+packet.getPort());
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        handleWeatherInfo(buffer);
        return true;

    }

    private void sendAllWeatherData(int destinationPort) throws IOException {
        for (String loc : this.knownData.keySet()){
            sendWeatherData(knownData.get(loc), destinationPort);
        }
    }

    /**
     * Übergibt anderen Clients auf einem bestimmten Port ein eigenes Datenpaket.
     * @param destinationPort
     * @throws IOException
     */
    private void sendWeatherData(WeatherInfo wi, int destinationPort) throws IOException {
        // System.out.println(this.port + ": Trying to send Weather to "+port);
        if (!isRunning) return;
        // byte [] msg = this.weather.toString().getBytes();
        byte [] msg = objectToBytes(wi);
        try {
            DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName("localhost"), destinationPort);
            ds.send(packet);
            // System.out.println(this.port + ": !!!Done sending weather object to "+port);
        }
        catch (Exception e) {
            // System.out.println(this.port + ": Failed to send weather Data to port "+ port);
            throw e;
        }
        // System.out.println(weather.toString());
    }



    private void handleWeatherInfo(byte[] bytes) {
        WeatherInfo wi = (WeatherInfo)bytesToObject(bytes);
        System.out.println("\n"+this.port + ": !!!got weather in "+wi.getLocation());
        if (this.knownData.get(wi.getLocation()) == null
                || this.knownData.get(wi.getLocation()).getTimestamp().before(wi.getTimestamp()))
            this.knownData.put(wi.getLocation(), wi);

        System.out.println("updated knownData");
        System.out.println("KNOWN DATA OF "+this.port+":\n"+this.getKnownDataAsString()+"\n");
    }


    /**
     * Ändert das Wetter am Standort des Clients.
     */
    private void changWeather() {
        String loc = this.weather.getLocation();
        this.weather = new WeatherInfo(loc);
        this.lastTimeWeatherChanged = new Date(); //now
        System.out.println(this.port+": Weather changed: "+weather.toString());
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
            // System.out.println("weather requested");
        }
        catch (Exception e) {
            System.out.println(this.port + ": Failed to send weather request to port "+ port);
        }
    }
    private byte[] objectToBytes(Object o) throws IOException {
        // o = (WeatherInfo)o;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(port);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            byte[] yourBytes = bos.toByteArray();
            return yourBytes;

        } catch (IOException e) {
            throw e;
            // e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
                throw ex;
            }
        }
        // return null;
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
                System.out.println(this.port + ": found open port ---> "+port);
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
        }
         catch(java.net.BindException e) {
             // port in use, move on
             return false;
        }
         catch (IOException e) {
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
        for(String loc : this.knownData.keySet()) {
            result.append("\n"+loc+knownData.get(loc)+"\n");
        }
        return result.toString();
    }
}
