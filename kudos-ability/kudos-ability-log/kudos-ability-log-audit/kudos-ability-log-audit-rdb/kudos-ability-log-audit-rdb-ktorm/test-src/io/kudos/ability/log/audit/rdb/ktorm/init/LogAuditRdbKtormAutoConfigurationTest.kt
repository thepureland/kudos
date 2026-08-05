package io.kudos.ability.log.audit.rdb.ktorm.init

import io.kudos.ability.log.audit.rdb.ktorm.service.RdbKtormAuditLogReadOnlyService
import io.kudos.ability.log.audit.rdb.ktorm.service.RdbKtormAuditService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Pure unit test for [LogAuditRdbKtormAutoConfiguration].
 *
 * Coverage:
 *  - getComponentName returns the module artifact name (used by the component initializer banner)
 *  - both bean factory methods produce instances of the module's own implementations
 *    (the Spring-container wiring path — @ConditionalOnProperty / @ConditionalOnMissingBean — is
 *    exercised end-to-end by the sibling service tests that inject the beans via @Resource)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class LogAuditRdbKtormAutoConfigurationTest {

    private val configuration = LogAuditRdbKtormAutoConfiguration()

    @Test
    fun getComponentName_returnsModuleArtifactName() {
        assertEquals("kudos-ability-log-audit-rdb-ktorm", configuration.getComponentName())
    }

    @Test
    fun rdbKtormAuditService_factoryReturnsKtormImplementation() {
        assertIs<RdbKtormAuditService>(configuration.rdbKtormAuditService())
    }

    @Test
    fun rdbKtormAuditLogReadOnlyService_factoryReturnsKtormImplementation() {
        assertIs<RdbKtormAuditLogReadOnlyService>(configuration.rdbKtormAuditLogReadOnlyService())
    }
}
