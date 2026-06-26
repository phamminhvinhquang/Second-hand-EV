package edu.uth.chat_service.Config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. Cấu hình Jackson để xử lý Date và List object
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Hỗ trợ Java 8 Date/Time
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Quan trọng: Lưu thông tin class type để khi đọc JSON lên không bị lỗi
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );

        // 2. Cấu hình Default
        RedisCacheConfiguration jsonConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .entryTtl(Duration.ofMinutes(30)); // Mặc định 30 phút

        // 3. Cấu hình riêng cho từng cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Sidebar: Query nặng, cache 10p (sẽ bị xóa khi có tin mới)
        cacheConfigurations.put("chat_sidebar", jsonConfig.entryTtl(Duration.ofMinutes(10)));
        
        // History: Cache cuộc trò chuyện
        cacheConfigurations.put("chat_history", jsonConfig.entryTtl(Duration.ofMinutes(30)));
        
        // User Info: Cache thông tin user
        cacheConfigurations.put("chat_users", jsonConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Block Status: Cache trạng thái chặn
        cacheConfigurations.put("block_status", jsonConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(jsonConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}