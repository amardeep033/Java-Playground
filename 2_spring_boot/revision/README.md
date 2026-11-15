# Spring revision

Two independent Maven projects comparing plain Spring with Spring Boot.
Runnable code uses Boot; the corresponding plain Spring setup is commented.

```text
revision/
├── 00_basics_comparision/  # Console app: component scanning and DI
│   ├── pom.xml
│   └── src/main/java/com/example/overall/s2autoconfig/
│       ├── Main.java
│       ├── AppConfig.java       # Plain Spring, commented
│       ├── LoggerService.java
│       ├── FileLogger.java
│       └── OrderService.java
└── 01_server_comparision/  # Spring MVC: GET /students
    ├── pom.xml
    └── src/main/java/com/example/comparison/
        ├── ComparisonApplication.java
        ├── WebConfig.java       # Plain Spring, commented
        ├── WebInitializer.java  # Plain Spring, commented
        ├── controller/
        ├── model/
        ├── repository/
        └── service/
```

Run `mvn spring-boot:run` inside either project folder.
See each folder's README for the comparison and expected output.
