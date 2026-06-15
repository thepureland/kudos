package io.kudos.ability.log.audit.rdb.clickhouse.init

import io.kudos.ability.log.audit.rdb.clickhouse.service.RdbClickhouseAuditLogReadOnlyService
import io.kudos.ability.log.audit.rdb.clickhouse.service.RdbClickhouseAuditService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

/**
 * Unit tests for [LogAuditRdbClickhouseAutoConfiguration]:
 * - `clickhouseAuditService()` builds a [RdbClickhouseAuditService].
 * - `clickhouseAuditLogReadOnlyService()` builds a [RdbClickhouseAuditLogReadOnlyService].
 * - [LogAuditRdbClickhouseAutoConfiguration.getComponentName] reports the module name used by
 *   the kudos component-initializer banner.
 *
 * Bean *conditions* (@ConditionalOnProperty / @ConditionalOnMissingBean by name) are container
 * concerns; the factory methods themselves are plain functions and are verified directly here —
 * no ClickHouse or Spring context needed. (The container-level wiring is implicitly proven by the
 * two service integration tests in this module, which @Resource-inject both beans.)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class LogAuditRdbClickhouseAutoConfigurationTest {

    private val config = LogAuditRdbClickhouseAutoConfiguration()

    @Test
    fun clickhouseAuditService_returnsClickhouseBackedImplementation() {
        assertIs<RdbClickhouseAuditService>(config.clickhouseAuditService())
    }

    @Test
    fun clickhouseAuditService_returnsFreshInstancePerCall() {
        // The factory is not memoizing — singleton-ness is the container's job, not the method's.
        assertNotSame(config.clickhouseAuditService(), config.clickhouseAuditService())
    }

    @Test
    fun clickhouseAuditLogReadOnlyService_returnsClickhouseBackedImplementation() {
        assertIs<RdbClickhouseAuditLogReadOnlyService>(config.clickhouseAuditLogReadOnlyService())
    }

    @Test
    fun getComponentName_reportsModuleName() {
        assertEquals("kudos-ability-log-audit-rdb-clickhouse", config.getComponentName())
    }
}
