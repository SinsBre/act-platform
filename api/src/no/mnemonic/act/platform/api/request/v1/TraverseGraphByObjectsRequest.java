package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@ApiModel(description = "Traverse the Object/Fact graph starting at a set of objects identified by either their id " +
        "or type/value tuple.")
public class TraverseGraphByObjectsRequest implements ValidatingRequest {

  @ApiModelProperty(
          value = "Set of object identifiers. Takes Object UUID or Object identified by 'type/value')",
          example = "['123e4567-e89b-12d3-a456-426655440000', 'ThreatActor/Sofacy']",
          required = true)
  @NotEmpty
  private Set<String> objects;

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;
  @ApiModelProperty(value = "Traverse retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only traverse Facts seen before a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only traverse Facts seen after a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the result size (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

  public TraverseGraphByObjectsRequest setObjects(Set<String> objects) {
    this.objects = ObjectUtils.ifNotNull(objects, SetUtils::set);
    return this;
  }

  public TraverseGraphByObjectsRequest addObject(String object) {
    this.objects = SetUtils.addToSet(this.objects, object);
    return this;
  }

  public Set<String> getObjects() {
    return objects;
  }

  public String getQuery() {
    return query;
  }

  public TraverseGraphByObjectsRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphByObjectsRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public TraverseGraphByObjectsRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public TraverseGraphByObjectsRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public TraverseGraphByObjectsRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public static TraverseGraphByObjectsRequest from(TraverseGraphRequest request, String object) {
    return new TraverseGraphByObjectsRequest()
            .setQuery(request.getQuery())
            .setAfter(request.getAfter())
            .setBefore(request.getBefore())
            .setIncludeRetracted(request.getIncludeRetracted())
            .setLimit(request.getLimit())
            .addObject(object);
  }
}
