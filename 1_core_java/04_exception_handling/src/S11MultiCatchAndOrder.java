import java.io.IOException;
import java.sql.SQLException;

public class S11MultiCatchAndOrder {
    public static void main(String[] args) {
        handle("file");
        handle("database");
        handle("other");
    }

    private static void handle(String source) {
        try {
            riskyOperation(source);
        }
        // Catch order matters. A broad catch, such as Exception, before a specific catch
        // makes the specific catch unreachable and causes a compile-time error.
        catch (IOException | SQLException exception) {
            System.out.println("Recoverable problem: " + exception.getMessage());
        } catch (Exception exception) {
            System.out.println("Fallback handler: " + exception.getMessage());
        }
    }

    private static void riskyOperation(String source) throws IOException, SQLException {
        if ("file".equals(source)) {
            throw new IOException("File missing");
        }
        if ("database".equals(source)) {
            throw new SQLException("Database unavailable");
        }
        throw new IllegalStateException("Unexpected state");
    }
}
