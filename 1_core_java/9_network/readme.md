# Java Networking

Goal: one practical server/client path for Core Java networking, enough for SDE2 prep before Spring Boot.

## Study Order

| No. | File | Covers |
| --- | --- | --- |
| 1 | `src/S01SocketServer.java` | Raw `ServerSocket`, `accept()`, `Socket`, `InputStream`, `OutputStream`. |
| 2 | `src/S02SocketClient.java` | Raw socket client sending `hello` and reading `hello back`. |
| 3 | `src/S03RestServer.java` | REST server with `POST /user`, `GET /user?id=1`, `HashMap`, status codes, Jackson deserialization. |
| 4 | `src/S04RestClient.java` | `HttpClient`, `HttpRequest`, `HttpResponse`, `send()`, `sendAsync()`, `CompletableFuture`, headers, timeout, Jackson serialization, `URLConnection`. |
| 5 | `src/S05GrpcServer.java` | Real grpc-java server generated from `src/main/proto/user.proto`. |
| 6 | `src/S06GrpcClient.java` | Real grpc-java client with `ManagedChannel`, generated blocking stub, request, RPC call, shutdown. |

## Run

Run one combo:

```bash
./run1.sh   # socket server + client
./run2.sh   # REST server + client
./run3.sh   # gRPC server + client
```

Test all combos:

```bash
./test1.sh
./test2.sh
./test3.sh
```

## Socket vs REST vs gRPC

```text
                    Your Application
                          |
          +---------------+---------------+
          |               |               |
       Raw Socket        REST            gRPC
          |               |               |
   Custom text bytes     HTTP        Protobuf RPC
          |               |               |
         TCP             TCP            HTTP/2
          |               |               |
       Socket          Socket           TCP
          |               |               |
          +---------------+---------------+
                          |
                       Network
```

| Style | Files | What you write | Protocol level | Best mental model |
| --- | --- | --- | --- | --- |
| Raw socket | `S01SocketServer`, `S02SocketClient` | `ServerSocket`, `Socket`, streams, your own message format | TCP directly | You define how bytes mean something. |
| REST | `S03RestServer`, `S04RestClient` | URLs, methods, headers, status codes, JSON body | HTTP over TCP | Resources exposed through HTTP endpoints. |
| gRPC | `S05GrpcServer`, `S06GrpcClient`, `user.proto` | `.proto`, generated stub, request/response objects | HTTP/2 over TCP | Call a remote method like a typed function. |

| Question | Socket | REST | gRPC |
| --- | --- | --- | --- |
| Data format | Whatever you design | Usually JSON | Protobuf |
| Contract | Informal unless you document it | Endpoint docs / OpenAPI later | `.proto` file |
| Java client API | `Socket` streams | `HttpClient` | Generated stub + `ManagedChannel` |
| Human readable? | Only if you send text | Yes, JSON/HTTP is easy to inspect | Mostly no, binary protobuf |
| Common backend use | Low-level protocols, custom networking | Public/internal web APIs | Service-to-service RPC |

Default ports:

| Demo | Port |
| --- | --- |
| Socket | `9090` |
| REST | `8081` |
| gRPC | `9092` |

## Concept Coverage

| Original topic | Where covered | Status |
| --- | --- | --- |
| TCP | `S01SocketServer`, `S02SocketClient` | Direct tiny exercise |
| Sockets | `ServerSocket`, `Socket`, streams | Direct |
| HTTP | `S03RestServer`, `S04RestClient` | GET, POST, headers, body, status |
| HTTPS | `S04RestClient` note | Same `HttpClient`; use `https://` URI |
| Serialization | `ObjectMapper.writeValueAsString`, `readValue` | Jackson |
| Java HttpClient | `S04RestClient` | `HttpClient`, `HttpRequest`, `HttpResponse` |
| Async networking | `S04RestClient` | `sendAsync()` + `CompletableFuture` |
| URLConnection | `S04RestClient` | Legacy recognition |
| REST client/server | `S03RestServer`, `S04RestClient` | Practical GET/POST project |
| gRPC client/server | `user.proto`, `S05GrpcServer`, `S06GrpcClient` | Real grpc-java server/client |

## REST Flow

```text
S04RestClient
|
|-- User object
|-- ObjectMapper.writeValueAsString(user)
|-- POST /user
|-- S03RestServer stores id -> name in HashMap
|
|-- GET /user?id=1
|-- S03RestServer returns name
|
|-- sendAsync(GET)
|-- CompletableFuture prints name later
|
|-- URLConnection GET
       |-- old API shape only
```

## gRPC Mental Model

```text
user.proto
   |
   v
Maven protobuf plugin generates Java classes
   |
   |-- UserServiceGrpc
   |-- AddUserRequest
   |-- GetUserRequest
   |-- User
   |
   v
ManagedChannel
   |
   v
Generated Stub
   |
   v
stub.getUser(request)
```

`S05` / `S06` are now real grpc-java code. The generated classes are created under `target/generated-sources` during `mvn compile`.
