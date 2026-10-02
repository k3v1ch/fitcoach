package ru.sportorg.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    CurrentUser login(@Valid @RequestBody LoginRequest request,
                      HttpServletRequest servletRequest, HttpServletResponse response) {
        return authService.login(request, servletRequest, response);
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(Authentication authentication,
                                HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authentication, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    CurrentUser me(@AuthenticationPrincipal AuthenticatedUser user, HttpSession session) {
        return authService.currentUser(user, session);
    }
}