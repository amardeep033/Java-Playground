# s4mvcarchitecture

## Goal

Learn how Spring discovers and connects a layered `Controller -> Service -> Repository` structure. This is plain Spring: `Main` simulates a request, so no web server or real database is involved.

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s4mvcarchitecture.Main
```

## Cheat Sheet

```text
Simulated request
    -> OrderController
        -> OrderService
            -> OrderRepository
                -> InMemoryOrderRepository
            <- OrderReceipt
        <- OrderReceipt
    <- String response
```

| Part | Purpose |
| --- | --- |
| `@Controller` | Marks the web/input layer that receives requests and creates responses. |
| `@Service` | Marks the layer that coordinates use cases and business flow. |
| `@Repository` | Marks the data-access layer and supports persistence exception translation. |
| `OrderRepository` | Keeps the service dependent on a storage abstraction. |
| `OrderReceipt` | Shared model used by all layers in this small example. |
| Constructor injection | Makes each dependency explicit; `@Autowired` is unnecessary when there is one constructor. |
| `@ComponentScan` | Discovers the stereotype-annotated classes and registers them as beans. |

Real Spring MVC adds web infrastructure before the same layered flow:

```text
Client -> DispatcherServlet -> HandlerMapping -> Controller -> Service -> Repository -> Database
```

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| What is MVC? | Model-View-Controller separates application data, presentation, and request handling. In REST APIs, the response body usually replaces a server-rendered view. |
| Is MVC the same as layered architecture? | No. MVC is a presentation pattern; Controller -> Service -> Repository is layered application architecture. They are commonly used together. |
| What is the controller responsible for? | It handles transport concerns, delegates work to the service, and creates the response. |
| What is the service responsible for? | It coordinates the use case and contains application or business flow. |
| What is the repository responsible for? | It hides persistence details behind a data-access abstraction. |
| Why not use `@Component` for every class? | The specialized stereotypes are components but communicate layer intent; `@Controller` and `@Repository` also enable layer-specific framework behavior. |
| Why use an `OrderRepository` interface? | The service depends on an abstraction, allowing the storage implementation to change without changing service code. |
| Is `OrderReceipt` a DTO, domain model, or entity here? | It is one shared application model for this small example. Larger applications may use separate DTO, domain, and persistence models. |
| Is `@Autowired` required on these constructors? | No. Spring automatically uses a class's single constructor. |
| What happens if `InMemoryOrderRepository` loses `@Repository`? | Component scanning does not register it, so Spring cannot satisfy the `OrderRepository` dependency and context startup fails. |
| What calls a controller for a real Spring MVC request? | `DispatcherServlet` receives the request and uses Spring MVC's handler mappings to invoke the matching controller method. |
| How does Clean Architecture differ from layered architecture? | Clean Architecture requires source-code dependencies to point inward toward application and domain rules; traditional layers often depend downward. |
| Is Clean Architecture required for DDD? | No. They can complement each other, but DDD is a domain-modeling approach and Clean Architecture is an architectural dependency approach. |
