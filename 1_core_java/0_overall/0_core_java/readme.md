# 0 · Core Java Hello World

Smallest possible Java program.

## Run

```bash
javac Main.java
java Main
```

Expected output:

```text
Hello from plain Java
```

## What Happens

| step | command | output |
|---|---|---|
| compile | `javac Main.java` | creates `Main.class` bytecode |
| run | `java Main` | starts JVM and calls `main` |

## Key Ideas

| term | interview answer |
|---|---|
| JDK | tools to develop Java, including `javac` |
| JRE | runtime environment to run Java apps |
| JVM | virtual machine that executes bytecode |
| WORA | Java source compiles to bytecode that can run on compatible JVMs |

## Interview Questions

- Why does Java compile to bytecode instead of directly to machine code?
- What is the difference between `javac Main.java` and `java Main`?
- Why is `main` static?
- What does `String[] args` receive?
- What can still break WORA in real backend deployments?
