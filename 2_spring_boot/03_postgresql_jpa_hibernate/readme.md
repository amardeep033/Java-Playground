## How To Run

Start your local PostgreSQL server. This module uses the same connection style as pgAdmin:

```text
Name:                 local
Host:                 localhost
Port:                 5432
Maintenance database: postgres
Username:             postgres
Password:             postgres
```

Spring Boot uses `Maintenance database` as the database name in the JDBC URL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```

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
curl -i http://localhost:8080/api/products

curl -i http://localhost:8080/api/products/1

curl -i "http://localhost:8080/api/products/search?name=pen&minPrice=5"

curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Pencil","price":5.0}'

curl -i -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Notebook - Large","price":120.0}'

curl -i -X DELETE http://localhost:8080/api/products/1

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

What Changed From 02 To 03

| Area | In 02 DTOs + Validation + Global Exceptions | In 03 PostgreSQL + JPA/Hibernate |
| --- | --- | --- |
| Request/response DTOs | `ProductRequest` and `ProductResponse`. | Same as 02; persistence does not change the API contract. |
| Validation | `@Valid` validates request DTOs. | Same as 02; invalid input is rejected before database work. |
| Missing product | Service throws `ProductNotFoundException`. | Same as 02; `findById` now checks PostgreSQL through JPA. |
| Error response | `GlobalExceptionHandler` returns `ApiError`. | Same as 02; database-backed storage does not change API errors. |
| Product model | Plain Java object. | JPA entity mapped to the `products` table. |
| Repository | Hand-written class with `ConcurrentHashMap`. | Interface extending `JpaRepository<Product, Long>`. |
| Id generation | `AtomicLong` generated ids in Java memory. | PostgreSQL generates ids with `GenerationType.IDENTITY`. |
| Search | Java stream filtered map values. | Spring Data derives a database query from the method name. |
| Startup data | Repository constructor inserted sample products. | `DataSeeder` inserts sample rows only when the table is empty. |
| Data lifetime | Lost when the app stops. | Kept in PostgreSQL, including across app restarts. |

Layer Shape

| Layer | Main Class | What It Knows |
| --- | --- | --- |
| Controller | `ProductController` | HTTP routes, request DTOs, response DTOs. |
| Service | `ProductService` | Application flow, missing-product rules, create/update/delete decisions. |
| Repository | `ProductRepository` | JPA persistence operations; no DTOs. |
| Entity/model | `Product` | Database mapping and internal product fields. |
| Database | PostgreSQL `products` table | Rows, columns, generated primary keys. |

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
| `Product` | `id`, `name`, `price` | JPA entity used by service/repository and mapped to PostgreSQL. |

JPA Entity Mapping

| Annotation | Used On | Meaning |
| --- | --- | --- |
| `@Entity` | `Product` class | Tells JPA this class should be persisted. |
| `@Table(name = "products")` | `Product` class | Maps the entity to the `products` database table. |
| `@Id` | `Product.id` | Marks the primary key field. |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | `Product.id` | Database generates the id during insert. |
| `@Column(nullable = false)` | `name`, `price` | Creates/enforces non-null column mapping. |

Spring Data JPA Repository

| Method | Comes From | What It Does |
| --- | --- | --- |
| `findAll()` | `JpaRepository` | Selects all product rows. |
| `findById(id)` | `JpaRepository` | Selects one row by primary key and returns `Optional<Product>`. |
| `save(product)` | `JpaRepository` | Inserts or updates a product depending on entity state/id. |
| `existsById(id)` | `JpaRepository` | Checks whether a row exists for that primary key. |
| `deleteById(id)` | `JpaRepository` | Deletes the row with that primary key. |
| `count()` | `JpaRepository` | Counts rows; used by `DataSeeder`. |
| `findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(...)` | Derived query | Spring Data builds a query from the method name. |

Repository Method Name Breakdown

| Part | Meaning |
| --- | --- |
| `findBy` | Start a SELECT query. |
| `Name` | Use the `name` field. |
| `Containing` | SQL `LIKE %value%` behavior. |
| `IgnoreCase` | Compare without case sensitivity. |
| `And` | Add another condition. |
| `Price` | Use the `price` field. |
| `GreaterThanEqual` | SQL `>=` behavior. |

PostgreSQL Configuration

| Property | Meaning |
| --- | --- |
| `spring.datasource.url=jdbc:postgresql://localhost:5432/postgres` | Connects Spring Boot to the local `postgres` database. |
| `spring.datasource.username=postgres` | Database user, same as pgAdmin. |
| `spring.datasource.password=postgres` | Database password, same as pgAdmin. |
| `spring.jpa.hibernate.ddl-auto=update` | Lets Hibernate create/update tables for this study project. |
| `spring.jpa.show-sql=true` | Prints SQL statements in logs. |
| `spring.jpa.properties.hibernate.format_sql=true` | Makes printed SQL easier to read. |

JPA vs Hibernate vs Spring Data JPA

| Term | Meaning |
| --- | --- |
| JPA | Java specification for mapping objects to relational database tables. |
| Hibernate | Popular JPA implementation/provider that actually performs ORM work. |
| Spring Data JPA | Spring project that builds repository implementations around JPA. |
| PostgreSQL driver | JDBC driver that lets Java talk to PostgreSQL. |
| JDBC | Lower-level Java database API used underneath the ORM stack. |

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

Request Lifecycle With DTOs And JPA

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
ProductRepository interface
        |
        v
Spring Data JPA proxy
        |
        v
Hibernate EntityManager
        |
        v
SQL over JDBC
        |
        v
PostgreSQL products table
        |
        v
Product entity
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

Create Product Database Flow

```text
POST /api/products
        |
        v
ProductRequest(name, price)
        |
        v
ProductRequest.toProduct() creates Product with id = null
        |
        v
ProductService.createProduct() keeps id null
        |
        v
productRepository.save(product)
        |
        v
Hibernate INSERT into products(name, price)
        |
        v
PostgreSQL generates id
        |
        v
Hibernate returns Product with generated id
        |
        v
ProductResponse(id, name, price)
```

Update Product Database Flow

```text
PUT /api/products/1
        |
        v
ProductService checks existsById(1)
        |
        v
If missing: ProductNotFoundException -> ApiError 404
        |
        v
If found: set id = 1 on Product
        |
        v
productRepository.save(product)
        |
        v
Hibernate UPDATE products set name=?, price=? where id=?
```

## Interview QA

| Question | Answer |
| --- | --- |
| What changed most from 02 to 03? | Storage changed from an in-memory map to PostgreSQL through Spring Data JPA/Hibernate. The REST API shape stayed almost the same. |
| Why did the controller barely change? | The controller depends on the service and DTOs, not on storage details. Persistence changes are hidden behind the service/repository layers. |
| Why turn `Product` into an `@Entity`? | JPA needs metadata to know that `Product` maps to a database table. |
| What is the difference between an entity and a DTO? | Entity is persistence/internal model. DTO is API input/output shape. They can look similar in small apps but have different responsibilities. |
| Why keep validation on `ProductRequest` instead of moving it to `Product`? | The validation describes what clients may send. Database constraints and API validation are related but not the same thing. |
| What does `JpaRepository<Product, Long>` mean? | This repository manages `Product` entities whose primary key type is `Long`. |
| Where is the repository implementation class? | Spring Data JPA creates it at runtime from the interface. You write the contract; Spring supplies the implementation. |
| How does `findByNameContainingIgnoreCaseAndPriceGreaterThanEqual` work? | Spring Data parses the method name and derives a query using `name contains`, case-insensitive comparison, and `price >=`. |
| Why does `findById` return `Optional<Product>`? | A row may or may not exist for that id, so `Optional` represents the missing case explicitly. |
| Why does service still throw `ProductNotFoundException`? | The service converts repository-level absence into an application-specific failure that the global handler maps to HTTP 404. |
| What does `save` do in JPA? | It persists a new entity or merges changes for an existing entity, depending mostly on whether the id/entity state represents new or existing data. |
| Why set id to `null` during create? | A null id tells JPA this should be treated as a new row, so PostgreSQL can generate the primary key. |
| Why set id from the path during update? | The URL identifies which product is being replaced. The request body cannot choose a different id. |
| What does `GenerationType.IDENTITY` mean? | The database generates the primary key when the row is inserted. |
| What does `ddl-auto=update` do? | Hibernate compares entities with the database schema and tries to update tables automatically. Useful for learning, risky for production. |
| Why is `ddl-auto=update` risky in production? | Schema changes should be reviewed, versioned, and repeatable. Production systems usually use Flyway or Liquibase migrations. |
| What is Hibernate in this project? | Hibernate is the JPA provider that turns repository operations into SQL and maps SQL results back into entities. |
| What is Spring Data JPA adding on top? | Repository interfaces, generated implementations, derived queries, and integration with Spring transactions. |
| What does `spring.jpa.show-sql=true` help with? | It lets you see the SQL generated by Hibernate while studying. |
| Why add `DataSeeder`? | The old in-memory repository constructor seeded products. After moving to JPA, seeding belongs in startup logic, not inside a repository interface. |
| Why does `DataSeeder` check `count()` first? | To avoid inserting duplicate sample products every time the app restarts. |
| What is still not covered in this milestone? | Relationships, lazy loading, joins, N+1 queries, explicit transactions, migrations, and integration tests come in later milestones. |
