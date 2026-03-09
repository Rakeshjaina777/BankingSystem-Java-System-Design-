package com.rakesh.systemdesign.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ==================== SWAGGER / OpenAPI ====================
 *
 * WHAT:  Swagger auto-scans ALL your @RestController classes and generates:
 *          1. Interactive UI → you can TEST APIs directly from browser (like Postman!)
 *          2. JSON documentation → frontend developers can see all endpoints
 *
 * HOW IT WORKS:
 *   - springdoc library scans your controllers at startup
 *   - Finds all @GetMapping, @PostMapping, @PutMapping, @DeleteMapping
 *   - Reads @RequestBody, @PathVariable, @RequestParam annotations
 *   - Reads DTO classes and their validation annotations
 *   - Generates interactive documentation automatically!
 *
 * URLs (open in browser after starting the app):
 *
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │  Swagger UI (interactive):  http://localhost:8081/swagger-ui.html  │
 *   │  OpenAPI JSON (raw docs):   http://localhost:8081/v3/api-docs     │
 *   └─────────────────────────────────────────────────────────────────┘
 *
 *   Swagger UI lets you:
 *     - See all endpoints grouped by controller
 *     - See request/response body schemas
 *     - Click "Try it out" → type values → send request → see response
 *     - No Postman needed for testing!
 *
 * NestJS comparison:
 *   In NestJS you do:
 *     const config = new DocumentBuilder().setTitle('Banking API').build();
 *     SwaggerModule.setup('api', app, document);
 *   Same concept — different syntax.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking System API")
                        .description("Production-level Banking System — Signup, Login, Accounts, Transactions")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rakesh")
                                .email("rakesh@gmail.com")));
    }
}
