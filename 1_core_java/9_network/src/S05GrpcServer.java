import grpcstudy.AddUserRequest;
import grpcstudy.AddUserResponse;
import grpcstudy.GetUserRequest;
import grpcstudy.User;
import grpcstudy.UserServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;

public class S05GrpcServer {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 9092 : Integer.parseInt(args[0]);

        // ServerBuilder creates a real gRPC server listening on this port.
        Server server = ServerBuilder.forPort(port)
                // addService() registers our implementation of the generated service contract.
                .addService(new UserService())
                .build()
                .start();

        System.out.println("gRPC server running on localhost:" + port);
        // Keep server process alive. Without this, main exits and server stops.
        server.awaitTermination();
    }

    // UserServiceGrpc.UserServiceImplBase is generated from user.proto.
    static class UserService extends UserServiceGrpc.UserServiceImplBase {
        private final Map<Integer, String> users = new HashMap<>();

        @Override
        public void addUser(AddUserRequest request, StreamObserver<AddUserResponse> responseObserver) {
            // Request object is generated from proto message AddUserRequest.
            users.put(request.getId(), request.getName());

            // Response object is generated from proto message AddUserResponse.
            AddUserResponse response = AddUserResponse.newBuilder()
                    .setSaved(true)
                    .build();

            // onNext sends the response; onCompleted marks RPC as done.
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
            // request.getId() is generated getter from the proto field id.
            String name = users.getOrDefault(request.getId(), "not found");

            User response = User.newBuilder()
                    .setId(request.getId())
                    .setName(name)
                    .build();

            // Unary RPC: exactly one response, then complete.
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

// Real gRPC server:
// user.proto -> generated UserServiceGrpc.UserServiceImplBase
// ServerBuilder -> addService() -> start()
// StreamObserver sends the response and completes the RPC.
