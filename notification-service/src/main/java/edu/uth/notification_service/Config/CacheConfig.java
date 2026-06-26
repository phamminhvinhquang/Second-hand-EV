package edu.uth.notification_service.Config;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import edu.uth.notification_service.DTO.RestPage;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. Cấu hình Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Hỗ trợ Date/Time Java 8
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // --- [QUAN TRỌNG] ĐĂNG KÝ MODULE CHO PAGEIMPL ---
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PageImpl.class, new JsonDeserializer<PageImpl>() {
            @Override
            public PageImpl deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                // Bảo Jackson đọc JSON như là RestPage, nhưng trả về dưới dạng PageImpl
                return mapper.readValue(p, RestPage.class);
            }
        });
        objectMapper.registerModule(module);
        // -------------------------------------------------

        // 2. Kích hoạt Default Typing (Để lưu thông tin class vào Redis)
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );

        // 3. Cấu hình chung cho Redis Cache
        RedisCacheConfiguration jsonConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .entryTtl(Duration.ofMinutes(30)); // Mặc định 30 phút

        // 4. Cấu hình riêng cho từng loại Cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cache danh sách thông báo: 5 phút (ngắn vì dữ liệu thay đổi liên tục)
        cacheConfigurations.put("user_notifications", jsonConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Cache số lượng chưa đọc: 5 phút (sẽ bị xóa ngay khi có thông báo mới/đọc)
        cacheConfigurations.put("unread_count", jsonConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(jsonConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}