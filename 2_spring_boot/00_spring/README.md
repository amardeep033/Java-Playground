# 00_spring

## Study Files

- `s0withoutspring`: manual constructor DI in `Main`.
- [s1manualconfig/readme.md](src/main/java/com/example/overall/s1manualconfig/readme.md): Spring DI using `@Configuration` and `@Bean`.
- [s2autoconfig/readme.md](src/main/java/com/example/overall/s2autoconfig/readme.md): Spring DI using `@Component` and `@ComponentScan`.
- [s3diresolution/readme.md](src/main/java/com/example/overall/s3diresolution/readme.md): expands the order/logging example with `@Autowired`, `@Primary`, and `@Qualifier`.
- [s4mvcarchitecture/readme.md](src/main/java/com/example/overall/s4mvcarchitecture/readme.md): expands the order example into MVC roles and controller -> service -> repository.
- [s5beanlifecycle/readme.md](src/main/java/com/example/overall/s5beanlifecycle/readme.md): demonstrates bean lifecycle hooks and singleton, prototype, request, and session scopes.
- [s6configurationandenv/readme.md](src/main/java/com/example/overall/s6configurationandenv/readme.md): combines configuration, properties, environments, and profiles.
- [s7aopandproxies/readme.md](src/main/java/com/example/overall/s7aopandproxies/readme.md): demonstrates proxy-based AOP, advice, pointcuts, caller -> proxy -> target, and self-invocation.
- [s8validation/readme.md](src/main/java/com/example/overall/s8validation/readme.md): demonstrates DTO validation with `@Valid`, Bean Validation constraints, and clean error formatting.
- [s9transactions/readme.md](src/main/java/com/example/overall/s9transactions/readme.md): demonstrates `@Transactional`, transaction manager flow, propagation, rollback, and self-invocation.
- [s10events/readme.md](src/main/java/com/example/overall/s10events/readme.md): demonstrates Spring events, publishers, listeners, ordering, and summarizes transaction-aware event listeners.

## Overall Comparison

The Spring-based examples still use constructor injection. The difference is how the object graph is assembled and managed.

| Version | Wiring style | Who passes the dependency? |
| --- | --- | --- |
| `s0withoutspring` | Manual constructor DI in `Main` | `Main` |
| `s1manualconfig` | Explicit Spring wiring with `@Bean` | Spring container |
| `s2autoconfig` | Spring auto-discovery with `@Component` | Spring container |
| `s3diresolution` | Component scanning with multiple matching beans | Spring container |
| `s4mvcarchitecture` | Component scanning with layered roles | Spring container |
| `s5beanlifecycle` | Component scanning with lifecycle hooks and bean scopes | Spring container |
| `s6configurationandenv` | External properties and profile-based bean selection | Spring container |
| `s7aopandproxies` | Proxy-based AOP with pointcuts and advice | Spring container |
| `s8validation` | DTO validation with Bean Validation annotations | Spring container |
| `s9transactions` | Proxy-based transactions with propagation and rollback | Spring container |
| `s10events` | Application events and event listeners | Spring container |

## Phase 2.1 — `2_spring_boot/`

| Step    | Topic                                     | Depth     | What to cover                                                                                                                     |
| ------- | ----------------------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **s0**  | Overall                                   | 🟢 Done   | Spring, Boot, ApplicationContext, Bean, IoC, DI                                                                                   |
| **s1**  | DI                                        | 🟢 Done   | Manual DI vs Spring DI                                                                                                            |
| **s2**  | Component scanning                        | 🟢 Done   | `@Component`, `@ComponentScan`, automatic bean discovery                                                                          |
| **s3**  | **DI resolution**                         | 🟢 Done   | `@Autowired`, multiple implementations, `@Primary`, `@Qualifier`                                                                  |
| **s4**  | **Application architecture + MVC**        | 🟢 Done   | Layered architecture, Spring MVC request flow, `@Controller`, `@Service`, `@Repository`, controller -> service -> repository      |
| **s5**  | **Beans**                                 | 🟢 Done   | Creation → dependency injection → initialization → ready → destruction; `@PostConstruct`, `@PreDestroy`; scopes                   |
| **s6**  | **Configuration + Environment**           | 🟢 Done   | `@Configuration`, `@Bean`, `@Value`, `Environment`, properties, `@Profile`, `dev/test/prod`, property precedence                  |
| **s7**  | **AOP / Proxies**                         | 🟢 Done   | Proxy, target, advice, pointcut, caller → proxy → target, self-invocation                                                         |
| **s8**  | **Validation**                            | 🟢 Done   | DTO, `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Email`                                                                         |
| **s9**  | **Transactions**                          | 🟢 Done   | `@Transactional`, proxy, begin → execute → commit/rollback, `REQUIRED`, `REQUIRES_NEW`, self-invocation                           |
| **s10** | **Events**                                | 🟢 Done   | Event → publisher → listener, `@EventListener`                                                                                    |
