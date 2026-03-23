package com.example.elephantfinancelab_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  public static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Elephant Finance Lab API")
                .description("elephantfinancelab 백엔드 API 문서")
                .version("v0.0.1"))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Access Token (Authorization: Bearer <token>)")));
  }
}
