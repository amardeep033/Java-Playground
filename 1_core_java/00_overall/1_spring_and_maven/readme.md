# 1 · Spring Framework + Maven

This is a tiny Spring MVC HTTP server without Spring Boot. Maven manages
dependencies and the run command, but the app still wires the web server and
Spring dispatcher explicitly.

## Run

```bash
mvn exec:java
```

Then call:

```bash
curl http://localhost:8080/hello
```

Expected response:

```text
Hello from Spring MVC with Maven
```

## Why This Exists

Spring Framework gives you building blocks:

- `@RestController`
- `@GetMapping`
- dependency injection
- `DispatcherServlet`
- MVC request routing

Without Spring Boot, you manually choose and start the embedded server, register
the servlet, and configure component scanning.

## Interview Questions

- What does Spring add on top of plain Java?
- What is the role of `DispatcherServlet`?
- What does `@RestController` do?
- What does Maven solve compared with manual `javac` classpaths?
- What does Spring Boot remove from this example?
