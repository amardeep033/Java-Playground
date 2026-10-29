package learning.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


// spring-boot-starter-data-redis -> Spring Data Redis -> Lettuce client -> Redis (default port 6379).
// This app uses Redis for product caching and shared GET rate limiting.

@SpringBootApplication
public class RedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }
}


// Two common approaches in Spring: direct Redis access, or Spring Cache with Redis as its cache provider.
// Redis is a data store with features beyond caching: atomic counters, hashes, lists, sets, streams, and scripting.
// Spring Cache is a caching abstraction: cache lookup, storage, and invalidation; it does not expose Redis INCR.
// It can cache a numeric result, but that does not make read-modify-write an atomic counter operation.
// @Cacheable skips the method body on a cache hit, so an increment inside that method would not run.
// For annotation-based caching, enable @EnableCaching and configure a cache manager such as RedisCacheManager.
// Spring Cache mental model: declare what to cache; Spring handles lookup, execution on a miss, and eligible result storage.
// | Use case                                   | Better starting point   |
// |--------------------------------------------|-------------------------|
// | Cache a method's result                    | Spring Cache            |
// | Custom key structure and TTL per operation | RedisTemplate           |
// | Atomic counters, sets or lists             | Direct Redis operations |
// | Lua scripts and specialized Redis commands | Direct Redis access     |
// | Shared counters and rate limiting          | Atomic Redis operations |

// Differences: direct Redis access vs. Spring Cache backed by Redis.
// | Aspect               | Direct Redis access                                            | Spring Cache backed by Redis                                                            |
// |----------------------|----------------------------------------------------------------|-----------------------------------------------------------------------------------------|
// | API                  | StringRedisTemplate / RedisTemplate                            | @Cacheable, @CachePut, @CacheEvict                                                      |
// | Mental model         | Explicitly issue Redis commands                                | Declare caching around a method                                                         |
// | Scope                | Data access, caching, counters, messaging, coordination        | Cache operations; not a general Redis command API                                       |
// | Counter increments   | INCR atomically increments the value inside Redis              | No atomic increment API; @Cacheable skips the body on a hit                             |
// | Rate limiting        | Atomic counter/expiry logic with native commands or Lua        | Cache reads plus @CachePut are not an atomic counter update                             |
// | Read flow            | You implement lookup, miss handling, and cache fill            | @Cacheable checks the cache, invokes the method on a miss, then caches the result       |
// | Write / invalidation | You implement SET / DEL and their ordering                     | @CachePut invokes the method and caches its result; @CacheEvict removes entries         |
// | Keys                 | You choose the full Redis key                                  | Cache name plus generated/custom key; Redis prefix is configurable                      |
// | TTL                  | Set expiry explicitly on each operation                        | Configure RedisCacheManager; TTL can also be computed per entry                         |
// | Data structures      | Strings, hashes, lists, sets, sorted sets, streams             | Cache key/value abstraction; no collection-command API                                  |
// | Atomic custom logic  | Use native commands, Lua, transactions, or Functions           | Annotations do not make custom multi-command logic atomic                               |
// | Setup                | Configure the Redis connection and template                    | Enable caching with @EnableCaching and configure a RedisCacheManager                    |
// | Provider dependency  | Application code uses Redis-specific operations                | Caching annotations can work with other cache providers                                 |
// | Method calls         | Explicit template calls execute wherever invoked               | Default proxy mode intercepts calls through the proxy; self-invocation bypasses caching |
// | This application     | Used for product caching, three-product LRU, and rate limiting | Enabled for /cache-demo/products; caches PostgreSQL method results                      |
// Spring Cache is a provider-independent abstraction; it uses Redis only when Redis is the configured cache provider.
// Using Redis underneath does not expose all Redis features through the Spring Cache API.
// Therefore /cache-demo/products uses Spring Cache for products and direct Redis Lua for its GET rate limiter.
// @CacheEvict explicitly invalidates cache entries; it is different from Redis LRU eviction under memory pressure.
// Neither approach automatically provides a distributed transaction across PostgreSQL and Redis.
