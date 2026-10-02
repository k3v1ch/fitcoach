package ru.sportorg.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.core.userdetails.UserDetailsService;

import ru.sportorg.config.SecurityConfig;
import ru.sportorg.config.PasswordConfig;
import ru.sportorg.api.ApiSecurityErrorHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({CsrfController.class, RegistrationController.class})
@Import({SecurityConfig.class, PasswordConfig.class, ApiSecurityErrorHandler.class,
    AbsoluteSessionExpiryFilter.class})
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void returnsSessionBoundTokenAndHeaderName() throws Exception {
        var result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andReturn();

        var session = (MockHttpSession) result.getRequest().getSession(false);
        Assertions.assertNotNull(session);
    }

    @Test
    void registrationEndpointIsPublicButStillRequiresCsrf() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                .post("/api/v1/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"person@example.org\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders
                .post("/api/v1/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"person@example.org\"}"))
            .andExpect(status().isAccepted());

        verify(registrationService).requestRegistration(
            ArgumentMatchers.any(RegistrationRequest.class),
            ArgumentMatchers.anyString()
        );
    }
}