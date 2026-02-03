package com.cryptotax.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cryptoTaxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crypto Tax Reporter API")
                        .description("Capital gains calculator using FIFO cost basis for BTC, ETH, SOL")
                        .version("1.0.0")
                        .contact(new Contact().name("Crypto Tax Reporter")));
    }
}
