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

        System.out.println("STEP 1 - Program started");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        System.out.println("STEP 2 - Bind successful");

        server.createContext("/", new ItemsHandler());

        server.setExecutor(null);

        System.out.println("Server started at http://localhost:8000");

        server.start();
    }

    // ===== Handler =====
    static class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (method.equals("GET") && (path.equals("/") || path.equals("/items"))) {
                sendResponse(exchange, 200, "{\"status\":\"API Running\"}");
            } else if (method.equals("POST") && path.equals("/items")) {
                handleAddItem(exchange);
            } else if (method.equals("GET") && path.matches("/items/\\d+")) {
                handleGetItem(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
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