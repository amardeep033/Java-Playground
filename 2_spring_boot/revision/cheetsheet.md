# Spring Boot REST API — Machine Coding Cheat Sheet

For quick CRUD machine-coding rounds. Spring MVC, Spring Boot, Spring Data JPA. Examples assume modern Spring Boot with `jakarta.*` imports.

## 1. ResponseEntity<T> (`org.springframework.http`)

| Expression | Result |
|---|---|
| `ResponseEntity.ok(data)` | 200 + body |
| `ResponseEntity.ok().build()` | 200 without body |
| `ResponseEntity.status(HttpStatus.CREATED).body(data)` | 201 + body |
| `ResponseEntity.created(uri).body(data)` | 201 + Location header + body |
| `ResponseEntity.noContent().build()` | 204 |
| `ResponseEntity.badRequest().build()` | 400 |
| `ResponseEntity.badRequest().body(error)` | 400 + body |
| `ResponseEntity.notFound().build()` | 404 |
| `ResponseEntity.status(HttpStatus.CONFLICT).body(error)` | 409 + body |
| `ResponseEntity.of(optional)` | 200 + value / 404 if empty |

- `.body(value)` adds a body, `.build()` completes without body.
- Returning a plain object from `@RestController` normally means 200 plus serialized JSON; `ResponseEntity` is optional.
- Empty list is typically 200 + `[]`, **not** 404.

## 2. Controller annotations (`org.springframework.web.bind.annotation`)

| Annotation | Use |
|---|---|
| `@RestController` | REST controller, response body serialization |
| `@RequestMapping("/products")` | Base route |
| `@GetMapping` | GET |
| `@PostMapping` | POST |
| `@PutMapping("/{id}")` | PUT |
| `@PatchMapping("/{id}")` | PATCH |
| `@DeleteMapping("/{id}")` | DELETE |
| `@PathVariable` | `/products/10` -> id |
| `@RequestParam` | `/products?page=0` -> page |
| `@RequestBody` | JSON body -> Java object |
| `@ResponseStatus(HttpStatus.CREATED)` | Static response status |

```java
@GetMapping("/{id}")
public Product get(@PathVariable Long id) { ... }

@GetMapping
public List<Product> list(@RequestParam(defaultValue = "0") int page) { ... }

@PostMapping
public Product create(@RequestBody Product product) { ... }
```

- Optional param: `@RequestParam(required = false) String category`.
- `@Valid @RequestBody CreateProductRequest request` validates a JSON DTO.

## 3. JpaRepository<T, ID> (`org.springframework.data.jpa.repository`)

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    Optional<Product> findByName(String name);
    List<Product> findByPriceGreaterThan(double price);
}
```

| Method | Meaning |
|---|---|
| `save(product)` | Save/update and return entity |
| `findById(id)` | `Optional<Product>` |
| `findAll()` | `List<Product>` |
| `existsById(id)` | `boolean` |
| `deleteById(id)` | Delete by id |
| `delete(product)` | Delete entity |
| `count()` | `long` |
| `findAll(Pageable)` | `Page<Product>` |
| `findAll(Sort)` | Sorted list |

## 4. Spring beans

| Annotation | Purpose |
|---|---|
| `@SpringBootApplication` | App startup config, scan, auto-config |
| `@Component` | Generic bean |
| `@Service` | Service bean |
| `@Repository` | Persistence bean |
| `@Configuration` | Configuration class |
| `@Bean` | Bean factory method |
| `@Autowired` | Inject dependency (optional on single constructor) |
| `@Qualifier("name")` | Choose bean among candidates |
| `@Value("${app.name}")` | Inject property |

```java
@Service
class ProductService {
    private final ProductRepository repo;
    ProductService(ProductRepository repo) { this.repo = repo; }
}
```

## 5. JPA entity (`jakarta.persistence`)

| Annotation | Purpose |
|---|---|
| `@Entity` | Entity |
| `@Table(name = "products")` | Table name |
| `@Id` | Primary key |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Auto-generated key |
| `@Column(nullable = false)` | Non-null DB column |
| `@Enumerated(EnumType.STRING)` | Persist enum as string |
| `@Transient` | Not persisted |
| `@Transactional` (`org.springframework.transaction.annotation`) | Transaction boundary |

```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
    protected Product() {} // required by JPA
    // other constructors, getters and setters
}
```

## 6. Validation (`jakarta.validation`)

| Annotation | Use |
|---|---|
| `@Valid` | Trigger validation |
| `@NotNull` | Must not be null |
| `@NotBlank` | Nonblank string |
| `@NotEmpty` | Nonempty string/collection |
| `@Size(min=2,max=50)` | Size limits |
| `@Min(1)`, `@Max(100)` | Numeric bounds |
| `@Positive` | Greater than zero |
| `@Email` | Email syntax |

```java
public record CreateProductRequest(@NotBlank String name, @Positive double price) {}
```

Needs `spring-boot-starter-validation`.

## 7. Exceptions

| API | Use |
|---|---|
| `ResponseStatusException` | Throw an HTTP status exception |
| `@ExceptionHandler` | Handler for exception type |
| `@RestControllerAdvice` | Global REST exception handling |

```java
Product product = repo.findById(id).orElseThrow(() ->
    new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
```

```java
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> handle(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

## 8. Five common endpoints — controller skeleton

```java
@RestController
@RequestMapping("/products")
class ProductController {
    private final ProductRepository repo;
    ProductController(ProductRepository repo) { this.repo = repo; }

    @GetMapping
    List<Product> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    ResponseEntity<Product> get(@PathVariable Long id) {
        return ResponseEntity.of(repo.findById(id));
    }

    @PostMapping
    ResponseEntity<Product> create(@RequestBody Product product) {
        Product saved = repo.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product input) {
        return repo.findById(id).map(existing -> {
            existing.setName(input.getName());
            existing.setPrice(input.getPrice());
            return ResponseEntity.ok(repo.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

For a real app, usually move business logic to a `@Service`, use DTOs, and handle concurrent deletion, validation, and database errors as appropriate.

## 9. Java helpers you will use

| API | Common calls |
|---|---|
| `Optional<T>` | `.isPresent()`, `.get()`, `.orElse()`, `.orElseThrow()`, `.map()` |
| `List<T>` | `.add()`, `.get()`, `.size()`, `.isEmpty()`, `.stream()` |
| `Map<K,V>` | `.put()`, `.get()`, `.getOrDefault()`, `.containsKey()`, `.remove()` |
| `Stream<T>` | `.filter()`, `.map()`, `.toList()`, `.findFirst()` |
| `PageRequest` | `PageRequest.of(page, size)` |
| `Sort` | `Sort.by("name").ascending()` |

## 10. Imports you tend to forget

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
```

## 11. Minimal dependencies

For Spring Boot 3 projects, common starters are `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, plus the database driver. For Spring Boot 4 MVC projects, use `spring-boot-starter-webmvc` instead of the older web starter naming.

## Sources

- [Spring MVC request mapping](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)
- [Spring ResponseEntity](https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/http/ResponseEntity.java)
- [Spring Data JPA repository API](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/JpaRepository.html)