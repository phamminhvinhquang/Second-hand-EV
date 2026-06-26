package edu.uth.example.review_service.Config;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import edu.uth.example.review_service.DTO.RestPage;

@Configuration
@EnableCaching
public class CacheConfig {

    // Deserializer tùy chỉnh cho PageImpl
    public static class PageImplDeserializer extends JsonDeserializer<PageImpl> {
        @Override
        public PageImpl deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(p, RestPage.class);
        }
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PageImpl.class, new PageImplDeserializer());
        objectMapper.registerModule(module);

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        RedisCacheConfiguration jsonCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        // --- CẤU HÌNH TTL CHO REVIEW SERVICE ---
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 1. Thống kê review (Rating/Count) - Cache lâu (1 ngày), xóa khi có review mới
        cacheConfigurations.put("userReviewStats", jsonCacheConfig.entryTtl(Duration.ofDays(1)));
        
        // 2. Danh sách review trên Profile - Cache 30 phút
        cacheConfigurations.put("userReviewsPage", jsonCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 3. Danh sách việc cần làm (Pending Tasks) - Cache ngắn (5 phút) vì thay đổi nhanh
        cacheConfigurations.put("reviewTasks", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 4. Đếm số lượng pending - Cache ngắn (5 phút)
        cacheConfigurations.put("reviewTasksCount", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));

        // 5. Lịch sử review đã làm - Cache 30 phút
        cacheConfigurations.put("reviewHistory", jsonCacheConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(jsonCacheConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}