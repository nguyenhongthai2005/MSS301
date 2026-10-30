package fu.de190299.reservation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reservationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DE190299ReservationService API")
                        .description("Reservation Service for MSS301 Practical Exam 2026")
                        .version("1.0.0"));
    }
}
