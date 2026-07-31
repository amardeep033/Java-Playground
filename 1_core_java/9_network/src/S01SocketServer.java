import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class S01SocketServer {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 9090 : Integer.parseInt(args[0]);

        // Use try-with-resources so the listening socket closes automatically.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("socket server waiting on port " + port);

            // accept() is blocking: this line waits until one client connects.
            // The returned Socket represents that one TCP connection.
            try (Socket socket = serverSocket.accept();
                 // getInputStream() reads bytes coming from the client.
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 // getOutputStream() writes bytes back to the client.
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                // readLine() waits until the client sends a line ending.
                String message = in.readLine();
                System.out.println("server received: " + message);
                // println() sends text plus newline, so client readLine() can finish.
                out.println(message + " back");
            }
        }
    }
}

// Covers raw TCP/socket basics:
// ServerSocket -> accept() -> Socket -> InputStream/OutputStream -> close with try-with-resources.
