import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class S04RestClient {
    // Reuse ObjectMapper. It converts Java object -> JSON and JSON -> Java object.
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8081 : Integer.parseInt(args[0]);
        String baseUrl = "http://localhost:" + port + "/user";

        // Reuse HttpClient. It can manage connections internally.
        HttpClient client = HttpClient.newBuilder()
                // connectTimeout limits how long we wait to establish a connection.
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        User user = new User(1, "Amar");
        // Serialization: Java object -> JSON string.
        String json = mapper.writeValueAsString(user);

        // Build POST request: URI + timeout + header + JSON body.
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofSeconds(2))
                // Content-Type tells server this request body is JSON.
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // send() is blocking: current thread waits for response.
        HttpResponse<String> postResponse = client.send(post, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST: " + postResponse.statusCode() + " " + postResponse.body());

        // GET request has no body; id is passed as query parameter.
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "?id=1"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        // BodyHandlers.ofString() converts response bytes to String.
        HttpResponse<String> getResponse = client.send(get, HttpResponse.BodyHandlers.ofString());
        System.out.println("GET: " + getResponse.statusCode() + " " + getResponse.body());

        // sendAsync() returns CompletableFuture immediately.
        client.sendAsync(get, HttpResponse.BodyHandlers.ofString())
                // thenApply transforms HttpResponse<String> into body String.
                .thenApply(HttpResponse::body)
                // thenAccept consumes the final value.
                .thenAccept(name -> System.out.println("ASYNC GET: " + name))
                // join() waits at the edge so demo does not exit early.
                .join();

        // Legacy S08 shape: recognize URLConnection, prefer HttpClient above.
        URLConnection oldConnection = URI.create(baseUrl + "?id=1").toURL().openConnection();
        // URLConnection exposes response body as InputStream.
        try (InputStream in = oldConnection.getInputStream()) {
            System.out.println("URLConnection GET: " + new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    // Simple DTO used by Jackson.
    public static class User {
        public int id;
        public String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

// Covers REST client:
// ObjectMapper.writeValueAsString(), HttpClient, HttpRequest, HttpResponse,
// headers, timeout, send(), sendAsync(), CompletableFuture, URLConnection.
