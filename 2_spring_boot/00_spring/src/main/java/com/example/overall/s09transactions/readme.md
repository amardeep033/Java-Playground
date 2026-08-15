# s9transactions

## How To Run

From `00_spring/`:

```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.overall.s9transactions.Main
```

## Cheatsheet

| Topic | Default behavior | How to change / remember |
| --- | --- | --- |
| Transaction entry | External call enters Spring proxy first. | `Controller -> Service proxy -> target method`. |
| Transaction manager | `@Transactional` delegates to `PlatformTransactionManager`. | JDBC commonly uses `DataSourceTransactionManager`; JPA commonly uses `JpaTransactionManager`. |
| `REQUIRED` | Join existing transaction; otherwise start one. | This is the default propagation. |
| `REQUIRES_NEW` | Suspend existing transaction and start a new one. | Must be reached through a Spring proxy. |
| Normal return | Commit. | The method completed successfully. |
| Checked exception | Commit by default. | Use `@Transactional(rollbackFor = Exception.class)` to roll back. |
| Unchecked exception | Rollback by default. | `RuntimeException` and `Error` trigger rollback. |
| Self-invocation | Inner method annotation is not intercepted. | `this.inner()` bypasses proxy. |
| Non-DB side effect | Not automatically rolled back. | Email/file/API side effects need compensation or after-commit handling. |

## Theory Not In Comments

| Theory | Meaning | Example |
| --- | --- | --- |
| Transaction isolation | Controls what one transaction can see while other transactions are running. | Prevents unsafe concurrent reads/writes. |
| Dirty read | A transaction reads data another transaction has not committed. | T2 reads balance `500`; T1 later rolls back to `1000`. |
| Non-repeatable read | Same row read twice in one transaction gives different values. | T1 reads `1000`; T2 commits `500`; T1 reads `500`. |
| Phantom read | Same query returns a different set of rows. | T1 sees 5 orders; T2 inserts one; T1 sees 6. |
| `READ_UNCOMMITTED` | Weakest common isolation. | Dirty reads may be possible depending on DB. |
| `READ_COMMITTED` | Reads only committed data. | Prevents dirty reads. |
| `REPEATABLE_READ` | Re-reading same row stays stable. | Helps prevent non-repeatable reads. |
| `SERIALIZABLE` | Strongest common isolation. | Highest consistency, lower concurrency. |

| Chain | Responsibility |
| --- | --- |
| `@Transactional` | Metadata on the method. |
| Spring proxy | Intercepts external method call. |
| `TransactionInterceptor` | Applies transaction rules. |
| `TransactionManager` | Begins, commits, or rolls back. |
| `DataSource` / connection | Talks to the database. |
| Repository SQL | Performs the actual DB work. |

## Try These Scenarios

| Scenario | Code change | Expected DB rows |
| --- | --- | --- |
| Success | Keep both throw lines in `OrderService` commented. | `orders=[Order1]`, `audits=[Audit for Order1]` |
| Checked exception | Uncomment `throw new Exception("checked exception");` | `orders=[Order1]`, `audits=[Audit for Order1]` |
| Unchecked exception | Uncomment `throw new RuntimeException("unchecked exception");` | `orders=[]`, `audits=[Audit for Order1]` |
| Self-invocation | In `OrderController`, comment `orderService.placeOrder()` and uncomment `orderService2.placeOrder()` | `orders=[]`, `audits=[]` because inner `REQUIRES_NEW` was bypassed. |

## Interview Q&A

| Question | Strong answer |
| --- | --- |
| Why put `@Transactional` on service methods? | A service method usually represents one use case and can wrap multiple repository calls in one unit of work. |
| Why does checked exception commit by default? | Spring's default rollback rules roll back unchecked exceptions, not checked exceptions. Checked exceptions often model expected/recoverable outcomes. |
| How do you roll back on checked exceptions? | Use `@Transactional(rollbackFor = Exception.class)` or a more specific checked exception class. |
| Why does audit remain when the outer order rolls back? | `AuditService.saveAudit()` uses `REQUIRES_NEW` and is called through another Spring bean proxy, so it commits in its own transaction. |
| Why does self-invoked audit roll back in `OrderService2`? | `this.saveAudit()` bypasses the proxy, so `REQUIRES_NEW` is not applied. The audit insert participates in the outer transaction and rolls back with it. |
| Does self-invocation mean there is no transaction? | Not always. It means the inner method's transaction annotation is not intercepted. If the outer method has a transaction, inner DB work may still run inside that existing transaction. |
| Is `@Transactional` useful for file operations or emails? | Not by itself. Spring can roll back transactional resources managed by a transaction manager; arbitrary side effects need compensation or after-commit events. |
| What does isolation solve? | It controls visibility between concurrent transactions and prevents issues such as dirty reads, non-repeatable reads, and phantom reads. |
| What is the tradeoff of higher isolation? | Stronger consistency, but potentially lower concurrency and more locking/contention. |
| How does Spring Boot choose a transaction manager? | It auto-configures one based on persistence technology, such as `DataSourceTransactionManager` for JDBC or `JpaTransactionManager` for JPA. |
