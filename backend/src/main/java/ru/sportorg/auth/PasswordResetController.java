package ru.sportorg.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class PasswordResetController {

    private final PasswordResetService passwordResetService;

    PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/password-reset/request")
    ResponseEntity<RegistrationController.AcceptedResponse> requestReset(
            @Valid @RequestBody PasswordResetRequest request, HttpServletRequest servletRequest) {
        passwordResetService.requestReset(request, servletRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(new RegistrationController.AcceptedResponse(
                "Если аккаунт с таким адресом существует, письмо будет отправлено."));
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        passwordResetService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }
}