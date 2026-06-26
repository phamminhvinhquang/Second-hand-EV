package local.contract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "local.contract")
@EnableFeignClients(basePackages = "local.contract.client") // 👈 thêm dòng này
public class ContractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractServiceApplication.class, args);
    }
}
