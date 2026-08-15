# s5beanlifecycle

## Goal

Learn the Spring bean lifecycle, `@PostConstruct`, `@PreDestroy`, and the singleton, prototype, request, and session scopes through the same order example.

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s5beanlifecycle.Main
```

## Cheat Sheet

```text
Container starts
    -> constructor
    -> dependency injection
    -> @PostConstruct
    -> bean ready for use
    -> container closes / scope ends
    -> @PreDestroy
```

| Scope | Instances | Lifetime | Typical use |
| --- | --- | --- | --- |
| Singleton | One per ApplicationContext | Until the context closes | Stateless services and shared infrastructure |
| Prototype | New instance per container lookup | Managed by Spring only through creation and initialization | Independent, short-lived mutable objects |
| Request | One per HTTP request | Until that request completes | Request-specific context or state |
| Session | One per HTTP session | Until that session expires or is invalidated | User-session state such as a shopping cart |

Important details:

- Singleton is Spring's default scope; it does not mean one object per JVM.
- A prototype injected directly into a singleton is resolved only once when the singleton is created. `ObjectProvider<T>` can request a fresh prototype when needed.
- Spring does not automatically run destruction callbacks for prototype beans.
- Ordinary domain objects are usually created with `new`; prototype scope is most useful when every fresh instance needs container-managed dependencies.
- Request and session scopes need an active web-aware application context.
- Scoped proxies let a singleton hold a proxy that finds the correct request/session target when a method is called.

## Why Lifecycle Matters

Use `@PostConstruct` when initialization must happen after Spring has injected every dependency: validate configuration, warm a cache, initialize an SDK client, or start a managed worker.

Use `@PreDestroy` to release resources owned by the bean: close clients or pools, stop executors and worker threads, flush buffers, and unregister listeners.

Prefer constructors for ordinary field assignment and invariants. Lifecycle hooks are for work connected to container startup and shutdown, not for moving normal constructor code elsewhere.

## Interview Q&A

| Question | Strong short answer |
| --- | --- |
| What is a Spring bean lifecycle? | It is the sequence from bean creation and dependency injection through initialization, use, and destruction. |
| When does `@PostConstruct` run? | Once after Spring has injected the bean's dependencies and before the bean is considered ready for normal use. |
| When does `@PreDestroy` run? | Before a fully managed bean is destroyed, commonly when its scope ends or the ApplicationContext closes. |
| Should application code call lifecycle methods manually? | Normally no. Spring invokes them; application code should close the ApplicationContext so destruction callbacks can run. |
| What is Spring's default scope? | Singleton: one bean instance per ApplicationContext and bean definition. |
| Is a Spring singleton one object per JVM? | No. Separate ApplicationContexts can each contain their own singleton instance. |
| If singleton beans are shared by many requests and threads, why shouldn't we store request-specific mutable data inside a singleton service? | Concurrent requests use the same object, so mutable fields can cause race conditions or leak one user's data into another request. Keep singleton services stateless and keep request data in method-local variables or request-scoped objects. |
| What does prototype scope mean? | Spring creates a new instance each time that bean is requested from the container. |
| Does Spring fully manage prototype destruction? | No. Spring creates and initializes it, but the consumer must clean up resources owned by the prototype. |
| Why use `ObjectProvider<OrderDraft>`? | The singleton `OrderService` can ask Spring for a fresh prototype `OrderDraft` for each order. |
| Should every short-lived domain object be a prototype bean? | No. Usually create simple domain objects with `new`; use prototype when fresh instances need Spring-managed construction or dependencies. |
| What is request scope? | One bean instance per HTTP request. It requires an active web request. |
| What is session scope? | One bean instance per HTTP session, shared by requests belonging to that session. |
| Why use a scoped proxy? | It lets a long-lived singleton refer to the correct shorter-lived request or session bean at runtime. |
| What happens if request scope is resolved without an active request? | Spring throws a scope-not-active exception because no request-bound instance can exist. |
| Why might `@PreDestroy` not run? | The process may be killed abruptly, the context may not be closed, or the bean may be prototype-scoped. |
