package io.github.ita07.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.heroku.url}")
    private String herokuUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(herokuUrl).description("Production server"),
                        new Server().url("http://localhost:8080").description("Local server")
                ))
                .info(new Info()
                        .title("TOTP Generator API")
                        .description("A secure Time-based One-Time Password (TOTP) generator service compatible with Google Authenticator and other TOTP applications.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .url("https://github.com/ita07/TOTPGenerator")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}