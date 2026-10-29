package learning.redis.repository;

import java.util.Optional;
import learning.redis.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private final JdbcTemplate db;
    private final RowMapper<Product> mapper = (rs, row) ->
            new Product(rs.getLong("id"), rs.getString("name"));

    public ProductRepository(JdbcTemplate db) { this.db = db; }

    public Product create(String name) {
        return db.queryForObject("INSERT INTO product(name) VALUES (?) RETURNING id, name", mapper, name);
    }

    public Optional<Product> find(long id) {
        return db.query("SELECT id, name FROM product WHERE id = ?", mapper, id).stream().findFirst();
    }

    public Optional<Product> update(long id, String name) {
        return db.query("UPDATE product SET name = ? WHERE id = ? RETURNING id, name", mapper, name, id)
                .stream().findFirst();
    }

    public boolean delete(long id) {
        return db.update("DELETE FROM product WHERE id = ?", id) > 0;
    }
}
