package no.mnemonic.act.platform.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.swagger.SwaggerApiListingResource;
import no.mnemonic.services.common.documentation.swagger.ResultContainerTransformation;
import no.mnemonic.services.common.documentation.swagger.SwaggerModelTransformer;

/**
 * Module which configures Swagger used for API documentation.
 */
public class SwaggerModule extends AbstractModule {

  private static final String API_PACKAGE = "no.mnemonic.act.platform.rest.api";

  @Override
  protected void configure() {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setBasePath("/");
    beanConfig.setResourcePackage(API_PACKAGE);
    beanConfig.setScan(true);

    SwaggerModelTransformer transformer = SwaggerModelTransformer.builder()
            .addTransformation(new ResultContainerTransformation(ResultStash.class, "data"))
            .build();

    bind(SwaggerApiListingResource.class).in(Scopes.SINGLETON);
    bind(SwaggerSerializers.class).in(Scopes.SINGLETON);
    bind(SwaggerModelTransformer.class).toInstance(transformer);
  }
}
