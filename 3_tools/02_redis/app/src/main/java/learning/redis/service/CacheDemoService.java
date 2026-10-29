package learning.redis.service;

import learning.redis.model.Product;
import learning.redis.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CacheDemoService {
    private static final Logger log = LoggerFactory.getLogger(CacheDemoService.class);
    private final ProductRepository db;

    public CacheDemoService(ProductRepository db) { this.db = db; }

    // @EnableCaching in RedisCacheConfig enables the proxies that apply these annotations.
    // Calls from the controller go through the proxy; self-invocation normally bypasses interception.
    // Both services use the same database, but their Redis caches are independent.

    // 1. @Cacheable: a hit returns the cached Product without executing this method body.
    //    A miss invokes the method and stores its eligible result.
    // 2. cacheNames selects the cache; key is a Spring Expression Language (SpEL) expression.
    //    For id=1, the default cache-name prefix produces "annotation-products::product:1".
    // 3. condition is checked before lookup: false bypasses cache reads/writes but still invokes the method.
    //    Here #id > 0 excludes nonpositive IDs from caching; it does not validate or reject the request.
    // 4. unless is checked after a normal return: true vetoes storing the result.
    //    #result is available in unless, not condition; cache hits skip this result check.
    //    The null check is defensive: this method returns a Product or throws 404, never a normal null.
    //    Thrown exceptions are not cached, and unless is not evaluated for them.
    @Cacheable(cacheNames = "annotation-products", key = "'product:' + #id", condition = "#id > 0", unless = "#result == null")
    public Product get(long id) {
        log.info("DEMO DB READ id={} (method body executed)", id);
        return db.find(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    // @CachePut: always invoke the method; store its successful result under the selected cache/key.
    // POST mirrors ProductService.create(): insert the row, then cache the returned Product.
    // The generated ID is available only after insertion, so this key uses #result.id(), not #id.
    // For a generated id=1, the physical key matches the read/update/delete key: "annotation-products::product:1".
    @CachePut(cacheNames = "annotation-products", key = "'product:' + #result.id()")
    public Product create(String name) {
        log.info("DEMO DB CREATE name={}", name);
        return db.create(name);
    }

    // @CachePut: always invoke the method; store its successful result under the selected cache/key.
    // Unlike @Cacheable, an existing cache entry does not skip this method's execution.
    // PUT updates an existing PostgreSQL row and refreshes the cached value, including its 10-minute TTL.
    @CachePut(cacheNames = "annotation-products", key = "'product:' + #id")
    public Product update(long id, String name) {
        // DB-first cache update: the database commits before the proxy updates Redis.
        // A cache-write failure does not roll back the completed database update.
        log.info("DEMO DB WRITE id={} name={}", id, name);
        return db.update(id, name).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    // @CacheEvict: remove this cache entry after successful method completion (the default).
    // It is explicit invalidation, not TTL expiry or Redis memory-pressure LRU eviction.
    // DELETE mirrors ProductService.delete(): delete the database row, then evict its cache entry.
    // A missing row throws 404; default after-invocation eviction does not run on that exception.
    @CacheEvict(cacheNames = "annotation-products", key = "'product:' + #id")
    public void delete(long id) {
        if (!db.delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        log.info("DEMO DB DELETE + CACHE EVICT id={}", id);
    }
}

// RedisCacheManager supports TTL but no built-in per-cache maximum-entry count or exact three-product LRU.
// Caffeine supports a local maximumSize limit with its own eviction policy; it is not this app's exact LRU.
// Redis server memory-pressure LRU is another mechanism and can affect rate-counter keys too.
