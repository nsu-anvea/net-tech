package portScanner.v1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PortScannerVersion1 {
    private static final String host = "localhost";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Too few arguments.");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            System.out.println("Port " + port + " is opened.");
        } catch (IOException e) {
            System.out.println("Port " + port + " is closed.");
        }
    }
}
