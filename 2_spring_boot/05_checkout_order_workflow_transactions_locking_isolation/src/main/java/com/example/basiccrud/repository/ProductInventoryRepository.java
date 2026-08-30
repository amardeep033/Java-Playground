package com.example.basiccrud.repository;

import com.example.basiccrud.model.ProductInventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

    Optional<ProductInventory> findBySku(String sku);

    // Note: in real code this is @Modifying(clearAutomatically = true, flushAutomatically = true).
    // A bulk update writes straight to the DB and skips the persistence context, so a ProductInventory already loaded
    // in this transaction keeps the old availableQuantity until the context is cleared.
    @Modifying
    @Query("""
            update ProductInventory p
            set p.availableQuantity = p.availableQuantity - :quantity
            where p.sku = :sku
              and p.availableQuantity >= :quantity
            """)
    int reserveStock(@Param("sku") String sku, @Param("quantity") int quantity);
}

// Why not do this?
// 1. SELECT available_quantity
// 2. if enough, UPDATE available_quantity
// Two concurrent checkouts can both read the same old stock before either update.
// Atomic update makes the database perform check + write as one statement.

// | Strategy | Idea | Where it fits |
// | -------- | ---- | ------------- |
// | Atomic update | One SQL statement checks stock and reduces it. | When the whole rule fits in a WHERE clause, like here. Java never sees the old value, so there is no window to race in. |
// | Optimistic locking | Version column detects a lost update and throws on conflict. | Read-modify-write where conflicts are rare. Nothing is locked; the loser only finds out at commit, so the caller must be able to retry. |
// | Pessimistic locking | `select ... for update` locks the row until the transaction ends. | Read-modify-write where the decision needs Java logic or several rows. Conflicting callers wait instead of failing. |

// Atomic vs pessimistic: both take the same exclusive row lock. The difference is how long it is held and who makes the decision.
// - Atomic update: the DB holds the lock for one statement and checks the condition itself. No deadlock window, but the rule has to be expressible in SQL.
// - Pessimistic: the lock is held from the SELECT until commit, so Java can read, decide, then write back safely. More flexible, but the row stays locked for the rest of the transaction, which is where contention and deadlocks come from.

// Concurrency option used in code: atomic stock update.
// Affected rows = 1 means stock was available and got reserved.
// Affected rows = 0 means product is missing or stock is insufficient.
