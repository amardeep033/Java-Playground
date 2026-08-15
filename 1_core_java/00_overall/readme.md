# 0 · Overall

## Topics Covered

| No | Area | Topics |
|---|---|---|
|00|`00_overall/`|WORA, JVM vs JRE vs JDK, how to compile/run Java, Java vs Spring vs Spring Boot, Maven vs Gradle|


## Folder Map

| folder | what it shows |
|---|---|
| [`0_core_java`](0_core_java/readme.md) | plain Java hello world using `javac` and `java` |
| [`1_spring_and_maven`](1_spring_and_maven/readme.md) | Spring Framework web endpoint using Maven, without Spring Boot |
| [`2_spring_boot_and_gradle`](2_spring_boot_and_gradle/readme.md) | Spring Boot web endpoint using Gradle |

## Java vs Spring vs Spring Boot

| item | meaning | backend interview angle |
|---|---|---|
| Java | language + standard library + runtime platform | syntax, OOP, collections, exceptions, concurrency, JVM behavior |
| Spring | framework for dependency injection, web MVC, data, transactions, security, etc. | inversion of control, beans, request lifecycle, transactions |
| Spring Boot | opinionated layer over Spring that auto-configures common production defaults | starters, auto-configuration, embedded server, actuator, config |

Spring Boot uses Spring. Spring uses Java. If something breaks in production,
you often debug all three layers: your Java code, Spring framework behavior,
and Boot auto-configuration.

## Normal Build vs Maven vs Gradle

| style | command shape | what you manage |
|---|---|---|
| plain Java | `javac Main.java`, `java Main` | source files and classpath manually |
| Maven | `mvn compile`, `mvn test`, `mvn exec:java` | dependencies, lifecycle, plugins through `pom.xml` |
| Gradle | `gradle bootRun`, `gradle test` | dependencies and tasks through `build.gradle` |

## JVM, JRE, JDK, WORA

| term | meaning |
|---|---|
| JVM | Java Virtual Machine. Runs compiled `.class` bytecode. |
| JRE | JVM plus runtime libraries needed to run Java apps. |
| JDK | JRE plus developer tools like `javac`, `jar`, `jdb`, `jshell`. |
| WORA | Write Once, Run Anywhere: compile Java to bytecode once, run it on any compatible JVM. |

Interview nuance: WORA is not magic. OS-specific files, networking, native
libraries, CPU architecture, container limits, and environment configuration can
still change runtime behavior.

## First Pass Checklist

- Explain what `public static void main(String[] args)` means.
- Run a Java class without Maven or Gradle.
- Start a Spring MVC endpoint and explain what `DispatcherServlet` does.
- Start a Spring Boot endpoint and explain what Boot configures for you.
- Explain when Maven/Gradle help: dependency resolution, repeatable builds,
  test lifecycle, plugins, packaging.
