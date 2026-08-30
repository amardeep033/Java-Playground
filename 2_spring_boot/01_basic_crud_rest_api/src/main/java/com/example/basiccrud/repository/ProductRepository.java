package com.example.basiccrud.repository;

import com.example.basiccrud.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;


@Repository
public class ProductRepository {

    // final means the map reference cannot be reassigned after construction; the map contents can still change.
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public ProductRepository() {
        save(new Product(null, "Notebook", 80.0));
        save(new Product(null, "Pen", 10.0));
    }

    // 1.1 Return all products as a List.
    public List<Product> findAll() {
        return new ArrayList<>(products.values()); // values() returns Collection<Product>; ArrayList converts it to List<Product>.
    }

    // 1.2 Return the product if present; Optional.empty() means no product exists for this id.
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    // 1.3 Search products by optional filters: name contains search text and price is at least minPrice.
    public List<Product> searchProducts(String name, double minPrice) {
        String searchText = name == null ? "" : name.toLowerCase();
        return products.values().stream()
                .filter(product -> product.getName() != null)
                .filter(product -> product.getName().toLowerCase().contains(searchText))
                .filter(product -> product.getPrice() >= minPrice)
                .toList();
    }

    // 2 and 3: create and update both store the product in the map.
    public Product save(Product product) {
        Long id = product.getId();

        if (id == null) { // null id means a new product, so generate and assign the next id.
            id = idSequence.incrementAndGet();
            product.setId(id);
        }

        products.put(id, product);
        return product;
    }

    // 3 and 4: update and delete use this to check whether the id exists before changing data.
    public boolean existsById(Long id) {
        return products.containsKey(id);
    }

    // 5 Remove the product mapped to this id.
    public void deleteById(Long id) {
        products.remove(id);
    }
}
