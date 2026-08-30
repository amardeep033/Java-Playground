## 1. Curl List

Start the app:

```bash
./run.sh
```

| Goal | Command | What To Watch In Logs |
| --- | --- | --- |
| Default safe endpoint | `curl http://localhost:8080/api/orders` | Uses the default implementation from the controller. |
| Show N+1 problem | `curl http://localhost:8080/api/orders/n-plus-one` | Orders load first; `order.getItems()` inside a loop can trigger extra item queries. |
| Fix using JPQL `JOIN FETCH` | `curl http://localhost:8080/api/orders/join-fetch` | Items are fetched in the main JPQL query. |
| Fix using `@EntityGraph` | `curl http://localhost:8080/api/orders/entity-graph` | Derived query method, but fetch plan includes `items`. |
| Fix using `@BatchSize` | `curl http://localhost:8080/api/orders/batch` | Lazy item loading is grouped using `where order_id in (...)`. |

## 2. Cheatsheet

### 2.1 Domain And Table Model

| Topic | Covered In Code Comment | Quick Pointer |
| --- | --- | --- |
| Domain model | `BasicCrudRestApiApplication` | `Customer <- CustomerOrder <-> OrderItem`. |
| Table relationship | `BasicCrudRestApiApplication` | `orders_04_order.customer_id` and `order_items_04_order.order_id` are the FK columns. |
| Owning side | `BasicCrudRestApiApplication`, `CustomerOrder`, `OrderItem` | The side with the FK column owns the relationship. |
| Why `Customer` has no `List<CustomerOrder>` | `BasicCrudRestApiApplication` | Java navigation is intentionally one-way from order to customer. |
| Why `OrderItem` is a separate entity | `BasicCrudRestApiApplication`, `CustomerOrder` | It has business data like quantity and unit price. |

### 2.2 Relationship Mapping

| Topic | Covered In Code Comment | Quick Pointer |
| --- | --- | --- |
| JPA fetch defaults | `CustomerOrder` | `@ManyToOne` / `@OneToOne` default EAGER; collections default LAZY. |
| `@ManyToOne` customer | `CustomerOrder` | Kept EAGER here intentionally for comparison. |
| `@OneToMany` items | `CustomerOrder` | Kept LAZY so N+1 can be demonstrated clearly. |
| `mappedBy` | `CustomerOrder` | `OrderItem.order` owns the FK; `CustomerOrder.items` is inverse side. |
| `cascade = ALL` | `CustomerOrder` | Saving/deleting order cascades to its items. |
| `orphanRemoval = true` | `CustomerOrder` | Removing item from order list deletes that row. |

| Relationship | Default Fetch Type | Example | Practical Note |
| --- | --- | --- | --- |
| `@ManyToOne` | EAGER | Order -> Customer | Often changed to LAZY in production to avoid accidental overfetching. |
| `@OneToOne` | EAGER | User -> AuthProfile | Add a UNIQUE constraint on the FK if the DB must enforce true one-to-one. |
| `@OneToMany` | LAZY | Customer -> Orders | Keep LAZY by default; fetch children only when the use case needs them. |
| `@ManyToMany` | LAZY | Student -> Course | Use a join table; if the relationship has fields, model it as an entity. |

| Setting | Meaning |
| --- | --- |
| `CascadeType.PERSIST` | Persist child items when the parent order is persisted. |
| `CascadeType.MERGE` | Merge child items when the parent order is merged. |
| `CascadeType.REMOVE` | Delete child items when the parent order is deleted. |
| `CascadeType.ALL` | Apply all cascade operations above. |
| `orphanRemoval = true` | Delete a child item when it is removed from `order.items`. |

### 2.3 N+1 And Fetch Fixes

| Topic | Covered In Code Comment | Quick Pointer |
| --- | --- | --- |
| N+1 demo | `OrderQueryService`, `OrderRepository` | Looping over orders and touching lazy items can issue extra queries. |
| `JOIN FETCH` | `OrderRepository` | Fetches lazy items in the JPQL query. |
| `distinct` with fetch join | `OrderRepository` | Removes duplicate root order entries caused by joining item rows. |
| Plain JPQL join vs fetch join | `OrderRepository` | Normal join filters/sorts; `join fetch` initializes the association. |
| `@EntityGraph` | `OrderRepository`, `OrderQueryService` | Changes fetch plan without writing custom JPQL. |
| `@BatchSize` | `CustomerOrder`, `OrderQueryService` | Reduces lazy collection N+1 into fewer `IN (...)` queries. |

### 2.4 Query Shape And Read Models

| Topic | Covered In Code Comment | Quick Pointer |
| --- | --- | --- |
| Pagination with collection fetch join | `OrderRepository` | SQL rows duplicate parent orders, so row limit is not the same as parent limit. |
| `Set` vs `List` vs `DISTINCT` | `OrderRepository` | Collection type affects Java collection; `DISTINCT` affects root query result. |
| Multiple collection fetch problem | `OrderRepository` | Multiple to-many joins can create row explosion. |
| Projection vs entity | `OrderRepository` | Use projections for read-only list/search/report screens. |
| LazyInitializationException | `OrderQueryService` | Lazy access needs an active persistence context. |

| Choice | What It Controls | Main Tradeoff |
| --- | --- | --- |
| `Set<OrderItem>` | Java/Hibernate collection uniqueness. | No guaranteed order; entity `equals`/`hashCode` must be designed carefully. |
| `List<OrderItem>` | Java/Hibernate collection order and duplicates. | Duplicates are allowed; use `@OrderColumn` for persisted list position. |
| JPQL `DISTINCT` | Duplicate root results, like duplicate `CustomerOrder` rows. | Does not remove duplicate child items or fix row explosion. |

| Option | Use When | Notes |
| --- | --- | --- |
| Entity + `JOIN FETCH` | You need domain objects and related data for business logic. | Managed by Hibernate; can update state. |
| Interface projection | You need a small read-only shape and method names match selected fields. | Spring Data can generate it from derived queries. |
| DTO/record projection | You want an explicit API/read model. | Good for list/search/report queries. |
| Dynamic projection | Same query should return different read shapes. | Example: `findByStatus(String status, Class<T> type)`. |

| EXPLAIN ANALYZE Signal | What It Means |
| --- | --- |
| Access method | Index Scan is usually good for selective lookups; Seq Scan means full table scan. |
| Estimated rows vs actual rows | Should be close. Big mismatch means planner statistics/selectivity assumptions may be wrong. |
| Join strategy | Nested Loop, Hash Join, or Merge Join. Best choice depends on data size, indexes, selectivity, and query shape. |
| Execution time | Real time spent after the query runs. Use this to compare before/after index/query changes. |

## 3. Interview QA

| Scenario | Strong SDE2 Answer |
| --- | --- |
| Your order list endpoint suddenly becomes slow after adding `items` to the response. What do you check first? | Enable SQL logs and compare query count before/after. If one order query is followed by repeated item queries, it is N+1 from lazy collection access. |
| Why not change `CustomerOrder.items` from LAZY to EAGER to fix N+1? | EAGER can overfetch for every endpoint and still create bad SQL/query patterns. Choose fetch plan per use case using `JOIN FETCH`, `@EntityGraph`, batch loading, or projections. |
| Your endpoint needs full order details for a small result set. Which fix is reasonable? | `JOIN FETCH` or `@EntityGraph` is fine because the use case needs the related data and result size is controlled. |
| Your endpoint needs page 1 of 20 orders with item summaries. Should you use collection `JOIN FETCH` with pagination? | Usually no. Page order ids/parents first, then batch load items or use DTO projections. Collection fetch joins duplicate parent rows before pagination. |
| You see `select distinct o ... left join fetch o.items`. Does `distinct` remove duplicate items? | No. It removes duplicate root `CustomerOrder` entries from the returned list. The order still keeps all joined items. |
| Why does one order with two items produce two SQL rows in a fetch join? | SQL is row-based. Joining one parent to two child rows returns `Order A | Item 1` and `Order A | Item 2`; Hibernate reconstructs one order object with two items. |
| When would you prefer `@EntityGraph` over `JOIN FETCH`? | When the query filter/sort is simple and only the fetch plan needs customization. It avoids handwritten JPQL but does not guarantee exactly one SQL query. |
| When would you prefer `@BatchSize`? | When you want to keep the relationship lazy but reduce repeated lazy collection queries into fewer `IN (...)` queries. It is useful with parent pagination. |
| Why can lazy access throw `LazyInitializationException`? | The entity was returned after the transaction/persistence context closed, so Hibernate cannot initialize the lazy association anymore. |
| Should you enable `spring.jpa.open-in-view=true` to avoid LazyInitializationException? | Avoid using it as the main fix. It can hide accidental DB access during response rendering. Fetch required data inside service transactions instead. |
| When should you use projection instead of entity + fetch joins? | For list screens, search results, reports, or API responses that need only selected fields and do not need managed entities. |
| Why is `OrderItem` not modeled as pure `@ManyToMany`? | The relationship has its own data: quantity, unit price, product name. That makes it a real entity, not just a join row. |
| What is the main production habit to build from this module? | Always ask: what data does this endpoint need, where is it accessed, and what SQL will Hibernate generate? |
