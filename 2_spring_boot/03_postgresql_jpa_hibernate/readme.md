## 1. Cheatsheet

### 1.1 What 03 Adds Over 02

| Area | Practical Takeaway |
| --- | --- |
| Storage | Products now live in PostgreSQL instead of memory. |
| Entity | `Product` is mapped to table `public.products`. |
| Repository | `ProductRepository` is an interface; Spring Data JPA creates the implementation. |
| Queries | This module covers derived queries, JPQL, native SQL, named parameters, and DTO projections. |
| Persistence | Loaded entities can become managed, so Hibernate can track changes. |
| Transactions | Service methods use `@Transactional` to define database work boundaries. |

### 1.2 Mental Model

| Thing | Meaning |
| --- | --- |
| PostgreSQL | Real database: tables, rows, constraints, indexes. |
| pgAdmin | GUI for checking PostgreSQL. It is not used by Spring Boot at runtime. |
| JDBC | Java database connectivity layer used under the hood. |
| DataSource | Spring Boot's configured database connection/pool. |
| JPA | Specification for mapping Java objects to relational tables. |
| Hibernate | JPA implementation that generates SQL and tracks managed entities. |
| Spring Data JPA | Creates repository implementations from interfaces. |
| Repository | Java abstraction used by service code to access data. It is not the database. |

### 1.3 Repository Hierarchy

```text
Repository
    |
    v
CrudRepository
    |
    v
PagingAndSortingRepository
    |
    v
JpaRepository
```

| Interface | Enough To Know |
| --- | --- |
| `Repository<T, ID>` | Marker/root interface. No useful CRUD methods by itself. |
| `CrudRepository<T, ID>` | Basic CRUD: `save`, `findById`, `findAll`, `deleteById`, `count`, `existsById`. |
| `PagingAndSortingRepository<T, ID>` | Adds pagination and sorting support. |
| `JpaRepository<T, ID>` | Common JPA default; includes CRUD, paging/sorting, plus JPA helpers like `flush`. |
| Why this project uses `JpaRepository` | It is the common production choice for JPA apps and prepares later modules for pagination and transaction behavior. |

### 1.4 Entity Mapping

| Code | Meaning |
| --- | --- |
| `@Entity` | Class can be persisted by JPA/Hibernate. |
| `@Table(name = "products")` | Maps entity to table `products`. |
| `@Id` | Primary key field. |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | PostgreSQL generates the id during `INSERT`. |
| `@Column(nullable = false)` | Database column should be `NOT NULL`; not the same as request validation. |
| Empty constructor | Required by Hibernate when creating entity objects from rows. |
| DTOs still exist | Entity follows database shape; DTOs protect API shape. |

### 1.5 Repository Methods And SQL Shape

| Repository Call | Practical Meaning | SQL Shape |
| --- | --- | --- |
| `findAll()` | Load every row. Avoid for large production tables. | `select ... from products` |
| `findById(id)` | Load by primary key. | `where id = ?` |
| `save(product)` with `id = null` | Insert new row. | `insert into products ...` |
| `save(product)` with existing id | Update/merge existing row. | Often `select`, then `update` |
| `existsById(id)` | Check if row exists. | `select count...` or exists-style query |
| `deleteById(id)` | Delete row by primary key. | `delete from products where id = ?` |
| `count()` | Count rows. Used by seeder. | `select count(*) from products` |

### 1.6 Query Styles Covered Here

| Style | Example In Code | When To Use |
| --- | --- | --- |
| Derived query | `findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(...)` | Simple readable filters. |
| JPQL | `@Query("select p from Product p where lower(p.name) = lower(:name)")` | Entity/field based queries that should stay database-independent. |
| `@Param` | `@Param("name") String name` | Named query parameters; clearer than positional parameters. |
| Native SQL | `@Query(value = "...", nativeQuery = true)` | PostgreSQL-specific SQL or query tuning cases. |
| DTO projection | `select new ...ProductSummary(p.name, p.price)` | Return only selected fields instead of full entities. |

### 1.7 Persistence Context And Dirty Checking

| Concept | Enough To Know |
| --- | --- |
| Persistence context | Hibernate's tracking workspace for managed entities inside a transaction. |
| Transient entity | `new Product(...)`; Java object only, not tracked by Hibernate. |
| Managed entity | Loaded/saved inside active context; Hibernate tracks changes. |
| Detached entity | Was managed earlier, but context closed; no automatic tracking now. |
| Dirty checking | If a managed entity changes inside `@Transactional`, Hibernate can issue `UPDATE` on flush/commit. |
| Flush | Synchronizes pending entity changes to SQL. Can happen before commit or before some queries. |
| Commit | Completes the transaction. Pending changes are flushed before commit. |

### 1.8 Transaction Basics

| Code | Meaning |
| --- | --- |
| `@Transactional(readOnly = true)` | Use for read methods. Communicates intent and can allow provider optimizations. |
| `@Transactional` | Use for write methods or use cases that need one database boundary. |
| Runtime exception | By default, Spring rolls back on unchecked exceptions. |
| Checked exception | Does not roll back by default unless configured. |
| Service layer | Best place for transaction boundaries in normal Spring apps. |

### 1.9 Database Connection Options

| Way | Use When | SDE2-Level Takeaway |
| --- | --- | --- |
| Local PostgreSQL + pgAdmin | Learning and manual inspection. | Spring Boot connects to PostgreSQL directly; pgAdmin only helps you view data. |
| Docker Compose | Team-local development. | Reproducible DB version, port, database, username, and password. |
| Testcontainers | Integration tests. | Best practical way to test repositories against real PostgreSQL. |
| Managed PostgreSQL | Staging/production. | Use secrets, SSL/network rules, backups, monitoring, and migrations. |

### 1.10 Current Local Config

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:postgresql://localhost:5432/postgres` |
| Username | `postgres` |
| Password | `postgres` |
| Table in pgAdmin | `public.products` |
| Check data | `SELECT * FROM public.products;` |
| Schema setting | `spring.jpa.hibernate.ddl-auto=update` |
| SQL logs | `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true` |

### 1.11 Generated SQL To Notice

| Action | What You Should See In Logs |
| --- | --- |
| Create product | `insert into products ...` |
| Get by id | `select ... from products where id=?` |
| Search by derived query | SQL with `lower(name)` and `price>=?`. |
| Update product | `select` first, then `update` on transaction flush/commit. |
| Delete product | Existence check, then `delete from products where id=?`. |
| DTO projection | Query selects only projected columns, not the whole entity. |

### 1.12 Production Defaults To Ask AI For

| Area | Ask AI To Do This |
| --- | --- |
| Schema changes | Use Flyway or Liquibase migrations, not `ddl-auto=update`. |
| Money values | Prefer `BigDecimal` over `double` for real prices/money. |
| Secrets | Read DB URL/user/password from environment variables or secret manager. |
| API boundary | Keep request/response DTOs separate from entities. |
| Errors | Keep global exception handling and stable error response format. |
| Transactions | Put `@Transactional` on service methods, not randomly on controllers/repositories. |
| Seed data | Keep demo seeders out of production or guard them by profile. |
| Pagination | Do not return unbounded `findAll()` for large tables. |
| Indexes | Add indexes for columns frequently used in filters/sorts, then verify with query plans. |
| Tests | Use Testcontainers for repository/integration tests that need real PostgreSQL behavior. |

### 1.13 Keep For Later Modules

| Topic | Module |
| --- | --- |
| Relationships, joins, fetch joins, N+1 | 04 |
| Deep pagination and `EXPLAIN ANALYZE` practice | 04 |
| Checkout/order transaction workflow | 05 |
| Idempotency and consistency across steps | 05 |
| Optimistic/pessimistic locking | 06 |
| Isolation levels, deadlocks, race conditions | 06 |

## 2. Interview QA

### 2.1 Core JPA Questions

| Question | Answer |
| --- | --- |
| Is Repository the database? | No. Repository is a Java abstraction. PostgreSQL is the database. |
| Where is the repository implementation class? | Spring Data creates it at runtime as a proxy from the interface. |
| Why use `JpaRepository` instead of `CrudRepository`? | `CrudRepository` is enough for basic CRUD, but `JpaRepository` is the common JPA default and adds paging/sorting/JPA helpers. |
| What does `@Entity` mean? | The class is eligible for JPA persistence. It is not automatically saved just because you created an object. |
| Is `@Column(nullable = false)` the same as `@NotBlank`? | No. `@NotBlank` validates API input; `@Column(nullable = false)` maps database nullability. |
| Why keep DTOs when entity fields look the same? | DTOs protect API contracts. Entities follow persistence needs. They diverge quickly in real systems. |

### 2.2 Query Questions

| Question | Answer |
| --- | --- |
| When is a derived query good? | When the method name is short and obvious. |
| When should derived query names be avoided? | When names become long, filters are dynamic, joins are needed, or performance matters. |
| JPQL vs native SQL? | JPQL uses entity/field names; native SQL uses real table/column names and database-specific features. |
| Why use `@Param`? | Named parameters make queries easier to read and safer to reorder than positional parameters. |
| Why use DTO projection? | To fetch and return only fields needed for a use case. |
| Does `product.getName()` run SQL? | Not in this simple entity. It reads a field already loaded into the object. |

### 2.3 Transaction And Persistence Questions

| Question | Answer |
| --- | --- |
| What is a persistence context? | Hibernate's tracking workspace for managed entities. |
| What is a managed entity? | An entity loaded/saved inside an active persistence context and tracked by Hibernate. |
| What is dirty checking? | Hibernate detects changes to managed entities and writes updates during flush/commit. |
| Do you always need `save()` after setters? | No. If the entity is managed inside `@Transactional`, dirty checking can update it. |
| Then why does `save()` exist? | New or detached entities are not automatically managed; `save()` persists/merges them. |
| When does SQL execute? | Often on flush/commit, not necessarily on the exact Java line that changed the object. |
| What rolls back by default? | Runtime exceptions. Checked exceptions need explicit rollback configuration. |

### 2.4 Production Scenario Questions

| Question | Answer |
| --- | --- |
| Why is `ddl-auto=update` risky in production? | Hibernate changes schema automatically. Production schema should be reviewed and versioned with migrations. |
| Why is `double` not ideal for price? | Floating point can introduce precision issues. Use `BigDecimal` for money. |
| Why avoid unbounded `findAll()`? | Large tables can cause slow responses and high memory usage. Use pagination. |
| What should you inspect when a JPA endpoint is slow? | Generated SQL, indexes, query plan, pagination, and later N+1 behavior. |
| What is Testcontainers useful for? | Testing repository/database behavior against real PostgreSQL instead of mocks or H2 differences. |
| Should seeders run in production? | Usually no. Use profile guards or migrations/reference-data scripts. |
