package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@ApiModel(description = "Request for traversing an Object/Fact graph")
public class TraverseGraphRequest implements ValidatingRequest {

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

  public String getQuery() {
    return query;
  }

  public TraverseGraphRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public TraverseGraphRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public TraverseGraphRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public TraverseGraphRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }
}
