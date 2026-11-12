package com.queuedesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The title and description shown at the top of the Swagger UI page (/swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queueDeskOpenApi() {
        return new OpenAPI().info(new Info()
                .title("QueueDesk API")
                .version("2.0")
                .description("Helpdesk ticketing: create, update and triage bug, feature and access tickets. "
                        + "Spring Boot + Spring Data JPA + MySQL."));
    }
}
