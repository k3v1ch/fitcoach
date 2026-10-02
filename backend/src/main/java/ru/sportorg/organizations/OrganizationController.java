package ru.sportorg.organizations;

import java.util.UUID;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
class OrganizationController {

    private final OrganizationService organizationService;

    OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    Organization getOrganization(@AuthenticationPrincipal AuthenticatedUser actor,
                                 @PathVariable UUID organizationId) {
        return organizationService.getOrganization(actor, organizationId);
    }

    @PatchMapping
    Organization patchOrganization(@AuthenticationPrincipal AuthenticatedUser actor,
                                   @PathVariable UUID organizationId,
                                   @Valid @RequestBody OrganizationPatch patch) {
        return organizationService.patchOrganization(actor, organizationId, patch);
    }

    @GetMapping("/members")
    OrganizationMemberPage getMembers(@AuthenticationPrincipal AuthenticatedUser actor,
                                       @PathVariable UUID organizationId,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String role,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return organizationService.getMembers(actor, organizationId, q, role, status, page, size);
    }

    @PostMapping("/members/parents")
    OrganizationMember addParent(@AuthenticationPrincipal AuthenticatedUser actor,
                                 @PathVariable UUID organizationId,
                                 @Valid @RequestBody ParentMembershipWrite write) {
        return organizationService.addParent(actor, organizationId, write);
    }
}