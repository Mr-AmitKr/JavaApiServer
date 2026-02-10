import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class ItemApiServer {

    // ===== Item Model =====
    static class Item {
        int id;
        String name;
        String description;

        Item(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        String toJson() {
            return String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\"}",
                    id, name, description);
        }
    }

    // ===== In-memory Data Store =====
    static List<Item> items = new ArrayList<>();
    static int idCounter = 1;

    public static void main(String[] args) throws Exception {
        // 1. Get port from environment variable, default to 8000 for local testing
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8000;

        System.out.println("Starting server on port: " + port);

        // 2. Bind to 0.0.0.0 (required for cloud) instead of localhost
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", new ItemsHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Server is live!");
    }

    // ===== Handler =====
    static class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Log exactly what the server sees to Render Logs
            System.out.println("LOG: Method=" + method + " | Path=" + path);

            // Normalize path: Remove trailing slashes and handle empty paths
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            // This covers "/", "/items", and the Render health checks
            if (method.equals("GET") && (path.equals("/") || path.endsWith("/items") || path.endsWith("/items/"))) {
                sendResponse(exchange, 200, "{\"status\":\"API Running\",\"message\":\"Welcome to the Item API\"}");
                return; // Stop processing once response is sent
            } else if (method.equals("POST") && path.equals("/items")) {
                handleAddItem(exchange);
            } else if (method.equals("GET") && path.startsWith("/items/")) {
                handleGetItem(exchange);
            } else {
                String errorMsg = String.format("{\"error\":\"Not Found\",\"debug_path\":\"%s\"}", path);
                sendResponse(exchange, 404, errorMsg);
            }
        }

        // ---- Add New Item ----
        void handleAddItem(HttpExchange ex) throws IOException {
            String body = new BufferedReader(
                    new InputStreamReader(ex.getRequestBody())).lines().collect(Collectors.joining());

            // Very simple JSON parsing
            Map<String, String> map = parseJson(body);

            // Input Validation
            if (!map.containsKey("name") || map.get("name").isEmpty()) {
                sendResponse(ex, 400, "{\"error\":\"Name is required\"}");
                return;
            }

            String name = map.get("name");
            String desc = map.getOrDefault("description", "");

            Item item = new Item(idCounter++, name, desc);
            items.add(item);

            sendResponse(ex, 201, item.toJson());
        }

        // ---- Get Item By ID ----
        void handleGetItem(HttpExchange ex) throws IOException {
            String[] parts = ex.getRequestURI().getPath().split("/");

            try {
                int id = Integer.parseInt(parts[2]);

                for (Item i : items) {
                    if (i.id == id) {
                        sendResponse(ex, 200, i.toJson());
                        return;
                    }
                }

                sendResponse(ex, 404, "{\"error\":\"Item not found\"}");

            } catch (Exception e) {
                sendResponse(ex, 400, "{\"error\":\"Invalid ID\"}");
            }
        }

        // ---- Utilities ----
        Map<String, String> parseJson(String json) {
            Map<String, String> map = new HashMap<>();
            json = json.replace("{", "").replace("}", "");

            for (String p : json.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    map.put(
                            kv[0].replace("\"", "").trim(),
                            kv[1].replace("\"", "").trim()

                    );
                }
            }
            return map;
        }

        void sendResponse(HttpExchange ex, int code, String resp) throws IOException {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(code, resp.getBytes().length);
            OutputStream os = ex.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        }
    }
}