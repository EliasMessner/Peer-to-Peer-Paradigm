
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Client zum initialiesieren und starten eines Clients
 */
public class startP2PClient {

    private static String[] locations = {
            "Dublin",
            "Beijing",
            "Sydney",
            "Berlin",
            "Washington"
    };
    private static Client client;

    /**
     * Main Finktion des programms
     * @param args  args[0] name und ggf location als arg[1], sonst zufällige location
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        String clientName = null;
        String location = null;

        switch (args.length) {
            case 2:
                if (isNumeric(location)) {
                    int index = Integer.parseInt(location);
                    location = locations[index];
                    break;
                }
                location = args[1];
            case 1:
                clientName = args[0];
        }

        if (clientName == null) {
            throw new IllegalArgumentException("Enter client name as arg[0]");
        }

        if (location == null) {
            location = getRandomLocation();
        }

        client = new Client(clientName, 50000, 50010);
        client.start(location);

    }

    /**
     * checkt ob ein String nur ziffern enthält
     * @param str zu prüfendeer String
     * @return true wenn gegebener String nur ziffern enthält
     */
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    /**
     * gibt eine zufällige location aus dem location Array zurück
     * @return location
     */
    public static String getRandomLocation() {
        return locations[(int) (Math.random() * locations.length)];
    }
}
