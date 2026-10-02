package ru.sportorg.dictionaries;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class DictionaryService {
    private static final List<String> TYPES = List.of("sport-types", "training-types", "venues");
    private final DictionaryRepository repository;
    private final Clock clock;

    DictionaryService(DictionaryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    DictionaryPage find(
            AuthenticatedUser actor,
            UUID org,
            String type,
            String q,
            String status,
            int page,
            int size) {
        require(actor, org, null);
        validate(type, status);
        page(page, size);
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        long total = repository.count(org, type, query, status);
        return new DictionaryPage(
                repository.find(org, type, query, status, size, page * size),
                page,
                size,
                total,
                (int) Math.ceil((double) total / size));
    }

    DictionaryItem create(AuthenticatedUser actor, UUID org, String type, DictionaryWrite write) {
        require(actor, org, "dictionaries.write");
        validate(type, write.status());
        if (write.name() == null
                || write.name().isBlank()
                || !"venues".equals(type) && write.address() != null) {
            throw new OrganizationRequestException("Некорректное значение справочника.");
        }
        try {
            return repository.insert(org, type, write);
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Такое значение уже существует.");
        }
    }

    DictionaryItem patch(AuthenticatedUser actor, UUID org, String type, UUID id, DictionaryWrite write) {
        require(actor, org, "dictionaries.write");
        validate(type, write.status());
        repository.findById(org, type, id).orElseThrow(OrganizationNotFoundException::new);
        if (!"venues".equals(type) && write.address() != null) {
            throw new OrganizationRequestException("Адрес поддерживается только для venues.");
        }
        repository.patch(org, type, id, write, clock.instant());
        return repository.findById(org, type, id).orElseThrow();
    }

    private void validate(String type, String status) {
        if (!TYPES.contains(type)
                || status != null && !List.of("ACTIVE", "ARCHIVED").contains(status)) {
            throw new OrganizationRequestException("Недопустимый тип или статус справочника.");
        }
    }

    private DictionaryRepository.Membership require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org);
        if (membership == null || permission != null && !membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        return membership;
    }

    private void page(int p, int s) {
        if (p < 0 || s < 1 || s > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }
}