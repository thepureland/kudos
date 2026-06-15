package io.kudos.ability.log.audit.mongo.init

import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import io.kudos.ability.log.audit.mongo.service.MongoAuditLogReadOnlyService
import io.kudos.ability.log.audit.mongo.service.MongoAuditService
import org.mockito.Mockito.mock
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for [LogAuditMongoAutoConfiguration]:
 * - `mongoAuditService(repository)` builds a [MongoAuditService].
 * - `mongoAuditLogReadOnlyService(template)` builds a [MongoAuditLogReadOnlyService].
 * - [LogAuditMongoAutoConfiguration.getComponentName] reports the module name used by the
 *   kudos component-initializer banner.
 *
 * Bean *conditions* (@ConditionalOnClass / @ConditionalOnMissingBean by name) are container
 * concerns; the factory methods themselves are plain functions and are verified directly here
 * with mocked collaborators — no Mongo or Spring context needed.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class LogAuditMongoAutoConfigurationTest {

    private val config = LogAuditMongoAutoConfiguration()

    @Test
    fun mongoAuditService_returnsMongoBackedImplementation() {
        val service = config.mongoAuditService(mock(SysAuditLogRepository::class.java))
        assertIs<MongoAuditService>(service)
    }

    @Test
    fun mongoAuditLogReadOnlyService_returnsMongoBackedImplementation() {
        val service = config.mongoAuditLogReadOnlyService(mock(MongoTemplate::class.java))
        assertIs<MongoAuditLogReadOnlyService>(service)
    }

    @Test
    fun getComponentName_reportsModuleName() {
        assertEquals("kudos-ability-log-audit-mongo", config.getComponentName())
    }
}
