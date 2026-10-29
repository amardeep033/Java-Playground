package learning.redis.controller;

import jakarta.validation.Valid;
import java.util.List;
import learning.redis.model.Product;
import learning.redis.service.CacheDemoService;

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
@RequestMapping("/cache-demo/products")
public class CacheDemoController {
    // Same Lua limiter as ProductController; each API has its own counter shared across callers and app instances.
    private static final RedisScript<Long> RATE = RedisScript.of(new ClassPathResource("lua/rate-limit.lua"), Long.class);
    private final CacheDemoService products;
    private final StringRedisTemplate redis;

    public CacheDemoController(CacheDemoService products, StringRedisTemplate redis) {
        this.products = products;
        this.redis = redis;
    }

    //-----------------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product input) {
        return products.create(input.name());
    }

    //-----------------------------------------------------------------------------------

    @GetMapping("/{id}")
    public Product get(@PathVariable long id) {

        // Spring Cache annotations handle product caching, not atomic counter increments.
        // @Cacheable skips the method body on a hit, so an increment inside that body would not run.
        // Reading a count and writing count + 1 with @CachePut is not atomic: concurrent requests
        // can read the same count and overwrite each other's increments. Rewriting can also reset the TTL.
        // Use Lua here too: INCR and the first-request EXPIRE execute without other commands interleaving.
        // The controller checks every GET before the cached service call, then maps count > 5 to HTTP 429.
        String key = "learning:cache-demo:get-count";
        Long count = redis.execute(RATE, List.of(key));
        if (count > 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Only 5 GETs per 60 seconds");

        return products.get(id);
    }

    //-----------------------------------------------------------------------------------

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @Valid @RequestBody Product input) {
        return products.update(id, input.name());
    }

    //-----------------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { products.delete(id); }
}
