package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.bindings.AccessChecker;
import no.mnemonic.act.platform.service.bindings.RetractionChecker;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static no.mnemonic.act.platform.dao.cassandra.entity.Direction.FactIsDestination;
import static no.mnemonic.act.platform.dao.cassandra.entity.Direction.FactIsSource;

public class FactConverter implements Converter<FactEntity, Fact> {

  private static final Logger LOGGER = Logging.getLogger(FactConverter.class);

  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, Organization> organizationConverter;
  private final Function<UUID, Subject> subjectConverter;
  private final Function<UUID, Origin> originConverter;
  private final Function<UUID, Object> objectConverter;
  private final Function<UUID, FactEntity> factEntityResolver;
  private final Predicate<FactEntity> accessChecker;
  private final Predicate<FactEntity> retractionChecker;

  @Inject
  public FactConverter(Function<UUID, FactType> factTypeConverter,
                       Function<UUID, Organization> organizationConverter,
                       Function<UUID, Subject> subjectConverter,
                       Function<UUID, Origin> originConverter,
                       Function<UUID, Object> objectConverter,
                       Function<UUID, FactEntity> factEntityResolver,
                       @AccessChecker Predicate<FactEntity> accessChecker,
                       @RetractionChecker Predicate<FactEntity> retractionChecker) {
    this.factTypeConverter = factTypeConverter;
    this.organizationConverter = organizationConverter;
    this.subjectConverter = subjectConverter;
    this.originConverter = originConverter;
    this.objectConverter = objectConverter;
    this.factEntityResolver = factEntityResolver;
    this.accessChecker = accessChecker;
    this.retractionChecker = retractionChecker;
  }

  @Override
  public Class<FactEntity> getSourceType() {
    return FactEntity.class;
  }

  @Override
  public Class<Fact> getTargetType() {
    return Fact.class;
  }

  @Override
  public Fact apply(FactEntity entity) {
    if (entity == null) return null;
    ConvertedObjects objects = convertObjects(entity);
    return Fact.builder()
            .setId(entity.getId())
            .setType(factTypeConverter.apply(entity.getTypeID()).toInfo())
            .setValue(entity.getValue())
            .setInReferenceTo(ObjectUtils.ifNotNull(convertInReferenceTo(entity.getInReferenceToID()), Fact::toInfo))
            .setOrganization(ObjectUtils.ifNotNull(organizationConverter.apply(entity.getOrganizationID()), Organization::toInfo))
            .setAddedBy(ObjectUtils.ifNotNull(subjectConverter.apply(entity.getAddedByID()), Subject::toInfo))
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(entity.getOriginID()), Origin::toInfo))
            .setTrust(entity.getTrust())
            .setConfidence(entity.getConfidence())
            .setAccessMode(AccessMode.valueOf(entity.getAccessMode().name()))
            .setTimestamp(entity.getTimestamp())
            .setLastSeenTimestamp(entity.getLastSeenTimestamp())
            .setSourceObject(ObjectUtils.ifNotNull(objects, ConvertedObjects::getSourceObject))
            .setDestinationObject(ObjectUtils.ifNotNull(objects, ConvertedObjects::getDestinationObject))
            .setBidirectionalBinding(ObjectUtils.ifNotNull(objects, ConvertedObjects::isBidirectionalBinding, false))
            .setFlags(retractionChecker.test(entity) ? SetUtils.set(Fact.Flag.Retracted) : SetUtils.set())
            .build();
  }

  private ConvertedObjects convertObjects(FactEntity entity) {
    if (CollectionUtils.isEmpty(entity.getBindings())) return null;
    if (CollectionUtils.size(entity.getBindings()) == 1) {
      return convertCardinalityOne(entity.getBindings().get(0));
    }
    if (CollectionUtils.size(entity.getBindings()) == 2) {
      return convertCardinalityTwo(entity, entity.getBindings().get(0), entity.getBindings().get(1));
    }

    // This should never happen as long as create Fact API only allows bindings with cardinality 1 or 2. Log it, just in case.
    LOGGER.warning("Fact is bound to more than two Objects (id = %s). Ignoring Objects in result.", entity.getId());
    return null;
  }

  private ConvertedObjects convertCardinalityOne(FactEntity.FactObjectBinding binding) {
    if (binding.getDirection() == FactIsDestination) {
      return new ConvertedObjects(objectConverter, binding.getObjectID(), null, false);
    }

    if (binding.getDirection() == FactIsSource) {
      return new ConvertedObjects(objectConverter, null, binding.getObjectID(), false);
    }

    // In case of bidirectional binding with cardinality 1 populate source and destination with same Object.
    return new ConvertedObjects(objectConverter, binding.getObjectID(), binding.getObjectID(), true);
  }

  private ConvertedObjects convertCardinalityTwo(FactEntity fact, FactEntity.FactObjectBinding first, FactEntity.FactObjectBinding second) {
    if ((first.getDirection() == FactIsDestination && second.getDirection() == FactIsDestination) ||
            (first.getDirection() == FactIsSource && second.getDirection() == FactIsSource)) {
      // This should never happen as long as create Fact API only allows bindings with cardinality 1 or 2. Log it, just in case.
      LOGGER.warning("Fact is bound to two Objects with the same direction (id = %s). Ignoring Objects in result.", fact.getId());
      return null;
    }

    // If 'first' has direction 'FactIsDestination' it's the source Object and 'second' the destination Object ...
    if (first.getDirection() == FactIsDestination) {
      return new ConvertedObjects(objectConverter, first.getObjectID(), second.getObjectID(), false);
    }
    // ... and vice versa. They can't have the same direction!
    if (second.getDirection() == FactIsDestination) {
      return new ConvertedObjects(objectConverter, second.getObjectID(), first.getObjectID(), false);
    }

    // With bidirectional binding it doesn't matter which Object is source/destination.
    // In order to be consistent always set first as source and second as destination.
    return new ConvertedObjects(objectConverter, first.getObjectID(), second.getObjectID(), true);
  }

  private Fact convertInReferenceTo(UUID inReferenceToID) {
    if (inReferenceToID == null) return null;

    FactEntity inReferenceTo = factEntityResolver.apply(inReferenceToID);
    if (inReferenceTo == null || !accessChecker.test(inReferenceTo)) {
      // If User doesn't have access to 'inReferenceTo' Fact it shouldn't be returned as part of the converted Fact.
      LOGGER.debug("Removed inReferenceTo Fact from result because user does not have access to it (id = %s).", inReferenceToID);
      return null;
    }

    // Convert 'inReferenceTo' Fact, but avoid resolving recursive 'inReferenceTo' Facts.
    // Clone entity first in order to not disturb DAO layer.
    return apply(inReferenceTo.clone().setInReferenceToID(null));
  }

  private class ConvertedObjects {
    private final Function<UUID, Object> objectConverter;
    private final UUID sourceObjectID;
    private final UUID destinationObjectID;
    private final boolean bidirectionalBinding;

    private ConvertedObjects(Function<UUID, Object> objectConverter, UUID sourceObjectID, UUID destinationObjectID, boolean bidirectionalBinding) {
      this.objectConverter = objectConverter;
      this.sourceObjectID = sourceObjectID;
      this.destinationObjectID = destinationObjectID;
      this.bidirectionalBinding = bidirectionalBinding;
    }

    private Object.Info getSourceObject() {
      if (sourceObjectID == null) return null;
      return ObjectUtils.ifNotNull(objectConverter.apply(sourceObjectID), Object::toInfo);
    }

    private Object.Info getDestinationObject() {
      if (destinationObjectID == null) return null;
      return ObjectUtils.ifNotNull(objectConverter.apply(destinationObjectID), Object::toInfo);
    }

    private boolean isBidirectionalBinding() {
      return bidirectionalBinding;
    }
  }
}
