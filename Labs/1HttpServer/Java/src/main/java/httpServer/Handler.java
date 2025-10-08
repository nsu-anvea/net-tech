package httpServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Handler extends Thread {
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        put("", "text/plain");
    }};
    private static final String NOT_FOUND_MESSAGE = "NOT FOUND";
    private static final String USER_FILES_PATH = "./files/userFiles";

    private String requestType;
    private String url;

    private final Socket socket;
    private final String directory;

    public Handler(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(input));

            processHeader(br);

            switch (requestType) {
                case "GET" -> {
                    Path filePath = Path.of(directory + url);

                    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                        var extension = this.getFileExtension(filePath);
                        var type = CONTENT_TYPES.get(extension);
                        var fileBytes = Files.readAllBytes(filePath);
                        this.sendHeader(output, 200, "OK", type, fileBytes.length);
                        output.write(fileBytes);
                    } else {
                        var type = CONTENT_TYPES.get("text");
                        this.sendHeader(output, 404, "Not Found", type, NOT_FOUND_MESSAGE.length());
                        output.write(NOT_FOUND_MESSAGE.getBytes());
                    }
                }
                case "POST" -> {
                    Path filePath = Path.of(USER_FILES_PATH + url);
                    Files.createDirectories(filePath.getParent());

                    int contentLength = getContentLength(br);
                    byte[] requestBodyBytes = getRequestBodyBytes(br, contentLength);

                    String fileExtension = getFileExtension(filePath);
                    String contentType = CONTENT_TYPES.get(fileExtension);

                    sendHeader(output, 200, "OK", contentType, contentLength);

                    if (requestBodyBytes != null) {
                        output.write(requestBodyBytes);
                        Files.write(filePath, requestBodyBytes);
                    } else {
                        System.out.println("Parsed zero bytes.");
                    }
                }
                default -> sendHeader(
                        output, 405, "Method Not Allowed", CONTENT_TYPES.get(""), 0
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processHeader(BufferedReader br) throws IOException {
        String line = br.readLine();
        String[] requestParts = line.split(" ");

        requestType = requestParts[0];
        url = requestParts[1];
    }
    
    private int getContentLength(BufferedReader br) throws IOException {
        int contentLength = 0;

        String line;
        while (!(line = br.readLine()).isEmpty()) {
            System.out.println(line);
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()).trim());
            }
        }
        return contentLength;
    }

    private byte[] getRequestBodyBytes(BufferedReader br, int contentLength) throws IOException {
        byte[] requestBodyBytes = null;

        if (contentLength > 0) {
            char[] body = new char[contentLength];
            if (br.read(body, 0, contentLength) == -1) {
                System.out.println("Issues with reading the body.");
            }
            String requestBody = new String(body);
            requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            System.out.println("POST Body: " + requestBody);
        }
        return requestBodyBytes;
    }

    private String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);
    }

    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }
}