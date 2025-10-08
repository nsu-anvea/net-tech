package portScanner.v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortScannerVersion2 {
    private static final String HOST = "localhost";
    private static final int THREAD_COUNT = 50;
    private static final int TIMEOUT = 1000;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int port = 1; port <= 65535; port++) {
            final int currentPort = port;

            executor.execute(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(HOST, currentPort), TIMEOUT);
                    System.out.println("Port " + currentPort + " is opened.");
                } catch (IOException ignored) {}
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
