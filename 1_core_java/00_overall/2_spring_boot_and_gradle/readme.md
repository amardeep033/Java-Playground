# 2 · Spring Boot + Gradle

This is the same `GET /hello` idea, but Spring Boot starts the embedded server
and auto-configures Spring MVC.

## Run

```bash
gradle bootRun
```

If you use the Gradle wrapper later, prefer:

```bash
./gradlew bootRun
```

Then call:

```bash
curl http://localhost:8080/hello
```

Expected response:

```text
Hello from Spring Boot with Gradle
```

## Why This Exists

Spring Boot gives you conventions and auto-configuration:

- embedded Tomcat by default
- Spring MVC setup
- JSON serialization defaults
- dependency versions through the Boot plugin
- production-friendly extensions like Actuator when added

Gradle gives you a task-based build model. In interviews, compare it with Maven
without turning the answer into "which one is better"; focus on lifecycle,
dependency management, plugins, and team conventions.

## Interview Questions

- What does `@SpringBootApplication` include?
- What is auto-configuration?
- What is a starter dependency?
- Why does this example need less explicit server setup than the Maven Spring example?
- What is the difference between Gradle tasks and Maven lifecycle phases?
