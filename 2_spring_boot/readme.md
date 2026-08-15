## Phase 2.1 — `2_spring_boot/`

| Step    | Topic                                     | Depth     | What to cover                                                                                                                     |
| ------- | ----------------------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **s0**  | Overall                                   | 🟢 Done   | Spring, Boot, ApplicationContext, Bean, IoC, DI                                                                                   |
| **s1**  | DI                                        | 🟢 Done   | Manual DI vs Spring DI                                                                                                            |
| **s2**  | Component scanning                        | 🟢 Done   | `@Component`, `@ComponentScan`, automatic bean discovery                                                                          |
| **s3**  | **Application architecture + MVC**        | 🟠 Medium | Layered architecture, DDD vs Clean Architecture, Spring MVC request flow                                                          |
| **s4**  | **Component stereotypes + DI resolution** | 🟠 Medium | `@Service`, `@Repository`, `@Controller`, constructor injection, `@Autowired`, multiple implementations, `@Primary`, `@Qualifier` |
| **s5**  | **Beans**                                 | 🔴 Deep   | Creation → dependency injection → initialization → ready → destruction; `@PostConstruct`, `@PreDestroy`; scopes                   |
| **s6**  | **Configuration**                         | 🟠 Medium | `@Configuration`, `@Bean`, `@Value`, `@ConfigurationProperties`, Java config → bean definitions → context                         |
| **s7**  | **AOP / Proxies**                         | 🔴 Deep   | Proxy, target, advice, pointcut, caller → proxy → target, self-invocation                                                         |
| **s8**  | **Validation**                            | 🟢 Light  | DTO, `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Email`                                                                         |
| **s9**  | **Transactions**                          | 🔴 Deep   | `@Transactional`, proxy, begin → execute → commit/rollback, `REQUIRED`, `REQUIRES_NEW`, self-invocation                           |
| **s10** | **Events**                                | 🟢 Light  | Event → publisher → listener, `@EventListener`                                                                                    |
| **s11** | **Profiles & Properties**                 | 🟢 Light  | `dev/test/prod`, `@Profile`, properties/YAML                                                                                      |

## Phase 2.2 — `2_spring_boot/`

| # | Folder | Topics |
|---|---|---|
|00|`00_overall/`|Boot architecture, standard project layout|
|01|`01_rest_crud/`|CRUD APIs|
|02|`02_database/`|Spring Data JPA, Hibernate|
|03|`03_configuration_logging/`|`application.yml`; links to `1_core_java/17_logging`|
|04|`04_testing/`|Spring/Boot integration testing only — links to `1_core_java/16_testing`|
|05|`05_grpc/`|gRPC with Spring|
|06|`06_swagger/`|OpenAPI|
|07|`07_actuator/`|Monitoring|
|08|`08_security_jwt/`|Spring Security + JWT + OAuth2 — **owns security for the whole roadmap**|
