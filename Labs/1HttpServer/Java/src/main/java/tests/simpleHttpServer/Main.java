package tests.simpleHttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream out = clientSocket.getOutputStream()) {

                // Читаем заголовки запроса
                String line;
                int contentLength = 0;
                while (!(line = in.readLine()).isEmpty()) {
                    System.out.println(line); // Логируем заголовки
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                    }
                }

                // Читаем тело POST-запроса, если есть
                if (contentLength > 0) {
                    char[] body = new char[contentLength];
                    in.read(body, 0, contentLength);
                    String requestBody = new String(body);
                    System.out.println("POST Body: " + requestBody);

                    // Формируем ответ
                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "\r\n" +
                            "Received POST data: " + requestBody;
                    out.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
