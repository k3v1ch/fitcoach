package ru.sportorg.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationConflictException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceChargesServiceTest {
    @Mock private FinanceRepository repository;
    private FinanceService service;
    private UUID org;
    private UUID trainerId;
    private UUID parentId;
    private AuthenticatedUser trainer;
    private AuthenticatedUser parent;

    @BeforeEach
    void setUp() {
        service = new FinanceService(repository);
        org = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        trainer = new AuthenticatedUser(trainerId, "t@example.org", "t@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
        parent = new AuthenticatedUser(parentId, "p@example.org", "p@example.org", "Parent", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(new FinanceRepository.Membership(
                "[\"TRAINER\"]", "[\"charges.read\",\"charges.write\",\"payments.read\",\"payments.write\"]"));
        when(repository.membership(parentId, org)).thenReturn(new FinanceRepository.Membership(
                "[\"PARENT\"]", "[\"charges.read\",\"payments.read\"]"));
    }

    private Charge charge(String status, String amount, String paid) {
        BigDecimal a = new BigDecimal(amount);
        BigDecimal p = new BigDecimal(paid);
        String paymentStatus = p.signum() == 0 ? "UNPAID" : a.compareTo(p) == 0 ? "PAID" : "PARTIALLY_PAID";
        return new Charge(UUID.randomUUID(), org, UUID.randomUUID(), UUID.randomUUID(), "SUBSCRIPTION", "Абонемент",
                a, LocalDate.of(2026, 10, 10), p, a.subtract(p), paymentStatus, false, status);
    }

    private ChargePatch patch(String json) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().readValue(json, ChargePatch.class);
    }

    @Test
    void parentSeesOnlyOwnCharges() {
        when(repository.countCharges(eq(org), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(parentId))).thenReturn(0L);
        when(repository.charges(eq(org), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(parentId), anyInt(), anyInt())).thenReturn(List.of());

        ChargePage page = service.charges(parent, org, null, null, null, null, null, null, null, null, null, null, 0, 20);

        assertEquals(0, page.totalElements());
        verify(repository).countCharges(eq(org), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(parentId));
    }

    @Test
    void trainerSeesWholeOrganization() {
        when(repository.charges(eq(org), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), isNull(), anyInt(), anyInt())).thenReturn(List.of());

        service.charges(trainer, org, "  абонемент ", null, null, null, null, "UNPAID", true, "ACTIVE", null, null, 0, 20);

        verify(repository).countCharges(eq(org), eq("абонемент"), isNull(), isNull(), isNull(), isNull(), eq("UNPAID"), eq(true), eq("ACTIVE"), isNull(), isNull(), isNull());
    }

    @Test
    void invalidChargeFiltersAreRejected() {
        assertThrows(OrganizationRequestException.class, () ->
                service.charges(trainer, org, null, null, null, null, null, "SOMETIMES", null, null, null, null, 0, 20));
        assertThrows(OrganizationRequestException.class, () ->
                service.charges(trainer, org, null, null, null, "WEEKLY", null, null, null, null, null, null, 0, 20));
        assertThrows(OrganizationRequestException.class, () ->
                service.charges(trainer, org, null, null, null, null, null, null, null, null, null, null, 0, 101));
        assertThrows(OrganizationRequestException.class, () ->
                service.charges(trainer, org, null, null, null, null, null, null, null, null,
                        LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), 0, 20));
    }

    @Test
    void cancelWithActivePaymentsIsConflict() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "400.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));

        assertThrows(OrganizationConflictException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"status\":\"CANCELLED\",\"cancelReason\":\"ошибка\"}")));
        verify(repository, never()).cancelCharge(any(), any(), any(), any());
    }

    @Test
    void cancelNeedsReasonAndNoOtherChanges() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "0.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));

        assertThrows(OrganizationRequestException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"status\":\"CANCELLED\"}")));
        assertThrows(OrganizationRequestException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"status\":\"CANCELLED\",\"cancelReason\":\"дубль\",\"amount\":500.00}")));
        assertThrows(OrganizationRequestException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"cancelReason\":\"без статуса\"}")));
    }

    @Test
    void cancelUnpaidChargeIsLogged() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "0.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));
        when(repository.charge(org, c.id())).thenReturn(Optional.of(c));

        service.updateCharge(trainer, org, c.id(), patch("{\"status\":\"CANCELLED\",\"cancelReason\":\" дубль \"}"));

        verify(repository).cancelCharge(org, c.id(), "дубль", trainerId);
        verify(repository).recordActivity(eq(org), eq(trainerId), eq("CHARGE_CANCELLED"), eq("CHARGE"), eq(c.id()), any());
    }

    @Test
    void amountCannotDropBelowPaid() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "400.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));

        assertThrows(OrganizationRequestException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"amount\":399.99}")));
        verify(repository, never()).updateCharge(any(), any(), any(), any());
    }

    @Test
    void cancelledChargeIsReadOnly() throws Exception {
        Charge c = charge("CANCELLED", "1000.00", "0.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));

        assertThrows(OrganizationConflictException.class, () -> service.updateCharge(trainer, org, c.id(),
                patch("{\"title\":\"Новое\"}")));
    }

    @Test
    void parentCannotEditCharges() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "0.00");
        assertThrows(OrganizationPermissionException.class, () -> service.updateCharge(parent, org, c.id(),
                patch("{\"title\":\"Новое\"}")));
    }

    @Test
    void validEditIsSavedAndLogged() throws Exception {
        Charge c = charge("ACTIVE", "1000.00", "400.00");
        when(repository.lockCharge(org, c.id())).thenReturn(Optional.of(c));
        when(repository.charge(org, c.id())).thenReturn(Optional.of(c));

        service.updateCharge(trainer, org, c.id(), patch("{\"amount\":400.00,\"comment\":null}"));

        verify(repository).updateCharge(eq(org), eq(c.id()), any(ChargePatch.class), eq(trainerId));
        verify(repository).recordActivity(eq(org), eq(trainerId), eq("CHARGE_UPDATED"), eq("CHARGE"), eq(c.id()), any());
    }

    @Test
    void voidingTwiceIsConflict() {
        Payment p = new Payment(UUID.randomUUID(), org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), LocalDate.of(2026, 10, 1), "CASH", null, "VOIDED", trainerId,
                java.time.Instant.now(), trainerId, java.time.Instant.now(), "ошибка");
        when(repository.lockPayment(org, p.id())).thenReturn(Optional.of(p));

        assertThrows(OrganizationConflictException.class, () -> service.voidPayment(trainer, org, p.id(), new PaymentVoid("повтор")));
        verify(repository, never()).voidPayment(any(), any(), any(), any());
    }

    @Test
    void voidNeedsReasonAndIsLogged() {
        Payment p = new Payment(UUID.randomUUID(), org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), LocalDate.of(2026, 10, 1), "CASH", null, "ACTIVE", trainerId,
                java.time.Instant.now(), null, null, null);
        Charge c = charge("ACTIVE", "1000.00", "0.00");
        when(repository.lockPayment(org, p.id())).thenReturn(Optional.of(p));
        when(repository.payment(org, p.id())).thenReturn(Optional.of(p));
        when(repository.charge(org, p.chargeId())).thenReturn(Optional.of(c));

        assertThrows(OrganizationRequestException.class, () -> service.voidPayment(trainer, org, p.id(), new PaymentVoid(" ")));

        PaymentResult result = service.voidPayment(trainer, org, p.id(), new PaymentVoid("ошибочная сумма"));

        verify(repository).voidPayment(org, p.id(), "ошибочная сумма", trainerId);
        verify(repository).recordActivity(eq(org), eq(trainerId), eq("PAYMENT_VOIDED"), eq("PAYMENT"), eq(p.id()), any());
        assertEquals(c, result.charge());
    }
}
