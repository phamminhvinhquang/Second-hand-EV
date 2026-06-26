package edu.uth.listingservice.Config;

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

import edu.uth.listingservice.DTO.RestPage;



@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Đây là class Deserializer tùy chỉnh.
     * Nó nói với Jackson: "Khi nào được yêu cầu tạo PageImpl,
     * hãy đọc JSON đó NHƯ THỂ nó là một RestPage".
     */
    public static class PageImplDeserializer extends JsonDeserializer<PageImpl> {
        @Override
        public PageImpl deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            
            // Lấy ObjectMapper từ context
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
                
            // Đọc JSON, nhưng BẢO JACKSON hãy đọc nó như là một 'RestPage'
            // Vì RestPage có @JsonCreator, Jackson sẽ biết cách làm
            // Và vì RestPage extends PageImpl, kết quả trả về
            // vẫn là một PageImpl, thỏa mãn kiểu trả về.
            return mapper.readValue(p, RestPage.class);
        }
    }


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // ===  TẠO OBJECT MAPPER ===
        ObjectMapper objectMapper = new ObjectMapper();

        // === (GIẢI PHÁP TÙY CHỈNH) ===
        
        SimpleModule module = new SimpleModule();
        // Đăng ký Deserializer tùy chỉnh của chúng ta
        module.addDeserializer(PageImpl.class, new PageImplDeserializer());
        objectMapper.registerModule(module);

        // =======================================================
        
        // ===  KÍCH HOẠT DEFAULT TYPING (BẮT BUỘC) ===
        // Để sửa lỗi LinkedHashMap và lưu trữ @class
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class) // Cho phép mọi kiểu
                .build();
        
        objectMapper.activateDefaultTyping(
            ptv, 
            ObjectMapper.DefaultTyping.NON_FINAL, 
            JsonTypeInfo.As.PROPERTY
        );
        // ====================================================

        // ===  BỎ QUA CÁC THUỘC TÍNH KHÔNG BIẾT (GIỮ NGUYÊN) ===
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // ===  DÙNG MAPPER TÙY CHỈNH CHO SERIALIZER (GIỮ NGUYÊN) ===
        RedisCacheConfiguration jsonCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper) 
                ));

        // --- Cấu hình TTL mặc định (GIỮ NGUYÊN) ---
        RedisCacheConfiguration defaultConfig = jsonCacheConfig
                .entryTtl(Duration.ofMinutes(10)); 

        // ---Tạo Map chứa các cấu hình TTL riêng biệt (GIỮ NGUYÊN) ---
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("productConditions", jsonCacheConfig.entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put("activeListings", jsonCacheConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("productDetails", jsonCacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("aiSuggestions", jsonCacheConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("userListings", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("userListingPage", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("adminListings", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("adminSearchListings", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("relatedListings", jsonCacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("productSpecs", jsonCacheConfig.entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put("productImages", jsonCacheConfig.entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put("productCondition", jsonCacheConfig.entryTtl(Duration.ofDays(1))); 
        cacheConfigurations.put("productImage", jsonCacheConfig.entryTtl(Duration.ofDays(1))); 
        cacheConfigurations.put("users", jsonCacheConfig.entryTtl(Duration.ofMinutes(5)));
        // --- Xây dựng CacheManager ---
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) 
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    
}