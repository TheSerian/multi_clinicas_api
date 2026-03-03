package com.multiclinicas.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Nome interno do esquema de segurança
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("MultiClínicas API")
                        .version("v1")
                        .description("API para gerenciamento de clínicas médicas multi-tenant")
                        .contact(new Contact()
                                .name("MultiClínicas")
                                .url("https://github.com/alexlsilva7/multi_clinicas_api"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                // 1. Aplica a exigência do token globalmente em todos os endpoints
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // 2. Configura como o Swagger deve montar o Header (Authorization: Bearer
                // <token>)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            // Não exige o header X-Clinic-ID obrigatório para endpoints de Gestão Global de
            // Clínicas
            String path = handlerMethod.getBeanType().getSimpleName();
            if (path.contains("Clinica")) {
                return operation;
            }

            Parameter clinicIdHeader = new Parameter()
                    .in("header")
                    .name("X-Clinic-ID")
                    .description(
                            "ID da clínica (tenant) para contexto multi-tenant. (Pode ser vazio para login de SUPER_ADMIN)")
                    .required(false) // false pois no login de SUPER_ADMIN ele não é enviado
                    .example("1")
                    .schema(new io.swagger.v3.oas.models.media.IntegerSchema());

            operation.addParametersItem(clinicIdHeader);
            return operation;
        };
    }
}
