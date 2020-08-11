package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.UUID;

@JsonDeserialize(builder = SubjectInfoSEB.Builder.class)
public class SubjectInfoSEB {

  private final UUID id;
  private final String name;

  private SubjectInfoSEB(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "set")
  public static class Builder {
    private UUID id;
    private String name;

    private Builder() {
    }

    public SubjectInfoSEB build() {
      return new SubjectInfoSEB(id, name);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }
  }
}
