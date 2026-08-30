package com.example.basiccrud.repository;

import com.example.basiccrud.model.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    // 0. N+1 demo: fetch orders. customer is EAGER by default, but items are LAZY.
    // When service loops over orders and touches order.getItems(), Hibernate can issue one item query per order.
    List<CustomerOrder> findByStatusOrderById(String status);

    // 1. JOIN FETCH fix: fetches orders and their lazy items in the same JPQL query.
    // distinct avoids duplicate CustomerOrder results caused by joining a to-many collection.
    // Example: one order with 2 items becomes 2 SQL rows after the join:
    // Order A | Item 1
    // Order A | Item 2

    // Without distinct, the result list can contain Order A twice.
    // With distinct, the returned parent list is [Order A], while Order A.items still contains [Item 1, Item 2].
    // Hibernate still uses both SQL rows to assemble the full object graph.
    // Plain JPQL joins do not initialize lazy relationships. The "fetch" keyword changes the fetch plan.
    @Query("""
            select distinct o
            from CustomerOrder o
            left join fetch o.items
            where o.status = :status
            order by o.id
            """)
    List<CustomerOrder> findByStatusUsingJoinFetch(@Param("status") String status);


    // 2. EntityGraph fix: keeps a derived query method but changes the fetch plan for this query.
    // Use it when the filter/sort is simple and only the relationship loading needs customization.
    // Here we fetch only items because customer is already EAGER by default in this example.
    // EntityGraph does not guarantee "exactly one SQL query"; it tells Hibernate what should be fetched.
    @EntityGraph(attributePaths = {"items"})
    List<CustomerOrder> findWithItemsByStatusOrderById(String status);
}

// ---------------------------------------- 1. PAGINATION ----------------------------------------------------
// If you ask for 20 orders, a join fetch on items first creates SQL rows like:
// Order A | Item 1
// Order A | Item 2
// Order B | Item 3
// The database LIMIT applies to rows, not reconstructed CustomerOrder objects.
// Hibernate then has to rebuild the object graph from duplicated parent rows.

// Practical fixes:
// 1. Page only order ids first, then fetch the selected orders with their items.
// 2. Keep items LAZY and use @BatchSize to reduce 1 + N into roughly 1 + (N / batch_size).
// 3. Use DTO projections when the screen needs summaries instead of full entities.

// | Choice | What It Controls | Main Tradeoff |
// | ------ | ---------------- | ------------- |
// | Set<OrderItem> | Java/Hibernate collection uniqueness. | No guaranteed order; entity equals/hashCode must be designed carefully. |
// | List<OrderItem> | Java/Hibernate collection order and duplicates. | Duplicates are allowed; use @OrderColumn for persisted list position. |
// | JPQL DISTINCT | Duplicate root results, like duplicate CustomerOrder rows. | Does not remove duplicate child items or fix row explosion. |


// ---------------------------------------- 2. CARTESIAN EXPLOSION ----------------------------------------------------
// MultipleBagFetchException: Hibernate cannot safely/efficiently reconstruct certain multiple bag fetches from a single joined result.
// Order | Item | Payment
// ------+------|--------
// 1     | A    | X
// 1     | A    | Y
// 1     | B    | X
// 1     | B    | Y
// 1     | C    | X
// 1     | C    | Y

// Strategy 1 — Fetch one collection at a time
// Strategy 2 — Batch fetching
// Strategy 3 — DTO projections -- Give me exactly the data required by this use case
// Strategy 4 — Two-step fetching

// ---------------------------------------- 3. PROJECTIONS ----------------------------------------------------
// Projections:
// Use projections when the API/screen needs only selected columns, not full managed entities.
// Common places: list screens, search results, dashboards, reports.

// | Option | Use When | Notes |
// | ------ | -------- | ----- |
// | Entity + JOIN FETCH | You need domain objects and related data for business logic. | Managed by Hibernate; can update state. |
// | Interface projection | You need a small read-only shape and method names match selected fields. | Spring Data can generate it from derived queries. |
// | DTO/record projection | You want an explicit API/read model. | Good for list/search/report queries. |
// | Dynamic projection | Same query should return different read shapes. | Example: findByStatus(String status, Class<T> type). |

// DTO projection example:

// public record OrderSummary(Long id, String orderNumber, String customerName) {}

// @Query("""
//     select new com.example.basiccrud.dto.OrderSummary(
//         o.id,
//         o.orderNumber,
//         o.customer.name
//     )
//     from CustomerOrder o
//     where o.status = :status
// """)
// List<OrderSummary> findOrderSummaries(String status);

// Mental model:
// Database row data
//      ├── Entity: managed object for domain/business logic
//      └── Projection: read-only shape for API/report/list screens


// ---------------------------------------- 4. QUERY OPTIMIZATION ----------------------------------------------------
// Query optimization mental model:
// EXPLAIN shows the database plan without running the query.
// EXPLAIN ANALYZE runs the query and shows the real execution statistics.

// Example:
// EXPLAIN ANALYZE
// SELECT *
// FROM orders_04_order
// WHERE customer_id = 123;

// What to check:
// | Signal | What It Means |
// | ------ | ------------- |
// | Access method | Index Scan is usually good for selective lookups; Seq Scan means full table scan. |
// | Estimated rows vs actual rows | Should be close. Big mismatch means planner statistics/selectivity assumptions may be wrong. |
// | Join strategy | Nested Loop, Hash Join, or Merge Join. The best choice depends on data size, indexes, selectivity, and query shape. |
// | Execution time | Real time spent after the query actually runs. Use this to compare before/after index/query changes. |

// Request flow:
// Java service/repository
//        ↓
// Hibernate generates SQL
//        ↓
// PostgreSQL query planner chooses plan
//        ↓
// PostgreSQL uses indexes or scans tables
//        ↓
// Data is read from memory or disk
