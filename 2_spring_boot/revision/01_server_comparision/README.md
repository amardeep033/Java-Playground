# Spring vs Spring Boot: one GET API

Only the **Read** operation of CRUD: `GET /students`. No database or HTML views.
This uses Spring MVC to return JSON through `@RestController`.

```text
GET /students → StudentController → StudentService → StudentRepository
             ← JSON list of Student objects
```

| Code / setup | Plain Spring | Spring Boot |
| --- | --- | --- |
| Model, repository, service, controller | Same four files | Same four files |
| Component scanning | `@ComponentScan` | Included in `@SpringBootApplication` |
| MVC configuration | `@EnableWebMvc` | Auto-configured |
| DispatcherServlet registration | `WebInitializer` | Auto-configured |
| Server | Deploy WAR to external Tomcat | Embedded Tomcat starts from `main()` |
| Dependencies | Declare MVC, JSON, Servlet API and versions | Web starter + managed versions |

Start with `ComparisonApplication.java`: the running Boot code is at the top;
the equivalent plain Spring configuration is below it **as a comment**.
`pom.xml` also comments on the dependency/build differences.
The commented Spring alternative illustrates a traditional WAR deployment.

Boot saves setup code here. It still uses Spring MVC and the same application code.

## Run

Package structure:

```text
src/main/java/com/example/comparison/
├── ComparisonApplication.java
├── controller/
│   └── StudentController.java
├── model/
│   └── Student.java
├── repository/
│   └── StudentRepository.java
└── service/
    └── StudentService.java
```

Requires Java 17+ and Maven.

```bash
mvn spring-boot:run
```

In another terminal:

```bash
curl http://localhost:8080/students
```

```json
[{"id":1,"name":"Amar"},{"id":2,"name":"Priya"}]
```

Reference: [Spring Boot MVC auto-configuration](https://docs.spring.io/spring-boot/3.5/reference/web/servlet.html).
