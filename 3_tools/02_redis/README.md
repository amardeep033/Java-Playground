# 02 Redis + PostgreSQL + Spring Boot

One PostgreSQL table, `product(id, name)`, with two matching CRUD APIs:
**direct Redis operations** and **Spring Cache backed by Redis**.
Both controllers use Redis Lua for GET rate limiting.

Read this document for experiments and comparisons; use [mental_model.md](mental_model.md)
for the reasoning behind them. [redis_cmds.md](redis_cmds.md) maps Spring APIs to Redis/Lua commands.

## 1. Curl: Run, Compare, And Observe

### 1.1 Start The App

Run these commands from `02_redis/`. Start PostgreSQL using your existing local setup first.

```bash
./run.sh
```

The script starts/reuses native Redis, cleans the Maven build, and starts Spring Boot.
It tries to stop a listener on the requested HTTP port; if that port remains occupied,
it chooses the next free port. **Use the URL printed by the script** in the second terminal:

```bash
BASE=http://localhost:8080
```

| Requirement / Default | This Project |
| --- | --- |
| Java / Maven | Java 17+; Spring Boot 3.5.16. |
| Redis | Native server and CLI installed in `~/.local/bin`; no Docker. |
| PostgreSQL | `jdbc:postgresql://localhost:5432/postgres`, local username/password `postgres`. |
| Schema | `schema.sql` creates singular `product`; it does not reuse the older JPA lesson's `products` table. |
| Redis connection | `localhost:6379`, no password for this local instance. |
| HTTP | Port 8080 by default; override with `SERVER_PORT`. |
| Shell utilities | Bash, `lsof`, and `ss` for `run.sh`; curl and Redis CLI for experiments. |

```bash
redis-cli ping
# Expected: PONG
```

For an already-running remote/password-protected Redis, start the app directly:

```bash
mvn -f app/pom.xml spring-boot:run
```

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`,
`REDIS_PASSWORD`, and `SERVER_PORT` as needed. `${DB_USERNAME:postgres}` means use
`DB_USERNAME` if present, otherwise the default `postgres`.

### 1.2 Direct Redis: Four CRUD Commands

POST returns the generated ID. Set `D_ID` to that actual ID before continuing;
IDs are not guaranteed to start at 1 or be consecutive.

```bash
# CREATE: database insert, then Lua cache write. Expected HTTP 201.
curl -i -X POST "$BASE/products" \
  -H 'Content-Type: application/json' -d '{"name":"Keyboard"}'

D_ID=1  # Replace with the ID returned above.

# READ: cache-aside; newly created products are already cached. Expected HTTP 200.
curl -i "$BASE/products/$D_ID"

# UPDATE: database update, then Lua cache replacement. Expected HTTP 200.
curl -i -X PUT "$BASE/products/$D_ID" \
  -H 'Content-Type: application/json' -d '{"name":"Mechanical Keyboard"}'

# DELETE: database row and direct-cache entry removed. Expected HTTP 204.
curl -i -X DELETE "$BASE/products/$D_ID"
```

### 1.3 Spring Cache: The Same Four CRUD Commands

Use a different product so the independent caches do not obscure the comparison.

```bash
# CREATE: @CachePut caches the returned product using its generated ID. HTTP 201.
curl -i -X POST "$BASE/cache-demo/products" \
  -H 'Content-Type: application/json' -d '{"name":"Mouse"}'

A_ID=2  # Replace with the ID returned above.

# READ: @Cacheable returns the cached value or invokes the service on a miss. HTTP 200.
curl -i "$BASE/cache-demo/products/$A_ID"

# UPDATE: @CachePut always invokes the method and updates the cache. HTTP 200.
curl -i -X PUT "$BASE/cache-demo/products/$A_ID" \
  -H 'Content-Type: application/json' -d '{"name":"Wireless Mouse"}'

# DELETE: delete the database row; @CacheEvict removes the cache entry. HTTP 204.
curl -i -X DELETE "$BASE/cache-demo/products/$A_ID"
```

Both DELETE endpoints delete **database data as well as the relevant cache entry**.
There is no cache-only HTTP DELETE endpoint. For the experiments below, create fresh products
using POST and update `D_ID` / `A_ID`; the rows deleted above cannot demonstrate cache refill.

### 1.4 Observe A Cache Miss Followed By A Hit

Create one product through each path and note their IDs. POST already populated the caches,
so remove only their cache entries while the app is otherwise idle:

```bash
# Direct cache: remove both the product field and its LRU-list membership.
redis-cli HDEL learning:crud:products "$D_ID"
redis-cli LREM learning:crud:lru 0 "$D_ID"
curl -i "$BASE/products/$D_ID"
curl -i "$BASE/products/$D_ID"

# Annotation cache: each product is a separate Redis string key.
redis-cli DEL "annotation-products::product:$A_ID"
curl -i "$BASE/cache-demo/products/$A_ID"
curl -i "$BASE/cache-demo/products/$A_ID"
```

| Path | First GET After Cache Removal | Second GET |
| --- | --- | --- |
| `/products/{id}` | `CACHE MISS -> DB READ`, then `CACHE WRITE`. | `CACHE HIT`. |
| `/cache-demo/products/{id}` | `DEMO DB READ` because the service method executes. | No new `DEMO DB READ`; the proxy returns the cached value. |

The CLI removals are manual lab operations, not a concurrent invalidation algorithm.
The two direct-cache removal commands above are separate operations; the app's Lua script
normally keeps that hash and list in sync atomically.

Inspect the storage:

```bash
redis-cli HGET learning:crud:products "$D_ID"
redis-cli LRANGE learning:crud:lru 0 -1
redis-cli TTL learning:crud:products
redis-cli GET "annotation-products::product:$A_ID"
redis-cli TTL "annotation-products::product:$A_ID"
```

The direct hash has no TTL (`-1`). An annotation entry starts around 600 seconds after a write.
A normal annotation-cache hit does not reset that TTL with the current configuration.

### 1.5 Observe Exactly Three Cached Products And LRU

Create three fresh direct-API products by repeating POST with names A, B, and C.
Record their returned IDs as `D1`, `D2`, and `D3`. After these three writes, previous direct
entries have been displaced; the list is `[D3, D2, D1]` from most to least recently used.

```bash
# Assign the actual returned IDs; these numbers are examples only.
D1=10
D2=11
D3=12

# Touch A: recency becomes [D1, D3, D2].
curl -i "$BASE/products/$D1"

# Add D: B (D2) is now the least recently used product and is evicted.
curl -i -X POST "$BASE/products" \
  -H 'Content-Type: application/json' -d '{"name":"D"}'

redis-cli HLEN learning:crud:products
redis-cli HEXISTS learning:crud:products "$D2"
redis-cli LRANGE learning:crud:lru 0 -1
```

Expected hash length: **3**. Expected `HEXISTS` for B: **0**.
GET B again: its database row still exists, so it is loaded and another cached product is evicted.
POST/PUT count as recent use in this implementation too. Run this exercise without competing
requests, which would legitimately change recency.

This is application-defined **product-entry LRU inside Redis**, not Redis's memory-pressure
LRU policy. Redis stores auxiliary keys and counters too; the whole server is not limited to three keys.
The annotation cache has a TTL but **does not** implement this three-product capacity.

### 1.6 Observe The GET Rate Limit

Every GET counts before any cache lookup: hits, misses, and missing IDs. Limits are shared
across callers and IDs within one API path, with a separate counter for each path.
Wait for a fresh window before running this experiment; earlier GETs may have used the allowance.

```bash
for n in {1..6}; do
  curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$BASE/products/$D_ID"
done

for n in {1..6}; do
  curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$BASE/cache-demo/products/$A_ID"
done

redis-cli GET learning:crud:get-count
redis-cli TTL learning:crud:get-count
redis-cli GET learning:cache-demo:get-count
redis-cli TTL learning:cache-demo:get-count
```

For existing products, a fresh window yields five **200** responses, then **429**.
For missing products, it yields five **404** responses, then **429**.
The fixed window lasts **60 seconds from the first request**. Rejected attempts increase the
counter but do not extend its TTL. Wait for expiry before continuing other GET experiments.
POST, PUT, and DELETE do not consume the GET budget.

### 1.7 Observe Condition, Missing Data, And Cache Refresh

```bash
# condition="#id > 0" is false: bypass cache lookup/storage, but execute the service.
# With no database row for ID 0, the service throws 404.
curl -i "$BASE/cache-demo/products/0"

# @CachePut refreshes the existing product's cached value immediately.
curl -i -X PUT "$BASE/cache-demo/products/$A_ID" \
  -H 'Content-Type: application/json' -d '{"name":"Updated Again"}'
curl -i "$BASE/cache-demo/products/$A_ID"
```

`condition` is not request validation: a false condition does not stop method execution.
`unless="#result == null"` is present as a defensive example. This service returns a Product
or throws 404, so **there is no normal null-return case here**. Exceptions are not cached;
a 404 does not demonstrate `unless=true`. No negative caching is implemented in either API.

### 1.8 Read, Build, Stop, And Troubleshoot

| Order | File | Follow This Responsibility |
| --- | --- | --- |
| 1 | [ProductController](app/src/main/java/learning/redis/controller/ProductController.java) | Direct API, GET counter, 429 decision. |
| 2 | [ProductService](app/src/main/java/learning/redis/service/ProductService.java) | Explicit cache-aside and DB-first writes. |
| 3 | [cache.lua](app/src/main/resources/lua/cache.lua) | Hash/list updates and exact three-product LRU. |
| 4 | [CacheDemoController](app/src/main/java/learning/redis/controller/CacheDemoController.java) | Matching API and its independent GET counter. |
| 5 | [CacheDemoService](app/src/main/java/learning/redis/service/CacheDemoService.java) | `@Cacheable`, `@CachePut`, `@CacheEvict`, keys and conditions. |
| 6 | [RedisCacheConfig](app/src/main/java/learning/redis/config/RedisCacheConfig.java) | `@EnableCaching`, JSON serialization, 10-minute TTL. |
| 7 | [ProductRepository](app/src/main/java/learning/redis/repository/ProductRepository.java) | Shared PostgreSQL CRUD using JdbcTemplate. |
| 8 | [rate-limit.lua](app/src/main/resources/lua/rate-limit.lua) | Atomic increment plus first-request expiry. |
| 9 | [properties](app/src/main/resources/application.properties), [schema](app/src/main/resources/schema.sql) | Connections, timeout, table creation. |

```bash
mvn -f app/pom.xml clean package -DskipTests
# Ctrl+C stops the app; Redis stays running.
redis-cli shutdown
```

Redis data/logs live in `.redis/` when started by this script. It enables AOF; if the script
reuses an existing server, that server keeps its original configuration and storage location.
There are no separate automated tests in this learning project; use the experiments above.

| Symptom | Check / Action |
| --- | --- |
| Port 8080 occupied | Use the port printed by `run.sh` in `BASE`; the script can select the next port. |
| `redis-cli: command not found` | Use `~/.local/bin/redis-cli` or add that directory to PATH. |
| PostgreSQL connection failure | Start PostgreSQL and check the datasource properties/environment. |
| Redis connection refused | Check `redis-cli ping`, host/port, and `.redis/redis.log`. |
| HTTP 429 while checking a 404 | The controller rejects before the service; wait for the window to expire. |
| Wrong value after switching API paths | The two caches are independent; clear the other cached entry. |
| Java imports disappear or red underlines conflict with Maven | Keep one Java language server; this workspace recommends Red Hat and disables save-time import cleanup. |
| `Unresolved compilation problem` at runtime | Restore missing imports and rebuild; `run.sh` runs Maven clean before startup. |
| Cache JSON cannot be read | Inspect key type, value, and configured serializer; do not reuse a key for incompatible formats. |

## 2. Notes As Tables

### 2.1 This Project's Boundaries

| Concern | Direct API | Annotation API |
| --- | --- | --- |
| Base path | `/products` | `/cache-demo/products` |
| Database | Same PostgreSQL `product(id, name)` table. | Same repository and rows. |
| Create | DB insert, then explicit cache write. | DB insert; `@CachePut` stores the returned product. |
| Read | Explicit Redis lookup, DB on miss, then fill. | Proxy checks cache; method queries DB only on a miss. |
| Update | DB update, then explicit cache replacement. | DB update; `@CachePut` replaces the entry. |
| Delete | DB deletion, then Lua cache removal. | DB deletion; `@CacheEvict` removes the entry after normal return. |
| Product cache | Redis hash plus recency list. | One Redis string key per product. |
| Capacity / lifetime | Three products with LRU; no TTL. | Ten-minute TTL; no custom capacity-three LRU. |
| GET rate limiting | Redis Lua in controller, key `learning:crud:get-count`. | Same Lua in controller, key `learning:cache-demo:get-count`. |
| Cross-path invalidation | Does not refresh or remove annotation entries. | Does not refresh or remove direct entries. |

Use one API path for each product's full experiment. Writing/deleting via the other path can
leave stale entries, including a cached product whose database row has been deleted.

### 2.2 Redis Access Versus Spring Cache

| Question | Direct Redis Access | Spring Cache Backed By Redis |
| --- | --- | --- |
| Scope | Data access, counters, collections, messaging, coordination, caching. | Cache lookup, storage, invalidation. |
| API | `StringRedisTemplate`, `RedisTemplate`, scripts and Redis commands. | `@Cacheable`, `@CachePut`, `@CacheEvict` and CacheManager. |
| Method result caching | Write the lookup/miss/fill path yourself. | Declare caching around a method. |
| Atomic increments | `INCR` runs inside Redis. | No atomic increment operation in the cache abstraction. |
| Rate limiter | Implement atomic counter/expiry logic. | Caching a numeric result is not an atomic counter update. |
| TTL | Set explicitly when needed. | Configure RedisCacheManager defaults/per-cache settings. |
| Providers | Application code uses Redis-specific operations. | Can use Redis, Caffeine, ConcurrentMap, JCache-backed providers, etc. |
| Invocation | Explicit commands execute when reached. | Default proxy mode intercepts calls through the Spring proxy. |

### 2.3 Annotations And Their Exact Behavior Here

| Item | Meaning | Detail In This App |
| --- | --- | --- |
| `@EnableCaching` | Enables annotation-driven interception. | Declared in RedisCacheConfig. |
| `@Cacheable` | Return an entry on a hit; invoke method and cache eligible result on a miss. | A hit skips `CacheDemoService.get()` entirely. |
| `@CachePut` | Always invoke the method, then store its successful result. | Used for create and update. |
| `@CacheEvict` | Remove selected entries; default timing is after successful invocation. | Delete removes the DB row; the interceptor then removes the cache entry. |
| `cacheNames` | Selects a named cache. | `annotation-products`. |
| Read/update/delete key | SpEL chooses the entry key. | `'product:' + #id`. |
| Create key | SpEL uses the generated result after insertion. | `'product:' + #result.id()`. |
| `condition` | Check inputs before cache lookup; false bypasses cache use but still calls the method. | `#id > 0`; not a validation rule. |
| `unless` | Check the normal result after execution; true vetoes storing it. | `#result == null`; defensive because this method throws for absence. |
| Exceptions | A thrown method exception is not cached as a result. | Missing row throws HTTP 404; `unless` is not evaluated for that exception. |
| Self-invocation | Calls within the same object bypass the usual proxy. | Controller-to-service calls go through the proxy. |

Reference: [Spring's annotation semantics](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html).

### 2.4 Keys And Serialization

| Item | Stored Representation / Rule | Consequence |
| --- | --- | --- |
| `learning:crud:products` | Hash field = product ID; field value = product JSON. | Three fields means three cached products, not three top-level Redis keys. |
| `learning:crud:lru` | List of ID strings, most recent first. | GET hits and writes move an ID to the front. |
| `annotation-products::product:42` | Redis string containing JSON for product 42. | Cache name prefix plus the SpEL-generated entry key. |
| Rate keys | Integer-valued Redis strings with 60-second expiry. | `INCR` updates a counter; the key name itself gives no behavior. |
| Colons in keys | Application naming convention. | No actual directory tree or automatic parent/child relationship. |
| `StringRedisTemplate` | String serialization; this code explicitly uses ObjectMapper for Product JSON. | Reading reverses JSON string to Product. |
| `RedisTemplate<String, Product>` | Configurable serializers. | Generic type parameters alone do not select JSON serialization. |
| Annotation values | RedisCacheConfig uses a typed JSON serializer for Product. | No Java Serializable requirement for these JSON values. |

TTL and serialization configured on RedisCacheManager do not change direct-template operations.
Reference: [RedisCacheManager configuration](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).

### 2.5 Read And Write Strategies

| Strategy | Flow | Boundary / This Project |
| --- | --- | --- |
| Cache-aside | Read cache; on miss load DB and fill. | Both product-read approaches implement this pattern. |
| DB-first cache update | Commit DB change, then synchronously replace cached value. | The write-through-style flow used here; not a distributed transaction. |
| DB-first invalidation | Update DB, delete cache, refill on next read. | An alternative update strategy; current PUT replaces rather than just invalidating. |
| Write-behind | Accept/cache write, persist asynchronously later. | Not implemented; needs durable delivery, ordering, retries and loss handling. |
| Refresh-ahead | Refresh selected hot entries before expiry. | Not implemented; useful when a hot-key miss would overload the source. |
| Negative caching | Store absence briefly to reduce repeated misses. | Not implemented; 404 requests repeat DB reads when permitted by the rate limiter. |

### 2.6 Expiry, Invalidation, And Eviction

| Mechanism | Trigger | What It Does Here |
| --- | --- | --- |
| TTL expiry | Entry reaches its configured lifetime. | Annotation cache entries expire after ten minutes from a successful cache write. |
| Explicit invalidation | Application removes a chosen cache entry. | DELETE uses Lua or `@CacheEvict`. |
| Custom product LRU | Fourth product enters the direct cache. | Removes the least recently used product from the hash/list. |
| Redis memory-pressure eviction | Server reaches configured maxmemory pressure. | Separate server policy; this app does not configure Redis's memory-based LRU. |
| `noeviction` | Refuse writes needing memory instead of evicting victims. | A server policy, not a promise of unlimited memory. |
| `allkeys-lru` / `allkeys-lfu` | Memory pressure selects approximately least recent/frequent keys. | Server policies, not exact three-product capacity controls. |
| `volatile-*` | Eviction candidates are keys with expiry. | Can still affect TTL-bearing coordination keys; not a data-isolation mechanism. |
| `TTL = -2` | Key is absent. | Could be expired, explicitly removed, or never created. |
| `TTL = -1` | Key exists without expiry. | Expected for the direct product hash/list. |
| `TTL >= 0` | Remaining lifetime in seconds. | Distinct from the Redis client command timeout. |

A normal SET generally clears the old key TTL unless the command preserves/sets one;
INCR preserves an existing TTL. Reference: [Redis eviction policies](https://redis.io/docs/latest/develop/reference/eviction/).

### 2.7 GET Rate Limiter

| Step / Property | Behavior | Why It Matters |
| --- | --- | --- |
| Controller entry | Increment the counter before invoking the service. | Cache hits are limited too. |
| First request | Lua runs INCR, sees 1, and assigns 60-second expiry. | Counter and first TTL are applied without interleaving. |
| Requests 1–5 | Controller continues to the service. | A missing product can still produce 404. |
| Request 6 onward | Counter exceeds 5; controller returns 429. | Service/cache lookup is skipped. |
| Denied attempts | Still increment the counter, without refreshing expiry. | Rejections do not prolong the window. |
| Identity scope | One counter per API, across all users and product IDs. | Not per user/IP; another caller can exhaust your allowance. |
| Multiple app instances | Share allowance if using the same Redis/database/key. | No per-JVM counter is used in either controller. |
| Boundary behavior | A new window starts after key expiry. | Bursts can happen around window boundaries. |
| Application restart | Does not reset Redis counters by itself. | Existing counter TTL still controls the window. |

### 2.8 Concurrency And Execution Choices

| Approach | Atomicity / Execution | When It Fits / Limitation |
| --- | --- | --- |
| One INCR | Atomic increment. | Prefer over Java GET + 1 + SET. |
| INCR then EXPIRE | Two separate commands. | Crash/interleaving gap can leave the counter without TTL. |
| Lua | Related commands and decisions execute without interleaving. | Used for limiter and LRU; keep scripts short. |
| MULTI/EXEC | Queued commands execute together without other clients interleaving. | No SQL-style rollback for execution errors. |
| WATCH + transaction | Detect a watched-key change, abort and retry. | Client-side optimistic decision/update; use one connection. |
| SessionCallback | Keeps Spring transaction operations on one connection. | Separate template calls are not sufficient connection coordination. |
| Pipelining | Batch requests to reduce round trips. | Does not itself provide atomic execution. |
| SET NX EX | Conditional create plus lease expiry in one command. | Atomic lease acquisition; adding a separate INCR does not make the pair atomic. |
| Redis Functions | Deploy a named server-side library, invoke with FCALL. | Alternative reusable server logic; not used by this app. |
| PostgreSQL write + Redis write | Two different systems. | Neither Lua nor a Redis transaction makes the whole pair atomic. |

Reference: [Redis transactions and errors](https://redis.io/docs/latest/develop/using-commands/transactions/).

### 2.9 Cache Failures And Mitigations

| Failure Pattern | Concrete Trigger | Possible Mitigation / Current Coverage |
| --- | --- | --- |
| Penetration | Repeatedly request IDs absent from PostgreSQL. | Short negative caching, validation, filtering; no negative caching here. |
| Breakdown / stampede | Many callers miss one hot entry together. | Coalesce refills, coordinate a loader, refresh-ahead; not implemented here. |
| Avalanche | Many entries expire together, or Redis becomes unavailable. | TTL jitter, protect DB capacity, load shedding; not implemented here. |
| Stale data | External write or independent cache misses invalidation. | Versioning/change events, explicit invalidation, suitable TTL; direct cache has no TTL bound. |
| Stale refill | Reader gets old DB data, writer updates cache, reader stores old result afterward. | Refill/write coordination or version checks; ordinary cache-aside is not strongly consistent. |
| Redis outage | Read, counter, or cache write cannot reach Redis. | Current code propagates errors; no DB fallback or limiter fail-open policy. |
| Partial write | DB succeeds, Redis update fails. | Treat outcome carefully; durable invalidation/outbox/change events may be needed. |
| Session/lock eviction | Disposable cache shares an evicting server with important keys. | Separate resources/policies according to durability and correctness needs. |

### 2.10 Data Structures And Choosing The Right Tool

| Structure / Feature | Example | Important Boundary |
| --- | --- | --- |
| String | JSON product, integer counter, token. | A JSON string is not a Redis hash. |
| Hash | Fields of an object, or this app's ID-to-JSON cache. | HSET changes a hash field; it does not parse/update JSON within that field. |
| List | Recency order, LPUSH + RPOP FIFO. | Pop removes before processing; no processing ACK. |
| Set | Unique membership via SADD/SISMEMBER. | Unordered; duplicates are not stored as new members. |
| Sorted set | Score-based leaderboard or time-window entries. | Distinct member with numeric ordering score. |
| Stream | Retained entries and consumer-group pending/ACK state. | XACK clears pending state, not the retained stream entry. |
| Pub/Sub | Live notifications to connected subscribers. | No offline backlog or automatic replay. |
| Lease | SET NX with expiry and an owner token. | Token-checked release does not stop an expired owner from writing elsewhere. |
| Session storage | Shared login/session state with a lifetime. | Persistence, expiry and eviction choices affect session availability. |

Only product caching and GET rate limiting are implemented by this app. The other rows
are study directions, not additional HTTP endpoints. For lock correctness, see
[Redis's lease/ownership discussion](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/).

### 2.11 Memory, Disk, And Operational Settings

| Concern | Mental Model | Current App / Limit |
| --- | --- | --- |
| Memory | Redis serves its data primarily from RAM. | Persistence does not turn cache hits into ordinary PostgreSQL queries. |
| Execution | Much command execution is serialized on a server; background/I/O work can use other threads. | This does not make a multi-call Java method atomic. |
| RDB / AOF | Snapshot or write-log persistence. | `run.sh` enables AOF when it starts a new server. |
| AOF everysec | Periodic fsync trades durability for write cost. | Typical crash-loss exposure is roughly the latest second; inspect actual reused-server settings. |
| Command timeout | How long the client waits for a Redis command. | `spring.data.redis.timeout=2s`; unrelated to product TTL. |
| Password placeholder | `${REDIS_PASSWORD:}` falls back to empty. | Local server starts without authentication. |
| Redis failure response | Unhandled Redis errors propagate. | Usually HTTP 500 here; there is no custom 503 handler in the current code. |
| Inspection | SCAN, TYPE, TTL, INFO, application logs. | SCAN needs cursor iteration and is not a consistent snapshot; avoid routine KEYS scans on a busy server. |
| Metrics | Application cache hits/misses, DB reads, latency, Redis memory/evictions/errors. | Redis-wide keyspace hit metrics include operations beyond this product cache. |

Reference: [Redis persistence tradeoffs](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/).

## 3. Interview Scenarios And Tricky Q&A

| No. | Scenario / Question | Expected Reasoning |
| --- | --- | --- |
| 1 | POST succeeds, but the first GET produces no DB-read log. Is caching broken? | POST already populated the cache. A hit should avoid a database read. Remove only the cache entry to observe a miss. |
| 2 | There are four PostgreSQL rows but only three direct-cache fields. Was data deleted? | No. The LRU cap applies to cached products; eviction does not delete PostgreSQL rows. |
| 3 | Three products are written A, B, C; then A is read; then D is written. Which leaves the cache? | B. After touching A, B is least recent; writes also update recency. |
| 4 | Redis contains more than three keys. Did the capacity limit fail? | No. It limits fields/products in the direct hash; the list, counters, and annotation keys are extra server keys. |
| 5 | Annotation GET works although its service log never appears. How? | The Spring proxy returns the cached result without invoking the service method. |
| 6 | A counter increment is inside @Cacheable. Why does it stop increasing? | Hits bypass the method body. Result caching does not expose an atomic increment API. |
| 7 | Two requests read cached count 4 and both @CachePut 5. Why only one effective increment? | Read-modify-write was split across operations. Use an atomic server-side increment; unconditional cache replacement cannot fix this race. |
| 8 | Each INCR is atomic, so why use Lua for the limiter? | Atomicity of one command does not cover the conditional EXPIRE. Lua removes the first-increment/expiry gap. |
| 9 | The client dies after first INCR but before EXPIRE. What can persist? | A counter without expiry, potentially denying requests indefinitely. This app's script combines those steps. |
| 10 | Is SET NX EX followed by INCR enough to remove that gap? | No. The key can expire between commands, and INCR can recreate it without a TTL. Separate atomic commands are not an atomic sequence. |
| 11 | A user made one request and got 429. What should you inspect? | Counter key scope and existing TTL. Other callers and earlier GETs share the same API counter. |
| 12 | A nonexistent product gets five 404s, then 429. Why change the error? | The limiter runs first. Once blocked, the service never checks whether the product exists. |
| 13 | PUT still works while GET returns 429. Is that inconsistent? | Only GET is limited in this application. Method-specific enforcement is intentional. |
| 14 | Two app instances each accept five requests against the same key. Is that expected here? | No, if they truly share Redis/database/key and window. Check configuration and distinct key names; the allowance should be shared. |
| 15 | Restarting Spring did not clear the GET limit. Why? | Counter state lives in Redis. App restart does not replace that key or remove its expiry. |
| 16 | A fixed-window limiter allows a burst around expiry. Is INCR losing updates? | Not necessarily. Five near one window's end and five in the next can happen by design. Consider a different limiting algorithm if that burst is unacceptable. |
| 17 | A Lua script updates a key and then fails on a later command. Does it restore the first value? | No. No interleaving is different from rollback; writes already executed can remain. |
| 18 | MULTI/EXEC has a command execution error. Are previous writes undone? | No SQL-style rollback. Distinguish queue-time errors from failures while executing queued commands. |
| 19 | Switching direct calls to a pipeline solved latency but not stale counters. Why? | Pipelining reduces round trips; it does not make a read/modify/write sequence atomic. |
| 20 | The same bean calls its own @Cacheable method, and the DB is hit every time. Why? | Default proxy interception is bypassed by self-invocation; an annotation alone does not intercept that internal call. |
| 21 | condition=false, yet the DB query still runs. Is that a bug? | No. It disables cache participation, not the method. Use validation or explicit logic to reject the input. |
| 22 | Why can POST use #result.id() but condition cannot inspect #result? | The generated result exists after insertion. A condition is evaluated before invocation; unless and a post-invocation cache-put key can inspect the result. |
| 23 | A 404 is returned. Did unless="#result == null" reject a cached result? | Not here. The service throws; there is no normal null result to inspect. Exceptions are not cached. |
| 24 | Adding a ten-minute RedisCacheManager TTL did not expire the direct hash. Why? | The manager configures Spring Cache entries, not arbitrary RedisTemplate operations. |
| 25 | A user GETs the annotation product every second, but it still expires. Why? | Normal cache hits do not refresh this configured TTL. TTL and sliding idle expiry are different policies. |
| 26 | A JSON object is in Redis, but HGET key name fails. Why? | If SET stored JSON, the value is a Redis string. Native hash commands do not parse JSON properties. |
| 27 | RedisTemplate<String, Product> is declared, but stored bytes are not readable JSON. Why? | Java generics do not configure the serializer. Inspect the actual key/value serializers. |
| 28 | Direct PUT changes the DB, but annotation GET returns the old product. Why? | Separate cache entries, no cross-path invalidation. This is an intentional comparison boundary, not one shared coherent cache. |
| 29 | A product was deleted through one API but the other still returns it. Is the DB delete necessarily missing? | No. The other cache may still have the old value. Verify PostgreSQL and the other cache independently. |
| 30 | The annotation cache eventually corrects stale data, but the direct cache may not. Why? | Annotation entries have a TTL. Direct entries have no TTL and can remain stale while continually retained by LRU. |
| 31 | DB update succeeds, then Redis fails and HTTP is 500. Is the DB unchanged? | No. The DB write may already be committed. Retrying a create can duplicate work; the client must not equate a failed HTTP response with rollback. |
| 32 | After DB commit, the process crashes before @CachePut or @CacheEvict. What closes that gap? | The annotation does not. Durable invalidation/change events, versioning or other consistency mechanisms may be needed. |
| 33 | Reader loads old DB data; writer commits new data and refreshes cache; reader caches old data afterward. What went wrong? | A stale refill raced with the write. Individually atomic cache writes do not order the DB/cache workflow. |
| 34 | Thousands of callers miss one hot key. Does Lua LRU prevent thousands of DB reads? | No. Only the cache operations are inside Lua; the DB load is outside. Refill coalescing/coordination needs separate design. |
| 35 | Random nonexistent IDs hammer the DB. Would negative caching solve every request? | It helps repeated absent IDs, not an unlimited stream of unique misses. Combine appropriate validation, admission controls and source protection. |
| 36 | All annotation keys were populated together and expire together. What can happen? | A miss wave can overload the DB. Jitter or coordinated refresh/load protection can reduce it; this app has no TTL jitter. |
| 37 | Redis is down but PostgreSQL is healthy. Why do GETs fail before querying PostgreSQL? | The controller's rate limiter needs Redis first. There is no configured fail-open fallback, even before product caching is considered. |
| 38 | Should an outage fallback send every request straight to the DB? | Only if the DB can tolerate it and the policy permits it. Otherwise a cache outage can become a database outage. |
| 39 | A Redis lock expires while its owner is paused; the owner later DELs it. What is the risk? | A new owner's lock may be deleted. Use an atomic owner-token comparison on release; a lease also cannot prevent stale external writes without stronger coordination. |
| 40 | List-pop worker crashes after receiving work. Can it ACK later like RabbitMQ? | Ordinary pop already removed the item. Use a reliable processing pattern, Streams recovery, or a broker according to requirements. |
| 41 | XACK succeeds, yet XRANGE still shows the entry. Why? | ACK updates group pending state; stream retention/deletion is a separate operation. |
| 42 | A Pub/Sub subscriber reconnects and misses earlier invalidation messages. What should the cache design consider? | Pub/Sub has no offline replay. A retained change mechanism, version checks, or a bounded lifetime may be needed. |
| 43 | Redis commands are serialized, so why does Java need concurrency reasoning? | Separate client commands can interleave, and DB operations/Java computation occur outside Redis's execution boundary. |
| 44 | A timeout occurred after a Redis write. Can you conclude it was never applied? | No. A lost/late response can leave the outcome uncertain; retries of increments are not automatically idempotent. |
