package net.techcrunch.outflowPayment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI outflowPaymentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Outflow Payment API")
                        .version("1.0.0")
                        .description("Outflow transfer, refund, settlement, diagnostics, failed-message, and reconciliation APIs."))
                .servers(List.of(new Server().url("http://localhost:9091").description("Local outflowpayment")));
    }
}
