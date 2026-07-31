# Maven Notes

Maven is a Java build tool and dependency manager. It can create a project,
download third-party libraries, compile code, run tests, and package the final
application as a JAR.

## Why Maven Helps

Java includes the standard library, such as `java.io`, `java.nio`, and
`java.util`. Libraries like Jackson and Picocli are not part of the JDK.

Maven solves that by downloading library JARs and placing them on the classpath
when the project builds or runs.

Simple mental model:

| Tool | Role |
| --- | --- |
| JDK | Built-in Java toolbox |
| Third-party JAR | Extra toolbox added to the project |
| Maven | Tool that downloads, organizes, builds, and runs the project |

## Project Layout

A typical Maven project looks like this:

```text
project/
|-- pom.xml
|-- src/
|   |-- main/
|   |   |-- java/
|   |   `-- resources/
|   `-- test/
|       `-- java/
`-- target/
```

Important folders:

| Path | Purpose |
| --- | --- |
| `pom.xml` | Maven project configuration |
| `src/main/java` | Application source code |
| `src/main/resources` | Files bundled with the application |
| `src/test/java` | Test source code |
| `target` | Generated build output |

## Creating a Maven Project

An archetype is a project template.

`mvn archetype:generate` means:

- ask Maven to create a new project
- choose a template called an archetype
- fill in project identity values such as `groupId` and `artifactId`

Generate a starter project:

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-app \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

Generated structure:

```text
my-app/
|-- pom.xml
`-- src/
    |-- main/
    |   `-- java/
    |       `-- com/example/App.java
    `-- test/
        `-- java/
```

Rust comparison:

| Rust | Maven |
| --- | --- |
| `cargo new my-app` | `mvn archetype:generate ...` |
| Creates a standard Rust project | Creates a Java project from a selected archetype |
| Uses Cargo's default project shape | Uses the archetype's project shape |

For a very small Java project, you can also create the folders and `pom.xml`
manually. The archetype command is just a quick starter generator.

## Project Identity: groupId and artifactId

Maven identifies a library or application using coordinates:

```text
groupId:artifactId:version
```

Example:

```text
com.fasterxml.jackson.core:jackson-databind:2.18.0
```

Meaning:

| Part | Meaning | Rust Comparison |
| --- | --- | --- |
| `groupId` | Organization, company, or namespace | No exact Cargo equivalent |
| `artifactId` | Project/library name | Crate name |
| `version` | Release version | Crate version |

### Why `com.example`?

Java package names are often based on reversed domain names.

If a company owns:

```text
example.com
```

then its Java packages often start with:

```text
com.example
```

Why reverse it?

- Domain names are globally unique.
- Reversing them makes package names unlikely to clash.
- It creates a clean namespace for code and libraries.

Examples:

| Domain | Java Package Prefix |
| --- | --- |
| `example.com` | `com.example` |
| `fasterxml.com` | `com.fasterxml` |
| `apache.org` | `org.apache` |

For personal practice projects, `com.javaplayground` or `com.example` is fine.
For real published libraries, use a namespace you control.

## Adding Dependencies

Dependencies are added inside the `<dependencies>` section of `pom.xml`.

Example: Jackson Databind

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.0</version>
</dependency>
```

Parts of a dependency:

| Field | Meaning |
| --- | --- |
| `groupId` | Organization or namespace |
| `artifactId` | Library or project name |
| `version` | Exact version to download |

Maven downloads dependencies from Maven Central by default.

### How To Know What Dependency To Add

Maven Central is the closest Maven equivalent to `crates.io`.

Usual workflow:

1. Search for the library on Maven Central or the library's documentation site.
2. Copy the Maven dependency snippet.
3. Paste it into the `<dependencies>` section of `pom.xml`.

Useful places to search:

- Maven Central: `https://central.sonatype.com/`
- Maven Repository browser: `https://mvnrepository.com/`
- The library's official documentation

Example search:

```text
jackson databind maven
```

That gives coordinates like:

```text
com.fasterxml.jackson.core:jackson-databind:2.18.0
```

which become:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.0</version>
</dependency>
```

### Is There A Command Like cargo add?

The traditional Maven habit is to edit `pom.xml` directly.

Modern Maven can also add a dependency from the command line using the Maven
Dependency Plugin:

```bash
mvn dependency:add -Dgav=com.fasterxml.jackson.core:jackson-databind:2.18.0
```

Here `gav` means:

```text
groupId:artifactId:version
```

Rust comparison:

| Rust | Maven |
| --- | --- |
| `cargo add serde` | `mvn dependency:add -Dgav=groupId:artifactId:version` |
| Adds to `Cargo.toml` | Adds to `pom.xml` |
| Crate name is usually enough | Maven usually needs full coordinates |

In many Java tutorials and workplaces, manually editing `pom.xml` is still the
normal approach because it is explicit and easy to review.

## Common Commands

| Command | What It Does |
| --- | --- |
| `mvn compile` | Compiles source code into `.class` files |
| `mvn test` | Runs tests |
| `mvn package` | Compiles, tests, and creates a JAR |
| `mvn clean` | Deletes the `target` directory |
| `mvn clean package` | Rebuilds from a clean state |
| `mvn exec:java -Dexec.mainClass=...` | Runs a main class with Maven |

Example:

```bash
mvn exec:java -Dexec.mainClass=com.javaplayground.iojsoncli.S04SmallCli -Dexec.args="2 detail"
```

## Build Output

Maven writes generated files into `target`.

Common files inside `target`:

| Output | Meaning |
| --- | --- |
| `classes/` | Compiled `.class` files and copied resources |
| `test-classes/` | Compiled test classes |
| `*.jar` | Packaged application archive |
| `maven-status/` | Build metadata used by Maven plugins |

### Is Java Output A Binary Like Rust?

Rust commonly builds a native executable binary:

```text
target/debug/my-app
target/release/my-app
```

Java usually builds `.class` files first:

```text
target/classes/com/example/App.class
```

Then Maven can package those `.class` files into a JAR:

```text
target/my-app-1.0-SNAPSHOT.jar
```

A JAR is a Java archive. It is basically a ZIP file with compiled Java bytecode
and metadata.

Important difference:

| Rust | Java |
| --- | --- |
| Builds native machine code | Builds JVM bytecode |
| Output is often an executable file | Output is often a JAR |
| Runs directly on the OS | Runs on the JVM using `java` |

Run a JAR:

```bash
java -jar target/my-app-1.0-SNAPSHOT.jar
```

Is it a single file? Sometimes.

- A simple app can be packaged as one JAR.
- If dependencies are separate, you need the app JAR plus dependency JARs.
- A "fat JAR" or "uber JAR" bundles app code and dependencies into one JAR.
- Some tools can build native executables from Java, but normal Maven output is
  usually `.class` files and/or JARs.

## Why So Many Nested Folders?

Example:

```text
src/main/java/com/example/App.java
```

This path has two ideas combined:

1. Maven's standard source layout.
2. Java's package-to-folder convention.

Maven part:

```text
src/main/java
```

This means "production Java source code lives here."

Java package part:

```text
com/example/App.java
```

This matches the package declaration inside the file:

```java
package com.example;
```

So the full file path becomes:

```text
src/main/java/com/example/App.java
```

Why Java does this:

- Package names prevent class-name conflicts.
- Folder structure keeps package names visible in the project.
- Build tools and IDEs can find classes predictably.
- Large applications can organize code by domain or feature.

Rust comparison:

| Rust | Java |
| --- | --- |
| `src/main.rs` | `src/main/java/.../App.java` |
| Modules often use files and `mod` | Packages usually match folders |
| Crate name gives the top-level namespace | Package name gives the namespace |

The nesting looks heavy at first, but it pays off when a project has many files,
packages, tests, and resources.

## Local Dependency Cache

Downloaded libraries are cached locally so Maven does not need to download them
again for every project.

Linux and macOS:

```text
~/.m2/repository/
```

Windows:

```text
C:\Users\<username>\.m2\repository\
```

## Maven Compared With Rust Cargo

| Rust | Java / Maven |
| --- | --- |
| `cargo new` | `mvn archetype:generate` |
| `Cargo.toml` | `pom.xml` |
| `crates.io` | Maven Central |
| `cargo build` | `mvn package` |
| Native binary | `.class` files or JAR |
| Crate | Package / library |
| `cargo add` | Edit `pom.xml` or use `mvn dependency:add` |

## Key Takeaways

- Maven is not required to write Java, but it makes dependency management easier.
- Third-party libraries like Jackson and Picocli are added through `pom.xml`.
- `groupId:artifactId:version` is Maven's dependency identity format.
- `mvn archetype:generate` creates a project from a template.
- Maven builds into `target`.
- Java usually produces `.class` files and JARs, not native binaries.
- Maven caches downloaded dependencies in `~/.m2/repository`.

## References

- Maven Central: `https://central.sonatype.com/`
- Maven Dependency Plugin: `https://maven.apache.org/plugins/maven-dependency-plugin/`
