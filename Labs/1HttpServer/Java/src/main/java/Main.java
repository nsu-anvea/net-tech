import httpServer.Server;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Too few arguments.");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String directory = args[1];

        Server server = new Server(port, directory);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}
