package io.kudos.ms.sys.core.accessrule.cache
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * [SysAccessRuleHashCache] 单元测试：按 id、按 systemCode+tenantKey（含平台级空串）查询与全量刷新。
 *
 * 测试数据来源：`SysAccessRuleHashCacheTest.sql`；`SYS_ACCESS_RULE__HASH` 由 Flyway `V1.0.0.20__sys_cache_access_rule_hash.sql` 注册。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cache: SysAccessRuleHashCache

    @Resource
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    override fun getTestDataSqlPath(): String = "sql/h2/accessrule/cache/SysAccessRuleHashCacheTest.sql"

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysAccessRuleHashCache.CACHE_NAME)

    @Test
    fun getAccessRuleById() {
        cache.reloadAll(true)
        val id = "a2000000-0000-0000-0000-000000000101"
        val e1 = cache.getAccessRuleById(id)
        assertNotNull(e1)
        assertEquals("tenant-ar-hash-1", e1.tenantId)
        assertEquals("sys-ar-hash-sub-a", e1.systemCode)
        val e2 = cache.getAccessRuleById(id)
        if (isLocalCacheEnabled()) assertSame(e1, e2)
        assertNull(cache.getAccessRuleById("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun getAccessRuleBySystemCodeAndTenantKey_withTenant() {
        cache.reloadAll(true)
        val e1 = cache.getAccessRuleBySystemCodeAndTenantId("sys-ar-hash-sub-a", "tenant-ar-hash-1")
        assertNotNull(e1)
        assertEquals("a2000000-0000-0000-0000-000000000101", e1.id)
        val e2 = cache.getAccessRuleBySystemCodeAndTenantId("sys-ar-hash-sub-a", "tenant-ar-hash-1")
        assertEquals(e1.id, e2?.id)
        assertEquals(e1.systemCode, e2?.systemCode)
        assertEquals(e1.tenantId, e2?.tenantId)
    }

    @Test
    fun getAccessRuleBySystemCodeAndTenantKey_platformBlankTenantId() {
        cache.reloadAll(true)
        val e1 = cache.getAccessRuleBySystemCodeAndTenantId("sys-ar-hash-platform-x", "")
        assertNotNull(e1)
        assertEquals("", e1.tenantId)
        assertEquals("a2000000-0000-0000-0000-000000000102", e1.id)
    }

    @Test
    fun getAccessRuleById_requireNonBlank() {
        assertFailsWith<IllegalArgumentException> { cache.getAccessRuleById("") }
    }

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        assertNotNull(sysAccessRuleDao.fetchCacheEntryById("a2000000-0000-0000-0000-000000000101"))
        val fromCache = cache.getAccessRuleBySystemCodeAndTenantId("sys-ar-hash-sub-a", "tenant-ar-hash-1")
        assertNotNull(fromCache)
    }
}
