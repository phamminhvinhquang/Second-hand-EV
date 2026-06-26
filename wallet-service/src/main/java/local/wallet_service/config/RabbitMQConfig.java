package local.wallet_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ RabbitMQConfig cho wallet-service
 * - Lắng nghe các sự kiện từ transaction-service (wallet.deposit.success, order.paid, wallet.order.debit)
 * - Lắng nghe các sự kiện từ user-service (user.created, user.role.updated)
 * - Dùng TopicExchange "ev.exchange" để đồng bộ với các service khác
 */
@Configuration
public class RabbitMQConfig {

    // ======================= EXCHANGE CHÍNH =======================
    public static final String EXCHANGE_NAME = "ev.exchange";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // ======================= QUEUES =======================
    public static final String WALLET_DEPOSIT_QUEUE = "wallet.deposit.queue";
    public static final String ORDER_PAID_QUEUE = "wallet.order.paid.queue";
    public static final String ORDER_DEBIT_QUEUE = "wallet.order.debit.queue"; // 🆕 thêm queue mới
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_ROLE_UPDATED_QUEUE = "user.role.updated.queue";

    // ---------- Wallet Deposit Success ----------
    @Bean
    public Queue walletDepositQueue() {
        return QueueBuilder.durable(WALLET_DEPOSIT_QUEUE).build();
    }

    @Bean
    public Binding bindingWalletDeposit(Queue walletDepositQueue, TopicExchange exchange) {
        return BindingBuilder.bind(walletDepositQueue).to(exchange).with("wallet.deposit.success");
    }

    // ---------- Order Paid ----------
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderPaid(Queue orderPaidQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderPaidQueue).to(exchange).with("order.paid");
    }

    // ---------- 🆕 Order Debit ----------
    @Bean
    public Queue orderDebitQueue() {
        return QueueBuilder.durable(ORDER_DEBIT_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderDebit(Queue orderDebitQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderDebitQueue).to(exchange).with("wallet.order.debit");
    }

    // ---------- User Created ----------
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(USER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userCreatedQueue).to(exchange).with("user.created");
    }

    // ---------- User Role Updated ----------
    @Bean
    public Queue userRoleUpdatedQueue() {
        return QueueBuilder.durable(USER_ROLE_UPDATED_QUEUE).build();
    }

    @Bean
    public Binding bindingUserRoleUpdated(Queue userRoleUpdatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userRoleUpdatedQueue).to(exchange).with("user.role.updated");
    }

    // ======================= CONVERTER (JSON <-> Object) =======================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ======================= TEMPLATE (Publish nếu cần) =======================
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
