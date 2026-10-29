package learning.redis.config;

import java.time.Duration;
import learning.redis.model.Product;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

// Configures Spring Cache backed by Redis; it does not configure RedisTemplate or ReactiveRedisTemplate.
// Spring Cache can use other providers, such as local Caffeine, by selecting another CacheManager.
@Configuration
@EnableCaching // Enables Spring's interception of @Cacheable, @CachePut, and @CacheEvict.
public class RedisCacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL for entries managed by this CacheManager; not a Redis-server-wide TTL.
                // Reject null cache values; the service also excludes nulls through unless.
                .disableCachingNullValues()
                // Cache values use Product JSON; the direct StringRedisTemplate keeps its own serializers.
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(Product.class)));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }
}

// RedisCacheManager has no built-in maximum-three-products LRU; the direct API implements that separately.
