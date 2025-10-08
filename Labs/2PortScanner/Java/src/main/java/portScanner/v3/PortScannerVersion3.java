package portScanner.v3;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScannerVersion3 {
    private static final String HOST = "localhost";
    private static final int THREAD_COUNT = 100;
    private static final int TIMEOUT = 1000;

    private static final Map<Integer, String> services = new HashMap<>();

    public static void main(String[] args) {
        loadServices();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<PortResult>> futures = new ArrayList<>();

        for (int port = 1; port <= 65535; port++) {
            futures.add(executor.submit(new PortChecker(port)));
        }

        executor.shutdown();

        for (Future<PortResult> future : futures) {
            try {
                PortResult result = future.get();
                if (result.isOpen) {
                    System.out.printf("Port %5d is opened - %s%n", result.port, result.service);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Scan completed.");
    }

    private static class PortResult {
        int port;
        boolean isOpen;
        String service;

        PortResult(int port, boolean isOpen, String service) {
            this.port = port;
            this.isOpen = isOpen;
            this.service = service;
        }
    }

    private static void loadServices() {
        Path servicesFile = Paths.get("/etc/services");

        if (!Files.exists(servicesFile)) {
            System.out.println("\"/etc/services\" is not found.");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(servicesFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                // http-alt	   8080/udp     # HTTP Alternate (see port 80)
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    return;
                }

                String[] portProto = parts[1].split("/");
                if (portProto.length != 2) {
                    return;
                }
                try {
                    int port = Integer.parseInt(portProto[0]);
                    String service = parts[0];
                    services.putIfAbsent(port, service);
                } catch (NumberFormatException ignored) {}
            }
            System.out.println(services.size() + " services were loaded.");
        } catch (IOException e) {
            System.err.println("Couldn't read the services from /etc/services: " + e.getMessage());
        }
    }

    private record PortChecker(int port) implements Callable<PortResult> {
        @Override
            public PortResult call() {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(HOST, port), TIMEOUT);
                    String service = services.getOrDefault(port, "Unknown");
                    return new PortResult(port, true, service);
                } catch (IOException e) {
                    return new PortResult(port, false, "");
                }
            }
        }
}
