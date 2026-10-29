# Redis Mental Model

Turn the notes from `rough.txt` into these connected ideas. Follow the executable exercises
in [README.md](README.md); use [redis_cmds.md](redis_cmds.md) when comparing Java APIs and Lua commands.

1. **Why use Redis?**
   - Reuse expensive results, share counters/rate limits, coordinate short leases, store sessions,
     or work with native collections and streams.
   - Read-heavy traffic is a candidate, not proof that caching helps: entries must be reused,
     the saved work must matter, and the chosen staleness must be acceptable.
   - In this project PostgreSQL is the product source of truth; Redis holds cached copies and counters.

2. **Memory, threads, and disk solve different questions.**
   - Redis serves data primarily from memory. RDB snapshots and AOF logs provide persistence.
   - Much command execution is serialized per server; background work and I/O can use other threads.
   - Serialized execution does not make an entire Java method, or a Redis + PostgreSQL workflow, atomic.
   - Acknowledgement, persistence and failover guarantees are separate from whether a cache value is fresh.

3. **Think key -> typed value, not tables and folders.**
   - The full string `learning:crud:get-count` is one key. Colons are application naming conventions.
   - Useful pattern: `<application>:<feature/entity>:<identifier>:<purpose>`; choose one consistent scheme.
   - Include user/tenant identity when state must be separate. This app's GET counters intentionally
     omit user and product IDs, so callers share an allowance within each API.
   - Keys are created by commands; no Redis table/schema declaration is needed.

4. **Choose the representation for the operations you need.**
   - String: a JSON document, numeric counter, or token. GET/SET access the string; INCR interprets an integer.
   - Hash: named fields with HGET/HSET. A field can itself contain a JSON string.
   - HSET updates a hash field, not part of the JSON stored inside that field. RedisJSON is a separate capability.
   - List: ordered items; LPUSH + RPOP is a simple FIFO. This project's list tracks recency instead.
   - Set: unique members, membership tests and set operations.
   - Sorted set: unique members ordered by score; useful for ranking and time-window algorithms.
   - Stream: retained entries plus consumer groups and explicit pending-work management.

5. **There are two common Spring access levels.**
   - Direct Redis APIs expose data operations, collections, counters and scripts.
   - Spring Cache abstracts cache lookup, storing and invalidation around methods. Redis is one provider;
     Caffeine and other providers can implement that abstraction too.
   - Redis is useful beyond caching. Spring Cache does not expose an atomic INCR just because its
     backend happens to be Redis. The annotation demo therefore still uses Redis Lua for rate limiting.

6. **Serialization is a deliberate boundary.**
   - Here: Product -> ObjectMapper -> JSON string -> StringRedisTemplate -> Redis bytes.
   - Read reverses that path. StringRedisTemplate handles strings; it does not automatically turn Product into JSON.
   - RedisTemplate<String, Product> can use a configured JSON serializer; its generic types alone do not choose one.
   - The annotation cache manager configures typed Product JSON serialization centrally.
   - A key's name, value type, and byte format must match what every reader expects.

7. **Read path: cache-aside.**
   - Cache hit -> return the cached product.
   - Miss -> query PostgreSQL -> fill cache -> return the product.
   - Missing DB row -> this app throws 404. It does not cache a negative result.
   - Two callers can both miss and both read the DB. An atomic cache write does not coordinate the loader.

8. **Write path: decide how the cache follows the database.**
   - This app commits the DB change, then synchronously updates the cache: a write-through-style application flow.
   - Another common choice is DB update -> invalidate -> refill on the next read.
   - Write-behind accepts/cache-writes first and persists asynchronously; that needs loss, ordering and retry handling.
   - A PostgreSQL commit and a Redis write are separate. A failure between them can leave stale data;
     an HTTP error does not prove the DB write was rolled back.
   - A concurrent reader can also refill an old value after a writer refreshed or invalidated it.

9. **Spring Cache changes who implements the read/write wrapper.**
   - @EnableCaching enables the usual proxy-based annotation interception.
   - @Cacheable: hit skips the body; miss executes it and stores an eligible successful result.
   - @CachePut: always execute the body; cache its result afterward.
   - @CacheEvict: explicitly remove an entry, normally after successful method completion.
   - Controller -> service goes through the proxy. A call to another method on the same service object
     normally bypasses interception; annotations are not magic around every Java call.

10. **A cache name, an entry key, and a Redis key are related but different.**
    - Cache name: `annotation-products`.
    - Entry key expression: `'product:' + #id`, giving `product:42`.
    - Physical Redis key with the default prefix: `annotation-products::product:42`.
    - POST needs `#result.id()` because PostgreSQL generates the ID during insertion.
    - The direct cache instead stores ID `42` as a field inside `learning:crud:products`.

11. **Condition decides participation; unless vetoes a result.**
    - condition uses inputs before lookup/execution. False bypasses cache reads/writes but does not reject the request.
    - unless runs after a normal method result and can inspect #result. True means do not store that result.
    - A cache hit skips the method and its result-veto evaluation.
    - Here condition is `#id > 0`; unless is a defensive null guard. The method throws for missing data,
      so a 404 is not an example of the guard vetoing a null return. No negative caching exists here.

12. **TTL, invalidation and LRU are different causes of removal.**
    - TTL: an entry has a deadline. TTL -2 means absent, -1 means no deadline.
    - Explicit invalidation: the application deletes an entry because its cached copy should not be used.
    - LRU: retain recently used data and discard the least recent when capacity is exceeded.
    - LFU instead favors frequently used data. Redis's memory-pressure policies approximate these choices.
    - The direct cache has an exact three-product limit through a Lua-managed hash/list, not Redis maxmemory.
    - The annotation cache has a ten-minute TTL, not the three-product LRU rule.
    - TTL on RedisCacheManager does not change direct-template writes. Ordinary hits do not extend this TTL.

13. **Know the four common cache failure patterns.**
    - Penetration: repeatedly ask for absent data -> short negative cache, validation, possible membership filtering.
    - Breakdown/stampede: one hot entry expires -> many simultaneous DB loads -> coalesce/coordinate or refresh ahead.
    - Avalanche: many keys miss together, often after synchronized expiry/outage -> jitter, load protection, refresh strategy.
    - Staleness: underlying data changed -> match writes with appropriate invalidation/versioning/change events.
    - These are design tools, not implemented features here. Small TTL bounds some staleness; it does not
      fix every race. Refresh-ahead does extra work to protect hot entries before they expire.

14. **Atomic command is not the same as atomic sequence.**
    - INCR atomically increments; Java GET -> add -> SET can lose concurrent updates.
    - INCR followed by EXPIRE is still two operations. A crash can leave a first-use counter without expiry.
    - Lua combines a short decision and its commands without interleaving on that server.
    - MULTI/EXEC groups queued commands; WATCH supports optimistic checks/retries. Use one connection.
    - Pipelining reduces round trips but does not itself provide atomicity.
    - Lua/Redis transactions do not offer SQL-style rollback of commands already executed successfully.
    - Redis Functions are deployable server-side libraries; this app uses bundled Lua scripts instead.

15. **Rate limiting needs shared state plus correct timing.**
    - This app: controller -> INCR -> on first request EXPIRE 60 -> allow count <= 5, otherwise 429.
    - Both actions run inside one Lua execution. Product cache hits still go through the limiter first.
    - The two API paths use different counter keys, but each path shares its key across users/instances.
    - Rejected requests do not extend the deadline. Restarting Spring does not reset the Redis key.
    - Fixed-window boundaries permit bursts; different algorithms give different guarantees.
    - @Cacheable cannot implement an increment in its body on every request because hits skip that body.
    - A read followed by @CachePut is not an atomic increment; repeated writes can also reset cache TTL.

16. **Locks are expiring leases, not a pause button for other systems.**
    - SET key owner-token NX with expiry can acquire a lease in one command.
    - Release must compare the owner token and delete atomically; plain DEL can remove a new owner's lease.
    - A paused owner can continue after expiry. Token-checked release does not prevent its stale DB writes.
    - Correctness-sensitive work may require fencing enforced by the protected resource and stronger coordination.
    - Lock coordination is a study topic here; no lock API is implemented.

17. **Queues, Pub/Sub and Streams have different failure behavior.**
    - List pop removes before processing; a worker crash can lose the popped work.
    - Pub/Sub sends live messages; a disconnected subscriber has no offline replay.
    - Streams retain entries and track group pending work. XACK clears pending state, not retained entries.
    - Abandoned pending work needs a recovery policy, rather than assuming new reads automatically reclaim it.
    - RabbitMQ's ACK, retry queue and DLX from the previous lesson are not automatically reproduced by a list.

18. **Shared database does not imply a shared coherent cache.**
    - Both APIs query the same table, but their cache representations and lifetimes differ.
    - Writing/deleting through one does not invalidate the other cache.
    - Use separate products when comparing full lifecycles, or explicitly clear stale entries across paths.
    - External writers create the same invalidation question even when only one HTTP API exists.

19. **Observe the boundaries when something fails.**
    - Check HTTP status, application hit/miss logs, DB state, key type/value, TTL, and limiter state separately.
    - `spring.data.redis.timeout=2s` is a client command wait limit, not a cache lifetime.
    - Redis failure can block the limiter before the product service is reached. There is no fallback policy here.
    - Sending every failed cache request to PostgreSQL can overload it; fallback needs capacity planning.
    - Scan incrementally, inspect memory/evictions/persistence health, and measure application DB reads/hit ratio.
    - A command timeout can leave the write outcome uncertain; retries are not automatically idempotent.

20. **What this lesson implements versus what remains design work.**
    - Implemented: real PostgreSQL CRUD, direct JSON/hash/list caching, exact three-product LRU,
      annotation caching, a shared Lua rate limiter per path, and native local Redis startup.
    - Not implemented: negative caching, TTL jitter, coordinated refill, refresh-ahead, cross-path invalidation,
      lock/session/messaging endpoints, outage fallback, clustering, or exactly-once cross-system writes.
    - Use the [README notes and scenario questions](README.md#2-notes-as-tables) to test these distinctions.
