package no.mnemonic.act.platform.service.ti.converters.request;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.request.SearchByNameRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;

public class SearchObjectRequestConverter {

  private static final int DEFAULT_LIMIT = 25;

  private final SearchByNameRequestResolver byNameResolver;
  private final SecurityContext securityContext;

  @Inject
  public SearchObjectRequestConverter(SearchByNameRequestResolver byNameResolver,
                                      SecurityContext securityContext) {
    this.byNameResolver = byNameResolver;
    this.securityContext = securityContext;
  }

  public FactSearchCriteria apply(SearchObjectRequest request) throws InvalidArgumentException {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .setKeywords(request.getKeywords())
            .setObjectID(request.getObjectID())
            .setFactID(request.getFactID())
            .setObjectTypeID(byNameResolver.resolveObjectType(request.getObjectType()))
            .setFactTypeID(byNameResolver.resolveFactType(request.getFactType()))
            .setObjectValue(request.getObjectValue())
            .setFactValue(request.getFactValue())
            .setOrganizationID(byNameResolver.resolveOrganization(request.getOrganization()))
            .setOriginID(byNameResolver.resolveOrigin(request.getOrigin()))
            .setMinNumber(request.getMinimum())
            .setMaxNumber(request.getMaximum())
            .addNumberFieldStrategy(ObjectUtils.ifNotNull(request.getDimension(),
                    dimension -> FactSearchCriteria.NumberFieldStrategy.valueOf(dimension.name()),
                    FactSearchCriteria.NumberFieldStrategy.certainty))
            .setStartTimestamp(request.getAfter())
            .setEndTimestamp(request.getBefore())
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp)
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();
  }
}
