package com.hostel.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Smart Hostel Management & Student Community Platform API",
                version = "1.0",
                description = "Production-grade hostel management system with AI features, JWT authentication, and role-based access control",
                contact = @Contact(
                        name = "Developer",
                        email = "contact@hostel.com"
                ),
                license = @License(
                        name = "MIT",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://api.hostel.com").description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("BearerAuth")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer globalTags() {
        return openApi -> openApi.getTags().addAll(List.of(
                new Tag().name("Authentication").description("Authentication endpoints"),
                new Tag().name("Admin").description("Admin-only endpoints"),
                new Tag().name("Student").description("Student endpoints"),
                new Tag().name("Warden").description("Warden endpoints"),
                new Tag().name("Rooms").description("Room management endpoints"),
                new Tag().name("Leaves").description("Leave request endpoints"),
                new Tag().name("Complaints").description("Complaint management endpoints"),
                new Tag().name("Notices").description("Notice board endpoints"),
                new Tag().name("Marketplace").description("Marketplace endpoints"),
                new Tag().name("Lost & Found").description("Lost and found endpoints"),
                new Tag().name("Mess Feedback").description("Mess feedback endpoints"),
                new Tag().name("AI Features").description("AI-powered endpoints")
        ));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-api")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi studentApi() {
        return GroupedOpenApi.builder()
                .group("student-api")
                .pathsToMatch("/api/student/**")
                .build();
    }

    @Bean
    public GroupedOpenApi wardenApi() {
        return GroupedOpenApi.builder()
                .group("warden-api")
                .pathsToMatch("/api/wardens/**")
                .build();
    }

    @Bean
    public GroupedOpenApi roomApi() {
        return GroupedOpenApi.builder()
                .group("room-api")
                .pathsToMatch("/api/rooms/**")
                .build();
    }

    @Bean
    public GroupedOpenApi leaveApi() {
        return GroupedOpenApi.builder()
                .group("leave-api")
                .pathsToMatch("/api/leaves/**")
                .build();
    }

    @Bean
    public GroupedOpenApi complaintApi() {
        return GroupedOpenApi.builder()
                .group("complaint-api")
                .pathsToMatch("/api/complaints/**")
                .build();
    }

    @Bean
    public GroupedOpenApi noticeApi() {
        return GroupedOpenApi.builder()
                .group("notice-api")
                .pathsToMatch("/api/notices/**")
                .build();
    }

    @Bean
    public GroupedOpenApi marketplaceApi() {
        return GroupedOpenApi.builder()
                .group("marketplace-api")
                .pathsToMatch("/api/marketplace/**")
                .build();
    }

    @Bean
    public GroupedOpenApi lostFoundApi() {
        return GroupedOpenApi.builder()
                .group("lost-found-api")
                .pathsToMatch("/api/lost-found/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messFeedbackApi() {
        return GroupedOpenApi.builder()
                .group("mess-feedback-api")
                .pathsToMatch("/api/mess-feedback/**")
                .build();
    }

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("ai-api")
                .pathsToMatch("/api/ai/**")
                .build();
    }
}
