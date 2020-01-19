import java.io.IOException;

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
        Client c2 = new Client(50000, 50010);
        c2.startInOwnThread(locations[(int)(Math.random() * locations.length)]);
        c1.start(locations[(int)(Math.random() * locations.length)]);
        Thread.sleep(1000000000);

    }

}
