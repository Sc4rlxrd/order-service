package com.scarlxrd.order_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .servers(buildServers(gatewayUrl))
                .info(buildInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents())
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto")
                        .url("https://github.com/Sc4rlxrd/order-service"));
    }

    private List<Server> buildServers(String gatewayUrl) {
        return List.of(
                new Server()
                        .url(gatewayUrl)
                        .description("Gateway")
        );
    }

    private Info buildInfo() {
        return new Info()
                .title("Order-Service API")
                .description("""
                        Serviço orquestrador do fluxo de pedidos do BookCommerce.
                        
                        **Endpoints disponíveis:**
                        - Criação de pedidos
                        
                        **Fluxo completo após criação:**
                        1. Publica `book.validate` para cada item → catalog-service valida
                        2. Consome `book.validated` → calcula total e atualiza status
                        3. Publica `payment.process` → payment-service processa
                        4. Consome `payment.result` → atualiza status final do pedido
                        
                        **Status possíveis do pedido:**
                        `CREATED` → `PENDING` → `VALIDATED` → `PAID` / `CANCELLED`
                        """)
                .version("v1")
                .contact(new Contact()
                        .name("Scarlxrd")
                        .url("https://github.com/Sc4rlxrd")
                        .email("contato@exemplo.com"));

    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}