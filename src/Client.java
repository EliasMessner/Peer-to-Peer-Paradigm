public class Client {

    private boolean isRunning;

    public Client() {
        this.isRunning = false;
    }

    private void start() {
        isRunning = true;
        while (isRunning) {
            advertise();
            discoverAndReply();
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void startInOwnThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }).start();
        System.out.println("Client Thread started.");
    }

    private void advertise() {
        //TODo

        /* pseudo:
        while true:
            for client in 1-10:
                send request for weather info
         */
    }

    public void discoverAndReply() {
        //TODO

        /* pseudo:
        while true:
            listen for request
                request found -> send answer with weather info
         */
    }


}
