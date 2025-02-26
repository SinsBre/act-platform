package no.mnemonic.act.platform.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.auth.properties.module.PropertiesBasedAccessControllerModule;
import no.mnemonic.act.platform.dao.DaoModule;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.seb.esengine.modules.SebESEngineModule;
import no.mnemonic.act.platform.seb.producer.modules.SebProducerModule;
import no.mnemonic.act.platform.service.aspects.*;
import no.mnemonic.act.platform.service.providers.HazelcastBasedLockProvider;
import no.mnemonic.act.platform.service.providers.HazelcastInstanceProvider;
import no.mnemonic.act.platform.service.providers.LockProvider;
import no.mnemonic.act.platform.service.providers.TriggerEventConsumerProvider;
import no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl;
import no.mnemonic.act.platform.service.ti.caches.ResponseCachesModule;
import no.mnemonic.act.platform.service.validators.DefaultValidatorFactory;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.triggers.api.service.v1.TriggerAdministrationService;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import no.mnemonic.services.triggers.service.TriggerAdministrationServiceImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Module which configures the implementation of the ThreatIntelligenceService.
 */
public class TiServiceModule extends AbstractModule {

  private boolean skipDefaultAccessController;
  private boolean skipDefaultHazelcastInstanceProvider;

  @Override
  protected void configure() {
    // Install all dependencies for the service.
    install(new DaoModule());
    install(new SebProducerModule());
    install(new SebESEngineModule());
    install(new RuntimeExceptionHandlerAspect());
    install(new AuthenticationAspect());
    install(new ValidationAspect());
    install(new TriggerContextAspect());
    install(new ServiceRequestScopeAspect());
    install(new ResponseCachesModule());

    if (!skipDefaultAccessController) {
      // Omit default access controller if the module is configured using withoutDefaultAccessController().
      install(new PropertiesBasedAccessControllerModule());
    }

    if (!skipDefaultHazelcastInstanceProvider) {
      // Omit default Hazelcast instance provider if the module is configured using withoutDefaultHazelcastInstanceProvider().
      bind(HazelcastInstance.class).toProvider(HazelcastInstanceProvider.class).in(Scopes.SINGLETON);
    }

    // Configure the ActionTriggers' pipeline worker and administration service.
    bind(TriggerEventConsumer.class).toProvider(TriggerEventConsumerProvider.class).in(Scopes.SINGLETON);
    // Can't be a Singleton because Guice would instantiate it eagerly, however, as TriggerEventConsumer
    // is already a Singleton TriggerAdministrationService will only be instantiated once.
    bind(TriggerAdministrationService.class).to(TriggerAdministrationServiceImpl.class);

    // Bind the concrete implementation classes of the ThreatIntelligenceService.
    bind(LockProvider.class).to(HazelcastBasedLockProvider.class).in(Scopes.SINGLETON);
    bind(ValidatorFactory.class).to(DefaultValidatorFactory.class).in(Scopes.SINGLETON);
    bind(ThreatIntelligenceService.class).to(ThreatIntelligenceServiceImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> provideFactStatisticsResolver() {
    // Don't include statistics in the default ObjectConverter, e.g. when including Objects as part of Facts.
    // When statistics should be included in responses create a new instance of the ObjectConverter instead.
    return id -> Collections.emptyList();
  }

  /**
   * Instruct the module to omit the default access controller implementation. In this case an alternative
   * implementation must be configured in Guice.
   *
   * @return this
   */
  public TiServiceModule withoutDefaultAccessController() {
    this.skipDefaultAccessController = true;
    return this;
  }

  /**
   * Instruct the module to omit the default Hazelcast instance provider. In this case an alternative
   * provider must be configured in Guice.
   *
   * @return this
   */
  public TiServiceModule withoutDefaultHazelcastInstanceProvider() {
    this.skipDefaultHazelcastInstanceProvider = true;
    return this;
  }
}
