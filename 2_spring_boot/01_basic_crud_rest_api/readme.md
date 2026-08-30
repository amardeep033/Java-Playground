## How To Run

Compile the project:

```bash
mvn compile
```

Start the Spring Boot app:

```bash
mvn spring-boot:run
```

Run curl checks:

```bash
curl http://localhost:8080/api/products

curl http://localhost:8080/api/products/1

curl "http://localhost:8080/api/products/search"

curl "http://localhost:8080/api/products/search?name=pen&minPrice=5"

curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Pencil","price":5.0}'

curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Notebook - Large","price":120.0}'

curl -X DELETE http://localhost:8080/api/products/1
```

## Cheatsheet

GET By Id Request Lifecycle

```text
GET /api/products/1
        |
        v
Embedded Tomcat - receives the HTTP request on server.port=8080 and passes it into Spring MVC
        |
        v
DispatcherServlet - Spring MVC front controller; every matching web request enters here first
        |
        v
HandlerMapping - finds which controller method matches GET + /api/products/1
        |
        v
ProductController.getProductById()
        |
        v
@PathVariable conversion - extracts "1" from the URL and converts it to Long id
        |
        v
ProductService - contains application/business flow
        |
        v
ProductRepository - reads product data from the in-memory map
        |
        v
ResponseEntity<Product> - wraps the response body with HTTP status like 200 OK or 404 Not Found
        |
        v
HttpMessageConverter - converts the Java Product object into JSON
        |
        v
JSON
        |
        v
HTTP response
```

Request Contents

| Part | Meaning | Example |
| --- | --- | --- |
| HTTP request | Message sent by the client to the server. | `GET /api/products/1` |
| Method | Action the client wants to perform. | `GET`, `POST`, `PUT`, `DELETE` |
| Path / URL | Endpoint being called. | `/api/products/1` |
| Header | Metadata about the request. | `Content-Type: application/json` |
| Body | Data sent to the server, usually for create/update. | `{"name":"Pencil","price":5.0}` |

Spring Request Annotations

| Annotation | Meaning | Example |
| --- | --- | --- |
| Path variable | Value inside the path. | `/api/products/{id}` with `@PathVariable Long id` |
| Request param | Value after `?` in the URL. | `/api/products/search?name=Pen` with `@RequestParam String name` |
| When to use `@PathVariable` | Use when the value identifies a specific resource. | `/api/products/1`, `/users/10`, `/orders/99` |
| When to use `@RequestParam` | Use for filtering, searching, sorting, pagination, or optional inputs. | `/api/products?minPrice=10&sort=price` |
| Optional request param | `required = false` means the client may skip this query param. | `@RequestParam(required = false) String name` |
| Default request param | `defaultValue` gives Spring a fallback value when the client skips the param. | `@RequestParam(defaultValue = "0.0") double minPrice` |
| `@RequestBody` | Converts JSON body into a Java object. | `@RequestBody Product product` |
| `@Valid` | Runs validation rules on the request body before controller logic continues. | `@Valid @RequestBody Product product` |

HTTP Methods

| Method | Meaning | Example |
| --- | --- | --- |
| `GET` | Read data; should not change server state. | `GET /api/products` |
| `POST` | Create a new resource. | `POST /api/products` |
| `PUT` | Replace an existing resource completely. | `PUT /api/products/1` |
| `PATCH` | Partially update an existing resource. | `PATCH /api/products/1` |
| `DELETE` | Remove a resource. | `DELETE /api/products/1` |

HTTP Status Codes

| Status | Meaning | Example |
| --- | --- | --- |
| `200 OK` | Request succeeded and usually returns a body. | Successful `GET` or `PUT` |
| `201 Created` | Resource was created successfully. | Successful `POST` |
| `204 No Content` | Request succeeded but response has no body. | Successful `DELETE` |
| `400 Bad Request` | Client sent invalid input. | Missing or invalid JSON field |
| `404 Not Found` | Resource does not exist. | Product id not found |
| `500 Internal Server Error` | Unexpected server-side failure. | Unhandled exception |

ResponseEntity

| Expression | Meaning | Example |
| --- | --- | --- |
| `ResponseEntity<T>` | Lets you control status, headers, and body. | `ResponseEntity<Product>` |
| `ResponseEntity.ok(body)` | Returns `200 OK` with a response body. | Found product |
| `ResponseEntity.notFound().build()` | Returns `404 Not Found` with no body. | Missing product id |
| `ResponseEntity.status(HttpStatus.CREATED).body(body)` | Returns custom status with a body. | Created product |
| `ResponseEntity.noContent().build()` | Returns `204 No Content` with no body. | Deleted product |

Java Helpers Used Here

| Helper | Meaning | Example |
| --- | --- | --- |
| `Optional<T>` | Represents a value that may or may not exist. | `Optional<Product>` |
| `Map.values()` | Returns all values from a map as a `Collection`. | `products.values()` |

Validation Annotations

| Annotation | Meaning | Example |
| --- | --- | --- |
| `@NotBlank` | String must not be `null`, empty, or only spaces. | `@NotBlank private String name;` |
| `@PositiveOrZero` | Number must be zero or greater. | `@PositiveOrZero private double price;` |
| `spring-boot-starter-validation` | Adds Jakarta Bean Validation support to Spring Boot. | Required for `@Valid`, `@NotBlank`, etc. |

Common Spring Boot Properties

| Key | Meaning | Example |
| --- | --- | --- |
| `spring.application.name` | Application name used by Spring Boot and logs/tools. | `spring.application.name=basic-crud-rest-api` |
| `server.port` | HTTP port for the embedded server. | `server.port=8080` |
| `server.servlet.context-path` | Base path added before all controller paths. | `server.servlet.context-path=/app` |
| `spring.profiles.active` | Selects the active environment profile. | `spring.profiles.active=dev` |
| `spring.datasource.url` | Database connection URL. | `spring.datasource.url=jdbc:mysql://localhost:3306/shop` |
| `spring.datasource.username` | Database username. | `spring.datasource.username=root` |
| `spring.datasource.password` | Database password. | `spring.datasource.password=secret` |
| `spring.jpa.hibernate.ddl-auto` | Controls schema creation/update behavior for JPA. | `spring.jpa.hibernate.ddl-auto=update` |
| `spring.jpa.show-sql` | Prints SQL queries in logs. | `spring.jpa.show-sql=true` |
| `logging.level.<package>` | Sets logging level for a package/class. | `logging.level.com.example.basiccrud=DEBUG` |
| `management.endpoints.web.exposure.include` | Exposes selected Actuator endpoints if Actuator is added. | `management.endpoints.web.exposure.include=health,info` |

## Interview QA

| Question | Answer |
| --- | --- |
| Why use `GET /api/products/1` instead of `GET /api/products?id=1`? | The id identifies one specific product, so it fits naturally as a path variable. Query params are better for filtering, sorting, searching, and pagination. |
| When should you use `@PathVariable`? | Use it when the value is part of the resource identity or hierarchy, such as `/api/products/1`, `/api/users/10/orders`, or `/api/orders/99/items/5`. |
| When should you use `@RequestParam`? | Use it when the value modifies the query result without identifying one exact resource, such as `/api/products?search=pen`, `/api/products?sort=price`, or `/api/products?page=2`. |
| What is the difference between `PUT` and `PATCH`? | `PUT` usually replaces the whole resource. `PATCH` updates only selected fields. |
| Is `PUT` idempotent? | Yes. Sending the same full update many times should leave the resource in the same final state. |
| Is `PATCH` always non-idempotent? | No. It depends on the patch operation. Setting `price` to `100` is idempotent; incrementing `price` by `10` is not. |
| Why return `201 Created` for `POST`? | Because a new resource was created. Returning `200 OK` works technically, but `201 Created` is more precise REST design. |
| Why return `204 No Content` for `DELETE`? | The delete succeeded and there is no response body to send back. |
| Why return `404 Not Found` instead of `null`? | HTTP clients understand status codes. A `null` body with `200 OK` can make failure look like success. |
| Why use `ResponseEntity`? | It gives explicit control over the HTTP status code, response body, and headers. |
| Should the client send `id` when creating a product? | Usually no. The server should generate the id to avoid conflicts and protect data ownership. |
| What should happen if a client sends an id in the POST body? | This API ignores it by setting `product.setId(null)`, then generates a new id. |
| What should `PUT /api/products/99` do if product 99 does not exist? | Common choices are `404 Not Found` or create it if your API documents upsert behavior. This project returns `404`. |
| Where should business logic go: controller or service? | Service. Controllers should focus on HTTP input/output; services should hold application rules. |
| Why have a repository layer? | It hides data storage details from the service. Today it is an in-memory map; later it could be a database. |
| What is the problem with this in-memory repository? | Data is lost when the app restarts, and it is not suitable as a real database replacement. |
| Why use `ConcurrentHashMap` instead of `HashMap`? | It is safer when multiple HTTP requests access the map at the same time. |
| Why use `AtomicLong` for ids? | It generates incrementing ids safely across concurrent requests. |
| Should all errors return `500`? | No. `500` means unexpected server failure. Client mistakes should usually return `400`, `404`, or another 4xx status. |
| What is a good URL for filtering products by price? | `GET /api/products?minPrice=10&maxPrice=100` because filters belong in query params. |
| What is a good URL for all reviews of product 1? | `GET /api/products/1/reviews` because reviews are a sub-resource of product 1. |
| Should `GET` requests have a body? | Usually no. Use path variables and query params for read inputs. |
