import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleFileServer {

    public static void main(String[] args) throws IOException {
        // Create an HTTP server listening on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Create a context for the "/download" endpoint
        HttpContext context = server.createContext("/download", new FileHandler());

        // Set up Basic Authentication
        context.setAuthenticator(new BasicAuthenticator("SecureRealm") {
            @Override
            public boolean checkCredentials(String username, String password) {
                // Hardcoded credentials for testing
                return "admin".equals(username) && "secret".equals(password);
            }
        });

        // Start the server
        server.start();
        System.out.println("Server started on port 8080. Access /download to test.");
    }

    static class FileHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Serve a file if authentication is successful
            File file = new File("gradlew"); // File to serve
            if (!file.exists()) {
                String response = "File not found.";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // Set response headers
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + file.getName());
            exchange.sendResponseHeaders(200, file.length());

            // Stream the file to the client
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = exchange.getResponseBody();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fis.close();
            os.close();
        }
    }
}