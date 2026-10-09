package ru.sportorg.finance;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationConflictException;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class FinanceService {
    private final FinanceRepository repository;
    FinanceService(FinanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    Charge createCharge(AuthenticatedUser actor, UUID org, ChargeWrite write) {
        requireTrainer(actor, org, "charges.write");
        if (write == null
                || write.athleteId() == null
                || write.sectionId() == null
                || blank(write.title())
                || write.amount() == null
                || write.amount().signum() <= 0
                || write.dueOn() == null
                || !ListValues.TYPES.contains(write.type())
                || !repository.athleteExists(org, write.athleteId())) {
            throw new OrganizationRequestException("Некорректное начисление.");
        }
        if ("SUBSCRIPTION".equals(write.type()) && (write.periodFrom() == null || write.periodTo() == null)) {
            throw new OrganizationRequestException("Для SUBSCRIPTION нужен период.");
        }
        return repository.insertCharge(org, write, actor.userId());
    }

    Charge getCharge(AuthenticatedUser actor, UUID org, UUID chargeId) {
        Access access = require(actor, org, "charges.read");
        Charge charge = repository.charge(org, chargeId).orElseThrow(OrganizationNotFoundException::new);
        if (access.self && !repository.athleteVisible(org, charge.athleteId(), actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        return charge;
    }

    @Transactional
    Payment pay(AuthenticatedUser actor, UUID org, PaymentWrite write, String key) {
        requireTrainer(actor, org, "payments.write");
        if (key == null || key.isBlank()) {
            throw new OrganizationRequestException("Idempotency-Key обязателен.");
        }
        Payment previous = repository.byKey(org, key).orElse(null);
        if (previous != null) {
            return previous;
        }
        Charge charge = repository.lockCharge(org, write.chargeId())
                .orElseThrow(OrganizationNotFoundException::new);
        if (!"ACTIVE".equals(charge.status())
                || write.amount() == null
                || write.amount().signum() <= 0
                || write.amount().compareTo(charge.remainingAmount()) > 0
                || write.paidOn() == null
                || write.paidOn().isAfter(LocalDate.now())
                || !ListValues.METHODS.contains(write.method())) {
            throw new OrganizationRequestException("Платёж превышает остаток или содержит недопустимые поля.");
        }
        return repository.insertPayment(org, charge, write, actor.userId(), key);
    }

    PaymentPage payments(
            AuthenticatedUser actor,
            UUID org,
            UUID paymentId,
            UUID athleteId,
            UUID sectionId,
            UUID chargeId,
            LocalDate from,
            LocalDate to,
            String status,
            int page,
            int size) {
        Access access = require(actor, org, "payments.read");
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new OrganizationRequestException("from не может быть позже to.");
        }
        if (status != null && !ListValues.PAYMENT_STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус платежа.");
        }
        UUID scopeUserId = access.self ? actor.userId() : null;
        long total = repository.countPayments(org, paymentId, athleteId, sectionId, chargeId, from, to, status, scopeUserId);
        return new PaymentPage(
                repository.payments(org, paymentId, athleteId, sectionId, chargeId, from, to, status, scopeUserId, size, page * size),
                page,
                size,
                total,
                (int) Math.ceil((double) total / size));
    }

    ChargePage charges(
            AuthenticatedUser actor,
            UUID org,
            String q,
            UUID athleteId,
            UUID sectionId,
            String type,
            UUID eventId,
            String paymentStatus,
            Boolean overdue,
            String status,
            LocalDate dueFrom,
            LocalDate dueTo,
            int page,
            int size) {
        Access access = require(actor, org, "charges.read");
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
        if (type != null && !ListValues.TYPES.contains(type)) {
            throw new OrganizationRequestException("Недопустимый тип начисления.");
        }
        if (paymentStatus != null && !ListValues.PAYMENT_STATES.contains(paymentStatus)) {
            throw new OrganizationRequestException("Недопустимый статус оплаты.");
        }
        if (status != null && !ListValues.CHARGE_STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус начисления.");
        }
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new OrganizationRequestException("dueFrom не может быть позже dueTo.");
        }
        String query = q == null || q.isBlank() ? null : q.trim();
        UUID scopeUserId = access.self ? actor.userId() : null;
        long total = repository.countCharges(org, query, athleteId, sectionId, type, eventId, paymentStatus, overdue, status, dueFrom, dueTo, scopeUserId);
        return new ChargePage(
                repository.charges(org, query, athleteId, sectionId, type, eventId, paymentStatus, overdue, status, dueFrom, dueTo, scopeUserId, size, page * size),
                page,
                size,
                total,
                (int) Math.ceil((double) total / size));
    }

    @Transactional
    Charge updateCharge(AuthenticatedUser actor, UUID org, UUID chargeId, ChargePatch patch) {
        requireTrainer(actor, org, "charges.write");
        if (patch == null || patch.isEmpty()) {
            throw new OrganizationRequestException("Передайте хотя бы одно поле.");
        }
        Charge charge = repository.lockCharge(org, chargeId).orElseThrow(OrganizationNotFoundException::new);
        if (!"ACTIVE".equals(charge.status())) {
            throw new OrganizationConflictException("Отменённое начисление не редактируется.");
        }
        if (patch.has("status")) {
            if (!"CANCELLED".equals(patch.status())) {
                throw new OrganizationRequestException("status можно передать только со значением CANCELLED.");
            }
            if (blank(patch.cancelReason())) {
                throw new OrganizationRequestException("Для отмены укажите причину.");
            }
            if (patch.has("title") || patch.has("amount") || patch.has("dueOn") || patch.has("comment")) {
                throw new OrganizationRequestException("Отмена не совмещается с изменением полей начисления.");
            }
            if (charge.paidAmount().signum() > 0) {
                throw new OrganizationConflictException("По начислению есть проведённые платежи: сначала аннулируйте их.");
            }
            String reason = patch.cancelReason().trim();
            repository.cancelCharge(org, chargeId, reason, actor.userId());
            repository.recordActivity(org, actor.userId(), "CHARGE_CANCELLED", "CHARGE", chargeId,
                    "Отменено начисление «" + charge.title() + "»: " + reason);
        } else {
            if (patch.has("cancelReason")) {
                throw new OrganizationRequestException("cancelReason передаётся только вместе со status = CANCELLED.");
            }
            if (patch.has("title") && blank(patch.title())) {
                throw new OrganizationRequestException("Название начисления не может быть пустым.");
            }
            if (patch.has("amount") && (patch.amount() == null
                    || patch.amount().signum() <= 0
                    || patch.amount().stripTrailingZeros().scale() > 2
                    || patch.amount().compareTo(charge.paidAmount()) < 0)) {
                throw new OrganizationRequestException("Сумма должна быть больше нуля, с точностью до копеек и не меньше уже оплаченного.");
            }
            if (patch.has("dueOn") && patch.dueOn() == null) {
                throw new OrganizationRequestException("Срок оплаты не может быть пустым.");
            }
            repository.updateCharge(org, chargeId, patch, actor.userId());
            repository.recordActivity(org, actor.userId(), "CHARGE_UPDATED", "CHARGE", chargeId,
                    "Изменено начисление «" + charge.title() + "»");
        }
        return repository.charge(org, chargeId).orElseThrow(OrganizationNotFoundException::new);
    }

    @Transactional
    PaymentResult voidPayment(AuthenticatedUser actor, UUID org, UUID paymentId, PaymentVoid request) {
        requireTrainer(actor, org, "payments.write");
        if (request == null || blank(request.reason())) {
            throw new OrganizationRequestException("Укажите причину аннулирования.");
        }
        Payment payment = repository.lockPayment(org, paymentId).orElseThrow(OrganizationNotFoundException::new);
        if (!"ACTIVE".equals(payment.status())) {
            throw new OrganizationConflictException("Платёж уже аннулирован.");
        }
        String reason = request.reason().trim();
        repository.voidPayment(org, paymentId, reason, actor.userId());
        repository.recordActivity(org, actor.userId(), "PAYMENT_VOIDED", "PAYMENT", paymentId,
                "Аннулирован платёж " + payment.amount().toPlainString() + " ₽: " + reason);
        return new PaymentResult(
                repository.payment(org, paymentId).orElseThrow(OrganizationNotFoundException::new),
                repository.charge(org, payment.chargeId()).orElseThrow(OrganizationNotFoundException::new));
    }

    FinanceSummary summary(AuthenticatedUser actor, UUID org, LocalDate from, LocalDate to) {
        require(actor, org, "finance.read");
        if (from == null || to == null || from.isAfter(to)) {
            throw new OrganizationRequestException("Укажите корректный период.");
        }
        return repository.summary(org, from, to);
    }

    private void requireTrainer(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null
                || !repository.organizationExists(org)
                || !java.util.Optional.ofNullable(repository.membership(actor.userId(), org))
                .filter(membership -> membership.permission(permission) && membership.role("TRAINER"))
                .isPresent()) {
            throw new OrganizationPermissionException();
        }
    }

    private Access require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org);
        if (membership == null || !membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        boolean selfOnly = (membership.role("PARENT") || membership.role("ATHLETE"))
            && !membership.role("TRAINER")
            && !membership.role("AGENCY");
        return new Access(selfOnly);
    }
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private record Access(boolean self) { }
    private static final class ListValues { static final java.util.List<String> TYPES = java.util.List.of("SUBSCRIPTION", "TRAINING", "EVENT"); static final java.util.List<String> METHODS = java.util.List.of("SBP", "TRANSFER", "CASH", "OTHER"); static final java.util.List<String> PAYMENT_STATUSES = java.util.List.of("ACTIVE", "VOIDED"); static final java.util.List<String> PAYMENT_STATES = java.util.List.of("UNPAID", "PARTIALLY_PAID", "PAID"); static final java.util.List<String> CHARGE_STATUSES = java.util.List.of("ACTIVE", "CANCELLED"); }
}