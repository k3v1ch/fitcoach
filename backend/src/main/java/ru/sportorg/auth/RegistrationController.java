package ru.sportorg.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class RegistrationController {

    private final RegistrationService registrationService;

    RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    ResponseEntity<AcceptedResponse> register(@Valid @RequestBody RegistrationRequest request,
                                               HttpServletRequest servletRequest) {
        registrationService.requestRegistration(request, servletRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(new AcceptedResponse("Если адрес можно зарегистрировать, письмо будет отправлено."));
    }

    @PostMapping("/register/confirm")
    ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        registrationService.confirmRegistration(request);
        return ResponseEntity.noContent().build();
    }

    record AcceptedResponse(String message) {
    }
}