package learning.redis.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import java.util.List;
import learning.redis.model.Product;
import learning.redis.service.ProductService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/products")
public class ProductController {

    // 1. Load the script; Long.class maps its returned Redis integer (the counter) to a Java Long.
    private static final RedisScript<Long> RATE = RedisScript.of(new ClassPathResource("lua/rate-limit.lua"), Long.class);

    private final ProductService products;
    // 2. StringRedisTemplate provides APIs for interacting with Redis using string keys and values.
    // Injecting the template does not prove Redis is reachable; commands can fail later.
    // RedisTemplate<K, V> is the general form with configurable key/value serialization.
    private final StringRedisTemplate redis;

    public ProductController(ProductService products, StringRedisTemplate redis) {
        this.products = products;
        // Rate limiting and direct caching share the same configured template/Redis database.
        // Separate databases require separate connection-factory configurations, not merely two template objects.
        this.redis = redis;
    }

    //-----------------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product input) throws JsonProcessingException {
        return products.create(input.name());
    }

    //-----------------------------------------------------------------------------------

    @GetMapping("/{id}")
    public Product get(@PathVariable long id) throws JsonProcessingException {

        // Each selected Redis logical database has one keyspace shared by all value types.
        // Standalone Redis supports numbered logical databases; this app uses database 0 by default.
        // Colons namespace keys by convention; they do not create folders or separate databases.
        String key = "learning:crud:get-count"; // Shared across callers/IDs; append a user identifier for a per-user scope.

        // Option 1: Direct commands without Lua. INCR and EXPIRE are separate operations.
        // If the client fails after the first INCR but before EXPIRE, the counter can remain without a TTL.
        // Long count = redis.opsForValue().increment(key); // Step 1: redis.call('INCR', KEYS[1])
        // if (count != null && count == 1) { // Set a 60-second TTL only when the counter first becomes 1.
        //     redis.expire(key, Duration.ofSeconds(60)); // Step 2: redis.call('EXPIRE', KEYS[1], 60)
        // }

        // Option 2: Lua executes INCR and the conditional EXPIRE without interleaving on this Redis server.
        // Other clients cannot interleave commands between these steps.
        // Pass the script, a list of keys (KEYS), and optional arguments (ARGV) to execute().
        // Lua indexes KEYS and ARGV starting at 1; this call passes one key and no ARGV arguments.
        Long count = redis.execute(RATE, List.of(key));

        // Option 4: Native atomic commands/options; this pair is not a safe replacement for the Lua limiter.
        // redis.opsForValue().setIfAbsent(key, "0", Duration.ofSeconds(60)); // SET key 0 NX EX 60.
        // Long count = redis.opsForValue().increment(key); // INCR preserves an existing TTL.
        // Each command is atomic, but the pair is not: the key can expire between SET and INCR, and INCR can then recreate it without a TTL.
        // SET NX EX alone can acquire a lease but cannot also increment a counter.
        // Use Lua, WATCH with a retried transaction, or a Redis Function for atomic conditional expiry.

        // The script could also return an allowed/denied result; the controller would still map denial to HTTP 429.
        if (count > 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Only 5 GETs per 60 seconds");
        return products.get(id);
    }

    //-----------------------------------------------------------------------------------

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @Valid @RequestBody Product input) throws JsonProcessingException {
        return products.update(id, input.name());
    }

    //-----------------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { products.delete(id); }
}

// Redis execution mental model:
// Separate redis.opsForValue().increment(key) calls execute as separate atomic INCR commands.
// Separate redis.execute(RedisScript, ...) calls execute as separate scripts on the same Redis server.
// Between separate direct commands, other clients can run commands; within a Lua script, they cannot.
// This resembles grouping SQL statements in a transaction, but Redis does not provide SQL-style rollback.
// An error in a script or during EXEC does not undo commands that have already succeeded.


// Benefits of Lua scripts:
// 1. Atomic execution: combine a decision and its commands without other clients interleaving.
// 2. Fewer network round trips: send one script invocation instead of separate command requests.
// 3. Reusable logic across application instances: package the same script with each application.
//    Changing a bundled script still requires deploying the updated resource to those instances;
//    it is not automatically a centrally managed, persistent function.


// | Option                         | API / mechanism                                   | Pros                                                  | Cons                                                                    | When to use                                   |
// |--------------------------------|---------------------------------------------------|-------------------------------------------------------|-------------------------------------------------------------------------|-----------------------------------------------|
// | Direct commands without Lua    | redis.opsForValue(), opsForHash(), etc.           | Simple Java APIs, easy debugging                      | Separate commands can interleave, extra round trips                     | Ordinary reads/writes, independent operations |
// | Lua scripts                    | redis.execute(RedisScript, keys, args...)         | Atomic conditional logic, fewer round trips           | Extra language to maintain, long scripts block commands                 | Rate limiting, compare-and-delete, cache LRU  |
// | Redis transactions             | MULTI / EXEC through a Spring SessionCallback     | Grouped execution without interleaving, Java APIs     | No rollback of execution errors, conditional updates need WATCH/retries | A known group of commands executed together   |
// | Native atomic commands/options | SET key value NX EX seconds, INCR                 | Atomic per command, no custom server-side logic       | Limited to built-in behavior, combining commands is not atomic          | Leases with SET NX EX, counters with INCR     |
// | Redis Functions                | Load a function library with FUNCTION LOAD; FCALL | Reusable server-side logic, persisted with Redis data | Redis 7+ required, library deployment/versioning needed                 | Shared server-side logic managed as a library |
// SET NX EX and INCR are direct-command examples, not a separate execution model.
// SET NX EX + INCR are still two commands; putting them next to each other does not make them atomic together.
// MULTI/EXEC must use the same connection; SessionCallback keeps Spring operations on that connection.
// These two-key cache scripts target standalone Redis; Redis Cluster would require keys in one hash slot.
// Pipelining is another way to reduce round trips for direct commands; it does not provide atomicity.


// | Interview question                                                 | Expected understanding                                                                                                  |
// |--------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
// | Are two INCR calls atomic?                                         | Each INCR is atomic. Two calls are two separate operations.                                                             |
// | Can two Lua executions run concurrently on the same Redis server?  | They execute sequentially on that server; commands from other clients don't interleave within a running script.         |
// | Does MULTI/EXEC provide rollback?                                  | No. It groups execution but does not provide SQL-style rollback for command execution errors.                           |
// | Why use Lua instead of MULTI/EXEC?                                 | Lua handles server-side conditions and related operations atomically without client-side read/decide/write round trips. |
// | Does SET NX EX plus INCR become atomic?                            | No. They are separate commands unless combined through an appropriate atomic mechanism.                                 |
// | What happens if a request fails after INCR but before EXPIRE?      | The counter may be left without a TTL.                                                                                  |
// | Why use Redis for rate limiting?                                   | Shared counters and expiry can enforce limits across application instances.                                             |
// | Does Redis atomicity mean a distributed transaction with rollback? | No. Atomic execution, rollback, and durability are different concepts.                                                  |
