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
 * 直接调用 [SysDictTypesStartupValidator.validate]（不走 ApplicationReadyEvent），
 * 在 [SqlTestBase][io.kudos.test.rdb.SqlTestBase] 的事务内验证 missing / extras 计算结果。
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
        /** 测试用独立 atomic_service_code，避免与共享的 `'sys'` 种子数据冲突。 */
        private const val TEST_ATOMIC_SERVICE_CODE = "sys-validator-test"
    }

    @Test
    fun `missing detects dict types declared in code but absent in DB`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // 测试 SQL 故意未插入 ip_type 与 access_rule_type
        assertContains(result.missing, SysDictTypes.IP_TYPE)
        assertContains(result.missing, SysDictTypes.ACCESS_RULE_TYPE)
        assertFalse(result.isOk, "存在 missing 时 isOk 应为 false")
    }

    @Test
    fun `extras detects dict types in DB but absent from code constants`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // 测试 SQL 故意插入了 legacy_only
        assertContains(result.extras, "legacy_only")
    }

    @Test
    fun `inactive rows are not counted as present`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // deactivated_type 在 SQL 中 active=false：既不应出现在 declared 的命中里，
        // 也不应出现在 extras（因为 extras 来自 DB 的 active 行，inactive 行被过滤掉了）
        assertFalse(result.extras.contains("deactivated_type"))
    }

    @Test
    fun `declared mirrors SysDictTypes constants`() {
        val result = validator.validate(TEST_ATOMIC_SERVICE_CODE)
        // 11 个常量都应该被反射到
        assertEquals(11, result.declared.size, "declared 应反射到 SysDictTypes 全部常量")
        assertTrue(result.declared.contains(SysDictTypes.DS_USE))
        assertTrue(result.declared.contains(SysDictTypes.IP_TYPE))
    }
}
