# s7aopandproxies

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s7aopandproxies.Main
```

Expected mental-model shape:

```text
M1. orderController false
M2. orderService true

C1. placeOrderFromOutside
Before
L1. logBeforeOrderPlacement placeOrderE
S1. placeOrderE
S0. calculateTotal
After

C2. placeOrderWithSelfInvocation
Before
L1. logBeforeOrderPlacement placeOrderI
S2. placeOrderI
S0. calculateTotal
After

Before
S0. calculateTotal
L2. totalCalculationOperation calculateTotal returned 900
After
```

## Cheatsheet: Mental Model

| Flow | Meaning | Advice runs? |
| --- | --- | --- |
| `OrderController -> orderService.placeOrderE(...)` | External bean calls the Spring-managed `OrderService` reference. That reference is a proxy. | Yes: `@Around` and `@Before` both run. |
| `OrderService.placeOrderE(...) -> calculateTotal(...)` | Internal call inside the same target object. Java treats it like `this.calculateTotal(...)`. | No, because the call does not re-enter the proxy. |
| `OrderService.placeOrderI(...) -> this.calculateTotal(...)` | Explicit self-invocation. Same behavior as the previous row, just easier to see. | No. |
| `Main -> orderService.calculateTotal(...)` | External call through the Spring bean reference. | Yes: `@Around` and `@AfterReturning` both run. |
| `context.getBean(OrderController.class)` | Controller bean is real in this example because no advice targets it. | Not proxied. |
| `context.getBean(OrderService.class)` | Service bean is proxied because `LoggingAspect` targets its methods. | Proxied. |

## Cheatsheet: Core Pieces

| Piece | In this example | What to remember |
| --- | --- | --- |
| Target | `OrderService` | The real object containing business logic. |
| Proxy | Spring-created `OrderService$$SpringCGLIB$$...` object | The object callers receive; it intercepts external calls. |
| Advice | `logBeforeOrderPlacement`, `logAfterTotalCalculation`, `log` | What extra behavior runs. |
| Pointcut | `orderPlacementOperation`, `totalCalculationOperation` | Which method executions should receive advice. |
| Aspect | `LoggingAspect` | Groups related pointcuts and advice. |
| JoinPoint | `JoinPoint joinPoint` | Metadata about the matched method call. |
| ProceedingJoinPoint | `ProceedingJoinPoint pjp` | Used by `@Around`; `pjp.proceed()` calls the target method. |

## Cheatsheet: Pointcut Designators

These are AspectJ pointcut expression designators, not Java annotations, except `@Pointcut` itself.

| Designator | How to write | What it matches | Example use |
| --- | --- | --- | --- |
| `@Pointcut` | `@Pointcut("execution(* com.example..service..*(..))")` | Defines a reusable pointcut expression method. | Put the long expression in one place and reuse it from multiple advice methods. |
| `execution(...)` | `execution(* com.example.overall.s7aopandproxies.service.OrderService.place*(..))` | Method executions by return type, package/class, method name, and arguments. | "Run advice for `place...` methods on `OrderService`." |
| `within(...)` | `within(com.example..service..*)` | Code executing inside matching classes/packages by type pattern. | "Run advice for methods declared inside service package classes." |
| `@annotation(...)` | `@annotation(org.springframework.transaction.annotation.Transactional)` | Methods that carry a specific annotation. | "Run advice only for methods annotated with `@Transactional` or a custom annotation." |
| `@within(...)` | `@within(org.springframework.stereotype.Service)` | Join points where the declaring/containing class has a specific annotation. | "Run advice for methods declared in classes annotated with `@Service`." |

## Cheatsheet: Advice Annotations

| Advice annotation | How to write | When it runs | Best mental model |
| --- | --- | --- | --- |
| `@Before` | `@Before("orderPlacementOperation()")` | Before the matched target method. | Check/log before target work starts. |
| `@After` | `@After("pointcutName()")` | After the method finishes, whether it returns or throws. | `finally` block. |
| `@AfterReturning` | `@AfterReturning(pointcut = "totalCalculationOperation()", returning = "total")` | Only after normal return. Can capture the returned value. | Success-only post-processing. |
| `@AfterThrowing` | `@AfterThrowing(pointcut = "pointcutName()", throwing = "ex")` | Only when the target throws. Can capture the exception. | Failure-only logging/auditing. |
| `@Around` | `@Around("execution(* com.example..*(..))")` | Around the method; you choose when or whether to call `proceed()`. | Manual wrapper: before -> target -> after. |

## Cheatsheet: Common Expressions

| Want | Expression | Notes |
| --- | --- | --- |
| Any return type | `execution(* ...)` | The first `*` means any return type. |
| Any method name in one class | `execution(* com.example.PaymentService.*(..))` | `*` after class means any method name. |
| Names starting with `place` | `execution(* com.example.OrderService.place*(..))` | Matches `placeOrderE`, `placeOrderI`, etc. |
| Any arguments | `(..)` | Zero or more arguments of any type. |
| No arguments | `()` | Exactly zero arguments. |
| One argument of any type | `(*)` | Exactly one argument. |
| Package and subpackages | `com.example..service..*` | `..` includes subpackages. |
| Method has annotation | `@annotation(com.example.TrackTime)` | Annotation must be on the method. |
| Class has annotation | `@within(org.springframework.stereotype.Service)` | Annotation must be on the target class. |

## Interview Q&A

| Question | Strong answer |
| --- | --- |
| Why is `OrderService` proxied but `OrderController` is not? | Because the pointcuts in `LoggingAspect` match `OrderService` methods only. A bean becomes proxied when some enabled proxy-based infrastructure has advice for it. `@Controller` alone does not mean proxy. |
| How do you check whether a bean is proxied? | Use `AopUtils.isAopProxy(bean)`. `bean.getClass()` can also reveal generated names like `$$SpringCGLIB$$`, but `AopUtils` is clearer. |
| What does `$$SpringCGLIB$$` mean? | Spring generated a class-based CGLIB proxy, usually a subclass of the target class. Calls enter that proxy before reaching the target. |
| What is the difference between JDK dynamic proxy and CGLIB proxy? | JDK proxy implements interfaces. CGLIB proxy subclasses the target class. `proxyTargetClass = true` asks Spring to use class-based proxying. |
| What does "without an interface" mean here? | `OrderService` does not implement an interface like `OrderOperations`. Without an interface, JDK proxying has no application-facing interface to implement, so class-based CGLIB proxying is used. |
| Does `@Pointcut` execute code? | No. A `@Pointcut` method is a named expression. Advice annotations such as `@Before` or `@AfterReturning` consume that expression and execute behavior. |
| What is the difference between `execution(...)` and `within(...)`? | `execution(...)` matches method executions with method-level detail: return type, method name, arguments. `within(...)` matches join points inside matching types/packages. Use `execution` when method signature matters; use `within` when package/class boundary matters. |
| What is the difference between `within(...)` and `@within(...)`? | `within(...)` matches by type/package pattern, such as `within(com.example..service..*)`. `@within(...)` matches declaring/containing classes carrying an annotation, such as `@within(org.springframework.stereotype.Service)`. |
| What is the difference between `@annotation(...)` and `@within(...)`? | `@annotation(...)` checks the method annotation. `@within(...)` checks the class annotation on the declaring/containing type. A method in a `@Service` class matches `@within(Service)` even if the method itself has no annotation. |
| Why does `calculateTotal(itemName)` inside `placeOrderE` skip advice? | In Java, an unqualified method call inside the same class is effectively `this.calculateTotal(itemName)`. It is self-invocation, so the proxy is bypassed. |
| Why does `this.calculateTotal(itemName)` inside `placeOrderI` skip advice? | It directly calls another method on the same target object. The call path is target -> target, not caller -> proxy -> target. |
| Why does `orderService.calculateTotal("main_item")` from `Main` trigger `@Around` and `@AfterReturning`? | `Main` gets `OrderService` from Spring, so it holds the proxy reference. The call enters through the proxy and matches both the broad `@Around` expression and `totalCalculationOperation()`. |
| Why do people say self-invocation "may not" apply `@Transactional`? | In normal Spring proxy-based AOP, self-invocation bypasses the proxy, so transaction advice does not run for the inner method. "May not" leaves room for non-proxy mechanisms such as AspectJ weaving or cases where no advice matches anyway. |
| Is `@Around` the same as `@Transactional`? | No. `@Around` is a custom advice annotation inside your `@Aspect`. `@Transactional` is a ready-made Spring transaction annotation handled by transaction infrastructure. Both commonly work through proxies. |
| Why do `Before` and `After` print only once around `placeOrderE`, even though `placeOrderE` calls `calculateTotal`? | `placeOrderE` entered through the proxy, so `@Around` runs for `placeOrderE`. The nested `calculateTotal` call is self-invocation, so it does not enter the proxy and does not get its own `@Around` wrapper. |
| Do we need `@EnableAspectJAutoProxy` for `@Around`? | In plain Spring, yes, for custom `@Aspect` advice. It tells Spring to process `@Aspect` beans and create proxies. Spring Boot may auto-configure parts of this in some setups, but this plain example enables it explicitly. |
| Do we need `@EnableAspectJAutoProxy` for `@Transactional`? | Not normally. Transactions use `@EnableTransactionManagement` or Spring Boot transaction auto-configuration, not `@EnableAspectJAutoProxy` directly. |
| What happens if `@Around` does not call `pjp.proceed()`? | The target method is not executed. `@Around` can block, replace, retry, time, or wrap the method because it controls the call to the target. |
| Can `@AfterReturning` capture anything? | It can capture the returned value of a normally completed method. The `returning` name must match the advice parameter name, and the parameter type must be compatible with the returned value. |
| Will `@AfterReturning` run when the target throws? | No. Use `@AfterThrowing` for exceptions, or `@After` for logic that should run in both success and failure cases. |
| Can private methods be advised by Spring AOP? | Not with normal proxy-based Spring AOP. The proxy intercepts external method calls; private methods are not called through the proxy. |
| Does Spring AOP change the target class bytecode? | Normal Spring AOP does not modify the target bytecode. It wraps the target in a proxy. AspectJ weaving is different and can modify/weave behavior more deeply. |
