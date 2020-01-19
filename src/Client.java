public class Client {

    public Client() {
        //TODO
    }

    private void start() {
        //TODO
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
    }

    public void discoverAndReply() {
        //TODO
    }


}
