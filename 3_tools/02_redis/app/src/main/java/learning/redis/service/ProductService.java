package learning.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import learning.redis.model.Product;
import learning.redis.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    // This exact three-product LRU uses two Redis keys: a product hash and a recency list.
    // Hits and writes move an ID to the front; inserting a fourth product removes the oldest ID.
    private static final List<String> CACHE_KEYS = List.of("learning:crud:products", "learning:crud:lru");
    private static final RedisScript<String> CACHE = RedisScript.of(new ClassPathResource("lua/cache.lua"), String.class);
    // Database commits and Redis updates are separate; neither cache path is a distributed transaction.
    // The annotation API uses independent entries, so writes here do not invalidate its cache.
    private final ProductRepository db;

    // RedisTemplate<String, Product> can serialize/deserialize objects through a configured JSON serializer.
    // StringRedisTemplate: Product -> ObjectMapper -> JSON string -> string serializer -> Redis bytes.

    // The generic type alone does not select JSON serialization; configure key/value serializers explicitly.
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public ProductService(ProductRepository db, StringRedisTemplate redis, ObjectMapper json) {
        this.db = db;
        this.redis = redis;
        this.json = json;
    }

    public Product get(long id) throws JsonProcessingException {
        // Cache-aside: cache first; on a miss read PostgreSQL and fill the cache.

        // Alternative representation: read a product stored as JSON under its own Redis string key.
        // String value = redis.opsForValue().get("product:" + id);
        // if (value != null) return json.readValue(value, Product.class);

        // The Lua script returns null on a cache miss; the Java code then queries PostgreSQL.
        // KEYS contains the hash/list names; ARGV contains the action ("get") and product ID.
        // Unlike a plain GET, this script also updates recency on a hit.
        String cached = redis.execute(CACHE, CACHE_KEYS, "get", String.valueOf(id));
        if (cached != null) {
            log.info("CACHE HIT id={}", id);
            return json.readValue(cached, Product.class);
        }
        log.info("CACHE MISS -> DB READ id={}", id);
        Product product = db.find(id).orElseThrow(this::notFound);
        cache(product);
        return product;
    }

    public Product create(String name) throws JsonProcessingException {
        Product product = db.create(name);

        // Alternative representation: write a product as JSON under its own Redis string key.
        // This plain SET has no TTL or capacity limit; the active Lua implementation enforces LRU.
        // redis.opsForValue().set("product:" + product.id(), json.writeValueAsString(product));

        cache(product); // DB-first cache update: commit the row, then cache it before returning.
        return product;
    }

    public Product update(long id, String name) throws JsonProcessingException {
        Product product = db.update(id, name).orElseThrow(this::notFound);
        cache(product); // Same DB-first update flow as create; replace the cached product.
        return product;
    }

    public void delete(long id) {
        if (!db.delete(id)) throw notFound();
        // Delete the ID from both the product hash and recency list in one Lua execution.
        // ARGV contains the action ("delete") and product ID.
        redis.execute(CACHE, CACHE_KEYS, "delete", String.valueOf(id));
        log.info("DB DELETE + CACHE DELETE id={}", id);
    }

    private void cache(Product product) throws JsonProcessingException {
        // ARGV contains three arguments: action ("put"), product ID, and serialized product JSON.
        redis.execute(CACHE, CACHE_KEYS, "put", String.valueOf(product.id()), json.writeValueAsString(product));
        log.info("CACHE WRITE id={} (capacity=3, LRU)", product.id());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    }
}
