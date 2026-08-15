# s8validation

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s8validation.Main
```

Expected mental-model shape:

```text
Valid DTO
C1. Controller accepted DTO
S1. Service creates order for keyboard

Invalid DTO
E1. customerEmail -> customerEmail must be a valid email
E1. itemName -> itemName must be between 2 and 30 characters
E1. itemName -> itemName must not be blank
E1. quantity -> quantity must not be null
```

## Cheatsheet: Validation Flow

| Step | In a web API | In this console example |
| --- | --- | --- |
| Request enters | HTTP body arrives as JSON | `Main` creates `OrderRequest` manually. |
| DTO binding | `@RequestBody OrderRequest` | `new OrderRequest(...)` |
| Trigger validation | `@Valid @RequestBody` | `@Valid OrderRequest` method parameter |
| Rules live on | DTO fields | `OrderRequest` record components |
| Invalid result | `MethodArgumentNotValidException` in MVC | `ConstraintViolationException` from method validation |
| Clean error response | `@ControllerAdvice` + `@ExceptionHandler` | `ValidationErrorFormatter` prints field errors |

## Cheatsheet: Annotations

| Annotation | Where used | Meaning |
| --- | --- | --- |
| `@Valid` | Controller/service method parameter or nested field | Validate this object using Bean Validation rules. |
| `@NotNull` | Any nullable type | Value must not be `null`; empty string is still allowed. |
| `@NotBlank` | `String` | Value must not be `null`, empty, or only whitespace. |
| `@Size` | `String`, collection, array, map | Length/size must be within the configured range. |
| `@Email` | `String` | Value must look like a valid email address. |
| `@Validated` | Spring bean class or method | Enables Spring method validation and supports validation groups. |

## Cheatsheet: `@Valid` vs `@Validated`

| Topic | `@Valid` | `@Validated` |
| --- | --- | --- |
| Provided by | Jakarta Bean Validation | Spring |
| Common use | Validate DTO object graph | Enable method validation or validation groups |
| Normal request DTO | Usually enough | Optional unless you need Spring-specific features |
| Nested object validation | Yes | Yes |
| Validation groups | No | Yes |

## Interview Q&A

| Question | Strong answer |
| --- | --- |
| What is a DTO? | A data transfer object that represents input/output at the application boundary. Validation annotations usually belong here, not on random business logic. |
| What does `@Valid` do? | It asks Bean Validation to validate the annotated object using constraints such as `@NotBlank`, `@Size`, and `@Email`. |
| What is the difference between `@NotNull` and `@NotBlank`? | `@NotNull` only rejects `null`. `@NotBlank` rejects `null`, empty strings, and strings containing only whitespace. |
| What happens when validation fails in Spring MVC? | For `@Valid @RequestBody`, Spring usually throws `MethodArgumentNotValidException` before the controller method body runs. |
| What is `@ControllerAdvice` for? | It centralizes MVC exception handling across controllers, often to return a consistent validation-error response. |
| Is `@ControllerAdvice` Spring AOP advice? | No. It is conceptually cross-cutting, but it belongs to Spring MVC exception/controller handling, not proxy-based AOP pointcuts. |
| Why does this console example use `ConstraintViolationException`? | It demonstrates method validation outside Spring MVC. MVC request-body validation uses a different exception because the failure happens during request binding. |
| When should I use `@Validated`? | Use it when you need Spring method validation or validation groups. For a normal request DTO, `@Valid` is usually enough. |
