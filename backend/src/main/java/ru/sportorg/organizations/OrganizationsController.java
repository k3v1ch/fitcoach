package ru.sportorg.organizations;

import java.util.List;

import jakarta.validation.Valid;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationsController {

    private final OrganizationService organizationService;

    OrganizationsController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    List<OrganizationAccess> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return organizationService.listOrganizations(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Organization create(@AuthenticationPrincipal AuthenticatedUser actor,
                        @Valid @RequestBody OrganizationWrite write) {
        return organizationService.createOrganization(actor, write);
    }
}