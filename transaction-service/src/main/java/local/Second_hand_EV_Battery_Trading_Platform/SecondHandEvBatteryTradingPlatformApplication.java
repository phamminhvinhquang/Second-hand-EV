package local.Second_hand_EV_Battery_Trading_Platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "local.Second_hand_EV_Battery_Trading_Platform") // Quét toàn bộ Bean (Service, Controller, Config)
@EnableJpaRepositories(basePackages = "local.Second_hand_EV_Battery_Trading_Platform.repository") // Quét Repository
@EntityScan(basePackages = "local.Second_hand_EV_Battery_Trading_Platform.entity") // Quét Entity
public class SecondHandEvBatteryTradingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecondHandEvBatteryTradingPlatformApplication.class, args);
        System.out.println("🚀 Server is running at: http://localhost:8083");
        System.out.println("✅ Connected successfully to MySQL: ev_trading_payment");
    }
}
