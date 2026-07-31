import grpcstudy.AddUserRequest;
import grpcstudy.AddUserResponse;
import grpcstudy.GetUserRequest;
import grpcstudy.User;
import grpcstudy.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class S06GrpcClient {
    public static void main(String[] args) {
        int port = args.length == 0 ? 9092 : Integer.parseInt(args[0]);

        // ManagedChannel is the client-side connection/channel to the gRPC server.
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                // Plaintext is fine for local demo. Production usually uses TLS.
                .usePlaintext()
                .build();

        try {
            // Blocking stub is generated from user.proto. Calls wait until response arrives.
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

            // Build generated request object and call remote AddUser RPC.
            AddUserResponse saved = stub.addUser(AddUserRequest.newBuilder()
                    .setId(1)
                    .setName("Amar")
                    .build());
            System.out.println("gRPC addUser saved: " + saved.getSaved());

            // Build generated request object and call remote GetUser RPC.
            User user = stub.getUser(GetUserRequest.newBuilder()
                    .setId(1)
                    .build());
            System.out.println("gRPC getUser: " + user.getId() + " " + user.getName());
        } finally {
            // Always shutdown the channel when the client is done.
            channel.shutdown();
        }
    }
}

// Real gRPC client:
// ManagedChannel -> generated blocking stub -> request object -> RPC call -> response object -> shutdown.
