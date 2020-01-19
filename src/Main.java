public class Main {

    public static void main(String[] args) throws InterruptedException {
        String[] locations = {
                "Dublin",
                "Beijing",
                "Sydney",
                "Berlin",
                "Washington"
        };

        Client c1 = new Client(50000, 50010);
        c1.startInOwnThread(locations[(int)(Math.random() * locations.length + 1)]);
        Client c2 = new Client(50000, 50010);
        c2.startInOwnThread(locations[(int)(Math.random() * locations.length + 1)]);
        Thread.sleep(1000000000);

    }

}
