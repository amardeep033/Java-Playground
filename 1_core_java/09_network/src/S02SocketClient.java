import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class S02SocketClient {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 9090 : Integer.parseInt(args[0]);

        // new Socket(host, port) opens a TCP connection to the server.
        // Use try-with-resources so socket and streams close automatically.
        try (Socket socket = new Socket("localhost", port);
             // Read server response bytes as text.
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             // Send client request bytes as text.
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            // println() sends "hello\n"; server readLine() needs that newline.
            out.println("hello");
            // readLine() blocks until server replies with a line.
            System.out.println("client received: " + in.readLine());
        }
    }
}

// Socket creates one TCP connection.
// OutputStream sends bytes; InputStream receives bytes.
