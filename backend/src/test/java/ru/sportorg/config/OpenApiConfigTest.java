package ru.sportorg.config;

import java.util.List;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void documentsSessionAndCsrfRequirementsForApiOperations() {
        var configuration = new OpenApiConfig();
        var openApi = configuration.sportsOrganizationOpenApi();
        var register = new Operation();
        var csrf = new Operation();
        var protectedMutation = new Operation();
        openApi.setPaths(new Paths()
            .addPathItem("/api/v1/auth/csrf", new PathItem().get(csrf))
                .addPathItem("/api/v1/auth/register", new PathItem().post(register))
                .addPathItem("/api/v1/organizations/{organizationId}/groups", new PathItem().post(protectedMutation)));

        OpenApiCustomizer customizer = configuration.apiOperationDocumentation();
        customizer.customise(openApi);

        assertEquals("Sports Organization API", openApi.getInfo().getTitle());
        assertEquals("JSESSIONID", openApi.getComponents().getSecuritySchemes().get("sessionCookie").getName());
        assertEquals(List.of("Authentication"), register.getTags());
        assertTrue(register.getSecurity().get(0).containsKey("csrfHeader"));
        assertTrue(register.getSecurity().get(0).containsKey("sessionCookie"));
        assertFalse(csrf.getSecurity() != null && !csrf.getSecurity().isEmpty());
        assertEquals(List.of("Groups"), protectedMutation.getTags());
        assertTrue(protectedMutation.getSecurity().get(0).containsKey("sessionCookie"));
        assertTrue(protectedMutation.getSecurity().get(0).containsKey("csrfHeader"));
    }
}