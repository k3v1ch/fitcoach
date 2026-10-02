package ru.sportorg.finance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {
    @Mock private FinanceRepository repository;
    private FinanceService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser trainer;

    @BeforeEach
    void setUp() {
        service = new FinanceService(repository);
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        trainer = new AuthenticatedUser(userId, "trainer@example.org", "trainer@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
        when(repository.membership(userId, organizationId)).thenReturn(new FinanceRepository.Membership(
                "[\"TRAINER\"]", "[\"payments.write\",\"payments.read\"]"));
    }

    @Test
    void paymentCannotExceedRemainingAmount() {
        UUID chargeId = UUID.randomUUID();
        when(repository.byKey(organizationId, "key")).thenReturn(Optional.empty());
        when(repository.lockCharge(organizationId, chargeId)).thenReturn(Optional.of(new Charge(
                chargeId, organizationId, UUID.randomUUID(), UUID.randomUUID(), "EVENT", "Fee",
                new BigDecimal("100.00"), LocalDate.of(2026, 10, 1), new BigDecimal("60.00"),
                new BigDecimal("40.00"), "PARTIALLY_PAID", false, "ACTIVE")));

        assertThrows(OrganizationRequestException.class, () -> service.pay(trainer, organizationId,
                new PaymentWrite(chargeId, new BigDecimal("40.01"), LocalDate.of(2026, 10, 1), "CASH", null), "key"));
        verify(repository, never()).insertPayment(any(), any(), any(), any(), any());
    }

    @Test
    void repeatedKeyReturnsExistingPayment() {
        Payment existing = new Payment(UUID.randomUUID(), organizationId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), LocalDate.of(2026, 10, 1), "CASH", null, "ACTIVE", userId,
                Instant.parse("2026-10-01T12:00:00Z"), null, null, null);
        when(repository.byKey(organizationId, "key")).thenReturn(Optional.of(existing));

        Payment actual = service.pay(trainer, organizationId,
                new PaymentWrite(existing.chargeId(), new BigDecimal("10.00"), existing.paidOn(), "CASH", null), "key");

        org.junit.jupiter.api.Assertions.assertEquals(existing, actual);
        verify(repository, never()).lockCharge(any(), any());
    }

    @Test
    void paymentHistoryAppliesFiltersAndPagination() {
        UUID paymentId = UUID.randomUUID();
        UUID athleteId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        Payment payment = new Payment(paymentId, organizationId, chargeId, athleteId, sectionId,
                new BigDecimal("10.00"), from, "CASH", null, "ACTIVE", userId,
                Instant.parse("2026-10-01T12:00:00Z"), null, null, null);
        when(repository.countPayments(organizationId, paymentId, athleteId, sectionId, chargeId, from, to, "ACTIVE", null))
                .thenReturn(1L);
        when(repository.payments(organizationId, paymentId, athleteId, sectionId, chargeId, from, to, "ACTIVE", null, 10, 10))
                .thenReturn(List.of(payment));

        PaymentPage result = service.payments(trainer, organizationId, paymentId, athleteId, sectionId, chargeId,
                from, to, "ACTIVE", 1, 10);

        org.junit.jupiter.api.Assertions.assertEquals(new PaymentPage(List.of(payment), 1, 10, 1, 1), result);
        verify(repository).payments(organizationId, paymentId, athleteId, sectionId, chargeId, from, to, "ACTIVE", null, 10, 10);
    }

    @Test
    void paymentHistoryRejectsInvalidDateRange() {
        assertThrows(OrganizationRequestException.class, () -> service.payments(trainer, organizationId,
                null, null, null, null, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), null, 0, 20));
        verify(repository, never()).countPayments(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

        @Test
        void trainerWithAthleteRoleCanReadOrganizationPaymentHistory() {
                when(repository.membership(userId, organizationId)).thenReturn(new FinanceRepository.Membership(
                                "[\"TRAINER\",\"ATHLETE\"]", "[\"payments.read\"]"));
                when(repository.countPayments(organizationId, null, null, null, null, null, null, null, null)).thenReturn(0L);
                when(repository.payments(organizationId, null, null, null, null, null, null, null, null, 20, 0))
                                .thenReturn(List.of());

                service.payments(trainer, organizationId, null, null, null, null, null, null, null, 0, 20);

                verify(repository).countPayments(organizationId, null, null, null, null, null, null, null, null);
        }
}