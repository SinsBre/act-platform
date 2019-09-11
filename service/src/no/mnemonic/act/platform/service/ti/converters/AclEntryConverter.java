package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class AclEntryConverter implements Converter<FactAclEntity, AclEntry> {

  private final Function<UUID, Origin> originConverter;
  private final Function<UUID, Subject> subjectConverter;

  @Inject
  public AclEntryConverter(Function<UUID, Origin> originConverter,
                           Function<UUID, Subject> subjectConverter) {
    this.originConverter = originConverter;
    this.subjectConverter = subjectConverter;
  }

  @Override
  public Class<FactAclEntity> getSourceType() {
    return FactAclEntity.class;
  }

  @Override
  public Class<AclEntry> getTargetType() {
    return AclEntry.class;
  }

  @Override
  public AclEntry apply(FactAclEntity entity) {
    if (entity == null) return null;
    return AclEntry.builder()
            .setId(entity.getId())
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(entity.getOriginID()), Origin::toInfo))
            .setSubject(ObjectUtils.ifNotNull(subjectConverter.apply(entity.getSubjectID()), Subject::toInfo))
            .setTimestamp(entity.getTimestamp())
            .build();
  }
}
