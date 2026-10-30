package fu.de190299.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class De190299ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(De190299ReservationServiceApplication.class, args);
    }
}
