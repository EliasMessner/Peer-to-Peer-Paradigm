import java.io.IOException;

/**
 * Die Main-Klasse des Programms.
 * @author Elias Messner, Marius Gerull
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        String[] locations = {
                "Dublin",
                "Beijing",
                "Sydney",
                "Berlin",
                "Washington"
        };

        Client c1 = new Client(50000, 50010);
        c1.startInOwnThread(locations[(int)(Math.random() * locations.length)]);
        Client c2 = new Client(50000, 50010);
        c2.startInOwnThread(locations[(int)(Math.random() * locations.length)]);
        Thread.sleep(30000);
        c1.stop();
        Client c3 = new Client(50000, 50010);
        c3.startInOwnThread(locations[(int)(Math.random() * locations.length)]);
        Thread.sleep(5000);
        System.out.println("\n\n######\n\nc3 knows: \n"+c3.getKnownDataAsString()+"\n\n#######\n\n");
        Thread.sleep(1000000);
    }

}
