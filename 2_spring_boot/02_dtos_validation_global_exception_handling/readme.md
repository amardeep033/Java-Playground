## How To Run

Compile the project:

```bash
mvn compile
```

Start the Spring Boot app:

```bash
mvn spring-boot:run
```

Run error-focused curl checks:

```bash
curl -i http://localhost:8080/api/products/99

curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":-1}'

curl -i -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"   ","price":25.0}'

curl -i -X PUT http://localhost:8080/api/products/99 \
  -H "Content-Type: application/json" \
  -d '{"name":"Missing","price":50.0}'

curl -i -X DELETE http://localhost:8080/api/products/99
```

## Cheatsheet

What Changed From 01 To 02

| Area | In 01 Basic CRUD | In 02 DTOs + Validation + Global Exceptions |
| --- | --- | --- |
| Request body | Controller accepted `Product` directly. | Controller accepts `ProductRequest`. |
| Response body | Controller returned `Product` directly. | Controller returns `ProductResponse`. |
| Validation | Started with basic validation concepts. | Validation lives on the request DTO and runs through `@Valid`. |
| Missing product | Controller built `ResponseEntity.notFound()` inline. | Service throws `ProductNotFoundException`; global handler builds the 404 response. |
| Error response | Spring/default or repeated controller logic. | `ApiError` gives one consistent error body. |
| Controller responsibility | HTTP mapping plus some error response decisions. | HTTP mapping plus DTO conversion; error formatting is moved out. |
| Service responsibility | Returned `Optional` for controller to interpret. | Returns data or throws meaningful application exceptions. |
| Repository responsibility | In-memory storage. | Same as 01; DTOs do not enter repository layer. |

DTO Flow

| Step | Class | Purpose |
| --- | --- | --- |
| Request body | `ProductRequest` | Shape accepted from the client for create/update. |
| Validation | `@Valid` + annotations | Rejects invalid request data before service logic runs. |
| Domain model | `Product` | Internal application object stored by the repository. |
| Response body | `ProductResponse` | Shape returned to the client. |

Request vs Response DTO

| DTO | Contains | Why |
| --- | --- | --- |
| `ProductRequest` | `name`, `price` | Client should send only fields allowed for create/update. |
| `ProductResponse` | `id`, `name`, `price` | Server returns generated/read-only fields like `id`. |
| `Product` | `id`, `name`, `price` | Internal model used by service/repository. |

Validation

| Annotation | Used On | Meaning |
| --- | --- | --- |
| `@Valid` | Controller request body parameter | Tells Spring to validate the DTO. |
| `@NotBlank` | `ProductRequest.name` | Name must not be null, empty, or only spaces. |
| `@PositiveOrZero` | `ProductRequest.price` | Price must be zero or positive. |
| `MethodArgumentNotValidException` | Global exception handler | Thrown by Spring when request DTO validation fails. |

Global Exception Handling

| Class | Purpose |
| --- | --- |
| `ProductNotFoundException` | Custom exception for missing product ids. |
| `GlobalExceptionHandler` | Central place for converting exceptions into HTTP responses. |
| `ApiError` | Common error response body for failures. |
| `@RestControllerAdvice` | Makes exception handling apply across controllers. |
| `@ExceptionHandler` | Selects which method handles which exception type. |

HTTP Results

| Scenario | Status | Body |
| --- | --- | --- |
| Product created | `201 Created` | `ProductResponse` |
| Product found | `200 OK` | `ProductResponse` |
| Product list/search | `200 OK` | List of `ProductResponse` |
| Product deleted | `204 No Content` | No body |
| Product id missing | `404 Not Found` | `ApiError` |
| Validation failed | `400 Bad Request` | `ApiError` with `fieldErrors` |

Request Lifecycle With DTOs

```text
JSON request
        |
        v
ProductRequest
        |
        v
@Valid validation
        |
        v
ProductController
        |
        v
ProductService
        |
        v
ProductRepository
        |
        v
ProductResponse
        |
        v
JSON response
```

Exception Lifecycle

```text
Missing product id
        |
        v
ProductService throws ProductNotFoundException
        |
        v
GlobalExceptionHandler catches it
        |
        v
ApiError is created
        |
        v
HTTP 404 response
```

## Interview QA

| Question | Answer |
| --- | --- |
| Why use DTOs instead of exposing `Product` directly? | DTOs separate API shape from internal model shape. This prevents clients from sending or depending on fields they should not control. |
| Why should validation usually be on request DTOs? | Validation rules often describe API input rules, not necessarily every rule of the internal domain model. |
| Why does `ProductRequest` not have `id`? | The server owns id generation. Clients should not choose ids during create. |
| Why does `ProductResponse` have `id`? | The client needs the generated id after create/read operations. |
| What happens when validation fails? | Spring throws `MethodArgumentNotValidException` before controller logic continues, and `GlobalExceptionHandler` returns `400 Bad Request`. |
| Why use `@RestControllerAdvice`? | It centralizes exception-to-response mapping instead of repeating error handling in every controller method. |
| Why throw `ProductNotFoundException` from service? | The service knows whether the requested product exists. The global handler decides how that exception becomes HTTP. |
| Why not return `null` when product is missing? | `null` can accidentally become `200 OK` with an empty body. A custom exception makes missing data explicit. |
| Should every exception be handled globally? | Handle expected application errors globally. Unexpected bugs can still become `500 Internal Server Error`. |
| Is `ApiError` required? | No, but a common error format makes client-side handling easier and consistent. |
| Why return field-level validation errors? | Clients can show exact messages beside the fields that failed. |
| What is the difference between DTO and entity/model? | DTO is for API input/output. Entity/model is for internal business/storage representation. |
