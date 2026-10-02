package ru.sportorg.dictionaries;

import java.util.UUID;

import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/dictionaries/{dictionaryType}")
class DictionaryController {
    private final DictionaryService service;

    DictionaryController(DictionaryService service) {
        this.service = service;
    }

    @GetMapping
    DictionaryPage find(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable String dictionaryType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.find(actor, organizationId, dictionaryType, q, status, page, size);
    }

    @PostMapping
    DictionaryItem create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable String dictionaryType,
            @RequestBody DictionaryWrite write) {
        return service.create(actor, organizationId, dictionaryType, write);
    }

    @PatchMapping("/{itemId}")
    DictionaryItem patch(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable UUID organizationId,
            @PathVariable String dictionaryType,
            @PathVariable UUID itemId,
            @RequestBody DictionaryWrite write) {
        return service.patch(actor, organizationId, dictionaryType, itemId, write);
    }
}