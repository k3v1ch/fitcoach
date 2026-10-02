package ru.sportorg.finance;

import java.util.UUID;
import java.time.LocalDate;

import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
class FinanceController {
    private final FinanceService service;

    FinanceController(FinanceService service) {
        this.service = service;
    }

    @PostMapping("/charges")
    Charge charge(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestBody ChargeWrite write) {
        return service.createCharge(actor, organizationId, write);
    }

    @GetMapping("/charges/{chargeId}")
    Charge getCharge(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable UUID chargeId) {
        return service.getCharge(actor, organizationId, chargeId);
    }

    @PostMapping("/payments")
    Payment payment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody PaymentWrite write) {
        return service.pay(actor, organizationId, write, key);
    }

    @GetMapping("/payments")
    PaymentPage payments(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID athleteId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID chargeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.payments(actor, organizationId, paymentId, athleteId, sectionId, chargeId, from, to, status, page, size);
    }

    @GetMapping("/finance/summary")
    FinanceSummary summary(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return service.summary(actor, organizationId, from, to);
    }
}