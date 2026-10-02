package ru.sportorg.documents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class DocumentService {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final List<String> TYPES = List.of("MEDICAL_CERTIFICATE", "CONSENT", "OTHER");
    private final DocumentRepository repository;
    private final Path root;
    private final Clock clock;

    DocumentService(
            DocumentRepository repository,
            @Value("${app.files.root:./data/files}") String root,
            Clock clock) {
        this.repository = repository;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.clock = clock;
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create file storage", e);
        }
    }

    @Transactional
    StoredFile uploadAttachment(AuthenticatedUser actor, UUID org, MultipartFile file) {
        require(actor, org, "announcements.write");
        return store(actor, org, file);
    }

    @Transactional
    void deleteFile(AuthenticatedUser actor, UUID org, UUID fileId) {
        require(actor, org, null);
        StoredFile file = repository.file(org, fileId).orElseThrow(OrganizationNotFoundException::new);
        repository.deleteFile(org, fileId, actor.userId());
        removePhysical(file);
    }

    @Transactional
    Document uploadDocument(
            AuthenticatedUser actor,
            UUID org,
            MultipartFile file,
            String title,
            String type,
            UUID athleteId,
            LocalDate issued,
            LocalDate valid) {
        Access access = requireDocumentUpload(actor, org);
        if (!TYPES.contains(type)
                || title == null
                || title.isBlank()
                || valid != null && issued != null && valid.isBefore(issued)) {
            throw new OrganizationRequestException("Некорректные данные документа.");
        }
        if (athleteId != null && !repository.athleteExists(org, athleteId)) {
            throw new OrganizationNotFoundException();
        }
        if (access.self && (athleteId == null || !repository.athleteVisible(org, athleteId, actor.userId()))) {
            throw new OrganizationNotFoundException();
        }
        StoredFile stored = store(actor, org, file);
        try {
            return repository.insertDocument(
                    org, athleteId, type, title, stored.id(), issued, valid, actor.userId(), clock.instant());
        } catch (RuntimeException e) {
            removePhysical(stored);
            throw e;
        }
    }

    DocumentPage documents(
            AuthenticatedUser actor,
            UUID org,
            String q,
            UUID athleteId,
            String type,
            Boolean expired,
            int page,
            int size) {
        Access access = require(actor, org, "documents.read");
        page(page, size);
        if (type != null && !TYPES.contains(type)) {
            throw new OrganizationRequestException("Недопустимый тип документа.");
        }
        if (access.self && athleteId != null && !repository.athleteVisible(org, athleteId, actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        String query = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        long total = repository.countDocuments(org, query, athleteId, type, expired);
        return new DocumentPage(
                repository.documents(org, query, athleteId, type, expired, size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    Document get(AuthenticatedUser actor, UUID org, UUID id) {
        Access access = require(actor, org, "documents.read");
        Document document = repository.findDocument(org, id).orElseThrow(OrganizationNotFoundException::new);
        if (access.self
                && (document.athleteId() == null
                || !repository.athleteVisible(org, document.athleteId(), actor.userId()))) {
            throw new OrganizationNotFoundException();
        }
        return document;
    }

    Resource content(AuthenticatedUser actor, UUID org, UUID fileId) {
        Access access = require(actor, org, "documents.read");
        if (access.self && !repository.fileVisible(org, fileId, actor.userId())) {
            throw new OrganizationNotFoundException();
        }
        StoredFile file = repository.file(org, fileId).orElseThrow(OrganizationNotFoundException::new);
        try {
            Resource resource = new UrlResource(root.resolve(file.storageName()).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new OrganizationNotFoundException();
            }
            return resource;
        } catch (IOException e) {
            throw new OrganizationNotFoundException();
        }
    }

    private StoredFile store(AuthenticatedUser actor, UUID org, MultipartFile file) {
        validateFile(file);
        String original = file.getOriginalFilename() == null
                ? "file"
                : Path.of(file.getOriginalFilename()).getFileName().toString();
        UUID id = UUID.randomUUID();
        String storage = UUID.randomUUID().toString();
        StoredFile stored = new StoredFile(
                id, org, original, file.getContentType(), file.getSize(), storage, actor.userId(), clock.instant());
        try {
            Files.copy(file.getInputStream(), root.resolve(storage));
            repository.insertFile(stored);
            return stored;
        } catch (IOException e) {
            throw new OrganizationRequestException("Файл не удалось сохранить.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new OrganizationRequestException("Файл должен быть непустым и не больше 10 МБ.");
        }
        String name = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String type = file.getContentType();
        try {
            byte[] head = file.getInputStream().readNBytes(12);
            boolean pdf = name.endsWith(".pdf")
                    && "application/pdf".equals(type)
                    && starts(head, new byte[]{'%', 'P', 'D', 'F'});
            boolean png = name.endsWith(".png")
                    && "image/png".equals(type)
                    && starts(head, new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            boolean jpg = (name.endsWith(".jpg") || name.endsWith(".jpeg"))
                    && "image/jpeg".equals(type)
                    && starts(head, new byte[]{(byte) 255, (byte) 216, (byte) 255});
            if (!pdf && !png && !jpg) {
                throw new OrganizationRequestException("Разрешены только PDF, PNG и JPEG с корректной сигнатурой.");
            }
        } catch (IOException e) {
            throw new OrganizationRequestException("Файл не удалось прочитать.");
        }
    }

    private boolean starts(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private Access require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org);
        if (membership == null || permission != null && !membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        return new Access(membership.role("PARENT") || membership.role("ATHLETE"));
    }

    private Access requireDocumentUpload(AuthenticatedUser actor, UUID org) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org);
        if (membership == null
                || !(membership.permission("documents.write")
                || membership.permission("documents.read")
                && (membership.role("PARENT") || membership.role("ATHLETE")))) {
            throw new OrganizationPermissionException();
        }
        return new Access(membership.role("PARENT") || membership.role("ATHLETE"));
    }

    private void removePhysical(StoredFile file) {
        try {
            Files.deleteIfExists(root.resolve(file.storageName()));
        } catch (IOException ignored) {
        }
    }

    private void page(int p, int s) {
        if (p < 0 || s < 1 || s > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }

    private int pages(long t, int s) {
        return (int) Math.ceil((double) t / s);
    }
    private record Access(boolean self) { }
}