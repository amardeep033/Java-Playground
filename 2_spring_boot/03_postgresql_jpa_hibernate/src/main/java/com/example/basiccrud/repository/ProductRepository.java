package com.example.basiccrud.repository;

import com.example.basiccrud.dto.ProductSummary;
import com.example.basiccrud.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// In 03 this interface extends JpaRepository, and Spring Data JPA creates the implementation automatically.
// JpaRepository<Product, Long> means: manage Product entities whose primary key type is Long.
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 0. If an entity has a composite primary key, the second generic type is not Long.
    //    You create a separate key class and use JpaRepository<Entity, KeyClass>.
    //    JPA supports this with @EmbeddedId or @IdClass; this project uses one simple Long id.
    // 1. Basic CRUD methods already come from JpaRepository: findAll, findById, save, existsById, deleteById, count.
    // 2. Small custom queries can often be derived from method names, like the search method below.
    // 3. For advanced queries, projections, or performance-sensitive SQL, write the query manually with @Query.
    // 3.1 JPQL query
    // 3.2 Native SQL query
    // 3.3 DTO projection

    // 2. Derived query:
    // Spring Data reads the method name and generates the query automatically.
    // Here it means: name contains the given text, ignoring case, and price >= minPrice.
    // Good for simple filters. If the method name becomes unreadable, switch to @Query.
    List<Product> findByNameContainingIgnoreCaseAndPriceGreaterThanEqual(String name, double minPrice);

    // 3.1 JPQL query:
    // - Uses Java entity name Product and Java field names like p.name.
    // - Does not use table name products or column names directly.
    // - Hibernate translates this JPQL into SQL for PostgreSQL.
    // - @Param("name") binds the method parameter to the :name placeholder in the query.
    @Query("select p from Product p where lower(p.name) = lower(:name)")
    Optional<Product> findByNameUsingJpql(@Param("name") String name);

    // 3.2 Native SQL query:
    // - Uses real database table/column names, so products means the PostgreSQL table.
    // - Useful for database-specific SQL or tuning cases.
    // - Less portable than JPQL because this query is now tied to the database schema.
    @Query(value = "select * from products where price >= :minPrice order by price desc", nativeQuery = true)
    List<Product> findExpensiveProductsNative(@Param("minPrice") double minPrice);

    // 3.3 DTO projection: (same as 3.1 - only diff is full entity vs specific detail, same can't be applied in 3.2 directly because native SQL doesn't understand our java class name - interface or class based projection needed or manual mapping)
    // - Selects only the fields needed by the use case.
    // - JPQL calls the ProductSummary(name, price) constructor directly.
    // - Caller receives DTOs, not managed Product entities.
    @Query("select new com.example.basiccrud.dto.ProductSummary(p.name, p.price) from Product p where p.price >= :minPrice")
    List<ProductSummary> findSummariesByPriceAtLeast(@Param("minPrice") double minPrice);
}

// Repository is not the database; it is the Java abstraction that the service uses to ask for database operations.
// CrudRepository would be enough for basic CRUD, but JpaRepository also adds paging/sorting and JPA-specific helpers.
