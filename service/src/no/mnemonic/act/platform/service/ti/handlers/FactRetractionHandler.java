package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.scopes.ServiceRequestScope;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler class computing whether a Fact has been retracted. See {@link #isRetracted(FactRecord)} for the details.
 */
@ServiceRequestScope
public class FactRetractionHandler {

  private final Map<UUID, Boolean> retractionCache = new ConcurrentHashMap<>();

  private final FactTypeRequestResolver factTypeRequestResolver;
  private final FactSearchManager factSearchManager;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;

  @Inject
  public FactRetractionHandler(FactTypeRequestResolver factTypeRequestResolver,
                               FactSearchManager factSearchManager,
                               AccessControlCriteriaResolver accessControlCriteriaResolver) {
    this.factTypeRequestResolver = factTypeRequestResolver;
    this.factSearchManager = factSearchManager;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
  }

  /**
   * Check whether a Fact as been retracted (from the current user's point of view).
   * <p>
   * A Fact has been retracted from the current user's point of view if the user has access to at least one Retraction
   * Fact and that Retraction Fact is not in turn retracted. The implementation will recursively follow the retractions
   * in order to figure out if retractions have been retracted.
   * <p>
   * The result of the above computation is cached per class instance in order to speed up multiple checks for the same
   * Fact. Note that one class instance should not been used across multiple requests from different users because the
   * computation result depends on whether the user has access to retractions.
   *
   * @param fact        FactRecord to check
   * @return True if Fact has been retracted
   */
  public boolean isRetracted(FactRecord fact) {
    if (fact == null) return false;

    boolean retractedHint = SetUtils.set(fact.getFlags()).contains(FactRecord.Flag.RetractedHint);

    // If it's known that the Fact has never been retracted store this information immediately.
    // This will save a lot of calls to ElasticSearch!
    if (!retractedHint) {
      retractionCache.put(fact.getId(), false);
    }

    // If no hint is provided or the Fact has been retracted by some user
    // compute if the Fact is retracted from the current user's point of view.
    return retractionCache.computeIfAbsent(fact.getId(), this::computeRetraction);
  }

  private boolean computeRetraction(UUID factID) {
    List<FactDocument> retractions = fetchRetractions(factID);
    if (CollectionUtils.isEmpty(retractions)) {
      // No accessible retractions, thus, the Fact isn't retracted.
      return false;
    }

    // The Fact is only retracted if not all of the retractions themselves are retracted.
    return !retractions.stream().allMatch(fact -> computeRetraction(fact.getId()));
  }

  private List<FactDocument> fetchRetractions(UUID factID) {
    // Create criteria to fetch all Retraction Facts for a given Fact. Only return retractions which a user has access
    // to. No access to retractions means that from the user's perspective the referenced Fact isn't retracted.
    FactSearchCriteria retractionsCriteria = FactSearchCriteria.builder()
            .addInReferenceTo(factID)
            .addFactTypeID(factTypeRequestResolver.resolveRetractionFactType().getId())
            .setAccessControlCriteria(accessControlCriteriaResolver.get())
            .build();

    // The number of retractions will be very small (typically one), thus, it's no problem to consume all results at once.
    return ListUtils.list(factSearchManager.searchFacts(retractionsCriteria));
  }
}
