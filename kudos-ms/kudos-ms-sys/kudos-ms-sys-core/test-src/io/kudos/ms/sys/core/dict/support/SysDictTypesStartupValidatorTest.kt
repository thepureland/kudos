package io.kudos.ms.sys.core.dict.support

import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Calls [SysDictTypesStartupValidator.validate] directly (without going through ApplicationReadyEvent),
 * verifying the missing / extras result inside [SqlTestBase][io.kudos.test.rdb.SqlTestBase]'s transaction.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictTypesStartupValidatorTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var validator: SysDictTypesStartupValidator

    companion object {
        /** Dedicated atomic_service_code for tests to avoid collisions with the shared `'sys'` seed data. */
        private const val TEST_ATOMIC_SERVICE_CODE = "sys-validator-test"
    }

    @Test
    fun `missing detects dict types declared in code but absent in DB`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // The test SQL intentionally omits ip_type and access_rule_type
        assertContains(result.missing, SysDictTypes.IP_TYPE)
        assertContains(result.missing, SysDictTypes.ACCESS_RULE_TYPE)
        assertFalse(result.isOk, "isOk should be false when missing is non-empty")
    }

    @Test
    fun `extras detects dict types in DB but absent from code constants`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // The test SQL intentionally inserts legacy_only
        assertContains(result.extras, "legacy_only")
    }

    @Test
    fun `inactive rows are not counted as present`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // deactivated_type has active=false in the SQL: it should not appear in the declared hits,
        // nor in extras (extras come from active DB rows; inactive rows are filtered out)
        assertFalse(result.extras.contains("deactivated_type"))
    }

    @Test
    fun `declared mirrors SysDictTypes constants`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // All 11 constants should be reflected
        assertEquals(11, result.declared.size, "declared should reflect every constant in SysDictTypes")
        assertTrue(result.declared.contains(SysDictTypes.DS_USE))
        assertTrue(result.declared.contains(SysDictTypes.IP_TYPE))
    }
}
