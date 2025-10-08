package httpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private final int port;
    private final String directory;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public Server(int port, String directory) {
        this.port = port;
        this.directory = directory;
    }

    public void start() {
        if (isRunning.get()) {
            throw new IllegalStateException("Server is already running");
        }

        isRunning.set(true);

        try (var server = new ServerSocket(port)) {
            serverSocket = server;
            System.out.println("Server started on port " + port);

            while (isRunning.get()) {
                Socket socket = server.accept();
                Handler handler = new Handler(socket, directory);
                handler.start();
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            System.out.println("Server stopped");
            isRunning.set(false);
        }
    }

    public void stop() {
        if (!isRunning.get()) return;

        System.out.println("Stopping server gracefully...");
        isRunning.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }
}