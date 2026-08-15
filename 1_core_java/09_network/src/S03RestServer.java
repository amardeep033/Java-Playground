import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class S03RestServer {
    // ObjectMapper converts JSON <-> Java objects. Spring Boot uses Jackson heavily.
    private static final ObjectMapper mapper = new ObjectMapper();
    // Simple in-memory storage: id -> name.
    private static final Map<Integer, String> users = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8081 : Integer.parseInt(args[0]);
        // JDK HttpServer is a tiny HTTP server, useful for Core Java demos.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // One path supports two HTTP methods: POST to save, GET to read.
        server.createContext("/user", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Deserialize request JSON body into Java object.
                User user = mapper.readValue(exchange.getRequestBody(), User.class);
                users.put(user.id, user.name);
                // Serialize Java object into JSON response body.
                send(exchange, 201, mapper.writeValueAsString(new Result(true)));
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                // Query string is simple here: /user?id=1
                int id = Integer.parseInt(exchange.getRequestURI().getQuery().split("=")[1]);
                String name = users.get(id);
                // 200 means found; 404 means id is not present.
                send(exchange, name == null ? 404 : 200, name == null ? "not found" : name);
                return;
            }

            // 405 means endpoint exists, but this HTTP method is not supported.
            send(exchange, 405, "method not allowed");
        });

        // start() begins accepting HTTP requests.
        server.start();
        System.out.println("REST server running on http://localhost:" + port + "/user");
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        // Header tells client how to interpret the response body.
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        // sendResponseHeaders(status, length) writes HTTP status code and body length.
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        // Close exchange after writing response.
        exchange.close();
    }

    // Jackson needs public fields or getters/setters for this simple mapping style.
    public static class User {
        public int id;
        public String name;
    }

    public static class Result {
        public boolean saved;

        public Result(boolean saved) {
            this.saved = saved;
        }
    }
}

// Covers REST server + Jackson:
// POST /user -> ObjectMapper.readValue() -> HashMap put.
// GET /user?id=1 -> HashMap get -> status/body response.
