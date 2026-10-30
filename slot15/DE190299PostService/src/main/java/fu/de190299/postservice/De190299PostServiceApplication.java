package fu.de190299.postservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class De190299PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(De190299PostServiceApplication.class, args);
    }
}
