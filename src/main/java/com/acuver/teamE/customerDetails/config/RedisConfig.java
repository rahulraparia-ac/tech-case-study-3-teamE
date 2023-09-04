package com.acuver.teamE.customerDetails.config;

import com.acuver.teamE.customerDetails.entity.Customer;
import org.redisson.Redisson;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import org.redisson.api.MapOptions;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.MapWriter;
import org.redisson.config.Config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;


@Configuration
public class RedisConfig implements InitializingBean {

    private final String CACHE_NAME="Customer";

    private RedissonClient redissonClient;

    @Autowired
    private CustomerRepository customerRepository;

    @Bean
    public RMapCache<String, Customer> customerRMapCache() {
        final RMapCache<String, Customer> customerRMapCache = redissonClient.getMapCache(CACHE_NAME, MapOptions.<String, Customer>defaults()
                .writer(getMapWriter())
                .writeMode(MapOptions.WriteMode.WRITE_THROUGH));
        return customerRMapCache;
    }
    @Bean
    public RedissonClient redissonClient() {
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }

    private MapWriter<String, Customer> getMapWriter() {
        return new MapWriter<String, Customer>() {

            @Override
            public void write(final Map<String, Customer> map) {
                map.forEach( (k, v) -> {
                    customerRepository.save(v);
                });
            }

            @Override
            public void delete(Collection<String> keys) {
                keys.stream().forEach(e -> {
                    customerRepository.delete(customerRepository.findById(e).orElseThrow(() -> new NoSuchElementException("Resource Not Found")));
                });
            }
        };
    }


    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(300))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("keyValueCache",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(300)));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        this.redissonClient = Redisson.create(config);
    }

}
