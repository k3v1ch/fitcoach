package ru.sportorg.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public
class OpenApiConfig {

    private static final String SESSION_SCHEME = "sessionCookie";
    private static final String CSRF_SCHEME = "csrfHeader";

    @Bean
    OpenAPI sportsOrganizationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sports Organization API")
                        .version("v1")
                        .description("API for sports organization management. "
                                + "Authenticated requests use the JSESSIONID cookie. "
                                + "State-changing requests also require the X-CSRF-TOKEN header."))
                .components(new Components()
                        .addSecuritySchemes(SESSION_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session cookie created when requesting a CSRF token; authenticated requests use it for the login session."))
                        .addSecuritySchemes(CSRF_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN")
                                .description("Token returned by GET /api/v1/auth/csrf.")));
    }

    @Bean
    OpenApiCustomizer apiOperationDocumentation() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    operation.setTags(List.of(tagFor(path)));
                    applySecurity(path, method, operation);
                }));
    }

    private static void applySecurity(String path, PathItem.HttpMethod method, Operation operation) {
        boolean requiresSession = !List.of(
                "/api/v1/auth/csrf",
                "/api/v1/auth/register",
                "/api/v1/auth/register/confirm",
                "/api/v1/auth/login",
                "/api/v1/auth/password-reset/request",
                "/api/v1/auth/password-reset/confirm").contains(path);
        boolean requiresCsrf = method == PathItem.HttpMethod.POST
                || method == PathItem.HttpMethod.PUT
                || method == PathItem.HttpMethod.PATCH
                || method == PathItem.HttpMethod.DELETE;

        SecurityRequirement requirement = new SecurityRequirement();
        if (requiresSession || requiresCsrf) {
            requirement.addList(SESSION_SCHEME);
        }
        if (requiresCsrf) {
            requirement.addList(CSRF_SCHEME);
        }
        operation.setSecurity(requirement.isEmpty() ? List.of() : List.of(requirement));
    }

    private static String tagFor(String path) {
        if (path.startsWith("/api/v1/auth/") || path.equals("/api/v1/me")) {
            return "Authentication";
        }
        if (path.contains("/results") || path.endsWith("/standards") || path.endsWith("/ranks")) {
            return "Progress";
        }
        if (path.contains("/announcements")) {
            return "Announcements";
        }
        if (path.contains("/athletes")) {
            return "Athletes";
        }
        if (path.contains("/dashboard")) {
            return "Dashboard";
        }
        if (path.contains("/dictionaries")) {
            return "Dictionaries";
        }
        if (path.contains("/documents") || path.contains("/files")) {
            return "Documents and files";
        }
        if (path.contains("/events")) {
            return "Events";
        }
        if (path.contains("/charges") || path.contains("/payments") || path.contains("/finance")) {
            return "Finance";
        }
        if (path.contains("/sections") || path.contains("/groups")) {
            return "Groups";
        }
        if (path.contains("/reports")) {
            return "Reports";
        }
        if (path.contains("/trainings")) {
            return "Trainings";
        }
        return "Organizations";
    }
}