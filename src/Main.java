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

        Client c0 = new Client(50000, 50010);
        c0.startInOwnThread(locations[0]);
        Thread.sleep(2500);
        Client c1 = new Client(50000, 50010);
        c1.startInOwnThread(locations[1]);
        Thread.sleep(10000);
        System.out.println("c2 joining in 10 sec...");
        Thread.sleep(10000);
        c0.stop();
        Client c2 = new Client(50000, 50010);
        c2.startInOwnThread(locations[(int)(Math.random() * locations.length)]);
        Thread.sleep(5000);
        // System.out.println("\n\n######\n\nc2 knows: \n"+c2.getKnownDataAsString()+"\n\n#######\n\n");
        Thread.sleep(1000000);
    }

}
