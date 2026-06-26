
package edu.uth.listingservice.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary; 
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder; 
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMQConfig {

    // (Tất cả @Value cũ giữ nguyên)
    @Value("${app.rabbitmq.exchange}")
    private String listingEventsExchange;
    @Value("${app.rabbitmq.ai.exchange}")
    private String aiExchange;
    @Value("${app.rabbitmq.ai.queue}")
    private String aiRequestQueue;
    @Value("${app.rabbitmq.ai.routing-key}")
    private String aiRoutingKey; 
    @Value("${app.rabbitmq.user.exchange}") 
    private String userEventsExchange; // "ev.exchange"
    @Value("${app.rabbitmq.user.queue.listener}") 
    private String userEventsQueue; // "listing.user.sync.queue"
    @Value("${app.rabbitmq.product-events.exchange}")
    private String productEventsExchange;
    public static final String ASYNC_REQUEST_KEY = "listing.created.needs_user_data";
    public static final String ASYNC_RESPONSE_KEY = "user.info.found";
    public static final String ASYNC_RESPONSE_QUEUE = "user.info.response.queue";

    // (Tất cả Bean gốc cho USER exchange giữ nguyên)
    @Bean(name = "userTopicExchange")
    public TopicExchange userTopicExchange() {
        return new TopicExchange(userEventsExchange, true, false);
    }
    @Bean
    public Queue asyncUserDataResponseQueue() {
        return QueueBuilder.durable(ASYNC_RESPONSE_QUEUE).build();
    }
    @Bean
    public Binding bindAsyncUserDataResponse(Queue asyncUserDataResponseQueue, 
                                             @Qualifier("userTopicExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(asyncUserDataResponseQueue).to(userExchange).with(ASYNC_RESPONSE_KEY);
    }
    @Bean
    public Queue userEventsQueue() {
        return new Queue(userEventsQueue, true); 
    }
    @Bean
    public Binding userEventsBinding(Queue userEventsQueue,
                                     @Qualifier("userTopicExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(userEventsQueue).to(userExchange).with("user.#"); 
    }

    // ⭐️ ==========================================================
    // ⭐️ THÊM CÁC BEAN MỚI ĐỂ LẮNG NGHE ORDER PAID
    // ⭐️ ==========================================================
    @Value("${app.rabbitmq.order-events.queue}")
    private String orderPaidQueueName;

    @Value("${app.rabbitmq.order-events.routing-key}")
    private String orderPaidRoutingKey;

    @Bean
    public Queue orderPaidQueue() {
        // Tạo hàng đợi (Queue) riêng cho listing-service
        return new Queue(orderPaidQueueName, true); // durable = true
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, 
                                    @Qualifier("userTopicExchange") TopicExchange exchange) {
        // Bind vào exchange chung ("ev.exchange") với key "order.paid"
        return BindingBuilder
                .bind(orderPaidQueue)
                .to(exchange) // Dùng lại bean userTopicExchange (là "ev.exchange")
                .with(orderPaidRoutingKey);
    }
    // ⭐️ --- KẾT THÚC THÊM BEAN MỚI ---


    // (Tất cả các Bean gốc còn lại giữ nguyên)
     @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper());
    }

    @Bean
    @Primary 
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter); 
        return rabbitTemplate;
    }
 
    @Bean
    public TopicExchange listingEventsExchangeName() {
        return new TopicExchange(listingEventsExchange);
    }
    @Bean
    public TopicExchange aiExchangeName() { 
        return new TopicExchange(aiExchange);   
    }
    @Bean
    public Queue aiRequestQueueName() {
        return new Queue(aiRequestQueue, true); 
    }
    @Bean
    public Binding aiRequestBinding(Queue aiRequestQueueName, TopicExchange aiExchangeName) {
         return BindingBuilder
                .bind(aiRequestQueueName)
                .to(aiExchangeName)
                .with(aiRoutingKey);
    }
    @Bean
    public TopicExchange productEventsExchangeName() {
        return new TopicExchange(productEventsExchange);
    }
}


