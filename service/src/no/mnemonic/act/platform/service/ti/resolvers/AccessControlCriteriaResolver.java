package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resolver which creates an {@link AccessControlCriteria} object based on the current user's permissions.
 * The created criteria can be used directly in other search criterias for access control purposes.
 */
public class AccessControlCriteriaResolver implements Supplier<AccessControlCriteria> {

  private final SecurityContext securityContext;

  @Inject
  public AccessControlCriteriaResolver(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  @Override
  public AccessControlCriteria get() {
    return AccessControlCriteria.builder()
            .setCurrentUserIdentities(securityContext.getCurrentUserIdentities())
            .setAvailableOrganizationID(resolveAvailableOrganizations())
            .build();
  }

  private Set<UUID> resolveAvailableOrganizations() {
    // getAvailableOrganizationID() will return all available organizations regardless of specific permissions.
    // Need to return the organizations for which the user is actually allowed to view Facts.
    return securityContext.getAvailableOrganizationID().stream()
            .filter(this::isAllowedToViewFacts)
            .collect(Collectors.toSet());
  }

  private boolean isAllowedToViewFacts(UUID organizationID) {
    try {
      securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact, organizationID);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }
}
