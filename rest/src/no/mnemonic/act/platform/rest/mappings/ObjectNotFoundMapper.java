package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectNotFoundMapper implements ExceptionMapper<ObjectNotFoundException> {

  @Override
  public Response toResponse(ObjectNotFoundException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.NOT_FOUND)
            .addActionError(ex.getMessage(), ex.getMessageTemplate(), ex.getProperty(), ex.getValue())
            .buildResponse();
  }

}
