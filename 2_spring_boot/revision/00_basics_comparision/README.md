# Basics: Spring component scanning and DI

A plain Spring console example. All Java classes use the package
`com.example.overall.s2autoconfig`, matching their source directory.

`AppConfig` enables component scanning. Spring injects `FileLogger` into
`OrderService`. `Main` starts the context, retrieves the service, and places an order.

Run from this folder (the existing Maven setup includes the Boot run plugin):

```bash
mvn spring-boot:run
```

Expected output:

```text
With Spring Component: order placed
File log: Order created
```
