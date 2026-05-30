package io.kudos.ability.log.audit.mongo.init

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import io.kudos.ability.log.audit.mongo.service.MongoAuditLogReadOnlyService
import io.kudos.ability.log.audit.mongo.service.MongoAuditService
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

/**
 * Wires the Mongo audit backend.
 *
 * Gating:
 *  - [ConditionalOnClass] [MongoTemplate]: avoid wiring when an upstream depender pulls the
 *    module transitively without intending to enable Mongo (e.g. a multi-backend bundle that
 *    only ever uses RDB).
 *  - [ConditionalOnMissingBean] on [IAuditService]: an app that already declares a different
 *    backend (RDB ktorm / MQ producer / custom) wins; this Mongo impl stays out of the way.
 *
 * [EnableMongoRepositories] is scoped to this module's [SysAuditLogRepository] package only — we
 * do not want to broaden the scan of the host app's @SpringBootApplication just by depending on
 * this jar.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(MongoTemplate::class)
@EnableMongoRepositories(basePackageClasses = [SysAuditLogRepository::class])
open class LogAuditMongoAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(IAuditService::class)
    open fun mongoAuditService(repository: SysAuditLogRepository): IAuditService =
        MongoAuditService(repository)

    /**
     * Read-side companion. Bean-name guarded (`@Bean("mongoAuditLogReadOnlyService")` would
     * mirror the ktorm module's pattern, but type-based @ConditionalOnMissingBean is enough here
     * because the typical kudos deployment uses one audit backend; admins call the appropriate
     * impl by `@Qualifier` only when multiple back-ends coexist).
     */
    @Bean
    @ConditionalOnMissingBean(IAuditLogReadOnlyService::class)
    open fun mongoAuditLogReadOnlyService(
        mongoTemplate: org.springframework.data.mongodb.core.MongoTemplate,
    ): IAuditLogReadOnlyService = MongoAuditLogReadOnlyService(mongoTemplate)

    override fun getComponentName() = "kudos-ability-log-audit-mongo"
}
