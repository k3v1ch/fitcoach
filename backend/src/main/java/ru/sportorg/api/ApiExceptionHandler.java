package ru.sportorg.api;

import java.util.List;
import java.util.UUID;

import ru.sportorg.auth.InvalidRegistrationTokenException;
import ru.sportorg.auth.InvalidCredentialsException;
import ru.sportorg.auth.RegistrationRateLimitException;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException exception) {
        List<ApiError.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "INVALID_FIELDS", "Проверьте поля запроса.", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Некорректное тело запроса.", List.of());
    }

    @ExceptionHandler(InvalidRegistrationTokenException.class)
    ResponseEntity<ApiError> invalidRegistrationToken() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Ссылка недействительна или срок её действия истёк.", List.of());
    }

    @ExceptionHandler(RegistrationRateLimitException.class)
    ResponseEntity<ApiError> registrationRateLimit() {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Повторите запрос позже.", List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Неверные данные для входа.", List.of());
    }

    @ExceptionHandler(OrganizationRequestException.class)
    ResponseEntity<ApiError> invalidOrganizationRequest(OrganizationRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_FIELDS", exception.getMessage(), List.of());
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    ResponseEntity<ApiError> organizationNotFound() {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Организация или доступная запись не найдены.", List.of());
    }

    @ExceptionHandler(OrganizationPermissionException.class)
    ResponseEntity<ApiError> organizationPermissionDenied() {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Недостаточно прав для выполнения запроса.", List.of());
    }

    private ApiError.FieldError toFieldError(FieldError fieldError) {
        return new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           List<ApiError.FieldError> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, fieldErrors, UUID.randomUUID().toString()));
    }
}