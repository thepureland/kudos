package io.kudos.ms.sys.core.tenant.cache
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * [SysTenantSystemHashCache] 单元测试（Hash 缓存，按 subSystemCode / tenantId 查询）。
 *
 * 覆盖：getTenantIdsBySubSystemCode、getSubSystemCodesByTenantId、全量刷新、
 * 新增/更新/删除/批量删除后同步。
 *
 * 测试数据：`sql/h2/tenant/cache/SysTenantSystemHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_TENANT_SYSTEM__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSystemHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cache: SysTenantSystemHashCache

    @Resource
    private lateinit var sysTenantSystemDao: SysTenantSystemDao

    override fun getTestDataSqlPath(): String = "sql/h2/tenant/cache/SysTenantSystemHashCacheTest.sql"

    private val systemCodeA = "subSys-a"
    private val tenantId1 = "218772a0-c053-4634-a5e5-111111118781"

    // ---------- 按子系统编码 ----------

    @Test
    fun getTenantIdsBySubSystemCode() {
        cache.reloadAll(true)
        val tenantIds = cache.getTenantIdsBySubSystemCode(systemCodeA)
        // subSys-a: tenant 111111, 333333, 444444 -> 3 个
        assertEquals(3, tenantIds.size)
        assertTrue(tenantIds.contains(tenantId1))
        assertTrue(tenantIds.contains("218772a0-c053-4634-a5e5-333333338781"))
        assertTrue(tenantIds.contains("218772a0-c053-4634-a5e5-444444448781"))
        val tenantIdsEmpty = cache.getTenantIdsBySubSystemCode("subSys-nonexistent")
        assertTrue(tenantIdsEmpty.isEmpty())
    }

    @Test
    fun getTenantIdsBySubSystemCode_requireNonBlank() {
        assertFailsWith<IllegalArgumentException> { cache.getTenantIdsBySubSystemCode("") }
    }

    // ---------- 按租户id ----------

    @Test
    fun getSubSystemCodesByTenantId() {
        cache.reloadAll(true)
        val systemCodes = cache.getSubSystemCodesByTenantId(tenantId1)
        // tenant 111111: subSys-a, subSys-b, subSys-c -> 3 个
        assertEquals(3, systemCodes.size)
        assertTrue(systemCodes.contains(systemCodeA))
        assertTrue(systemCodes.contains("subSys-b"))
        assertTrue(systemCodes.contains("subSys-c"))
    }

    @Test
    fun getSubSystemCodesByTenantId_requireNonBlank() {
        assertFailsWith<IllegalArgumentException> { cache.getSubSystemCodesByTenantId("") }
    }

    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        assertEquals(3, cache.getTenantIdsBySubSystemCode(systemCodeA).size)
        val newRel = insertNewRecordToDb()
        cache.reloadAll(false)
        assertTrue(cache.getTenantIdsBySubSystemCode(newRel.systemCode).contains(newRel.tenantId))
        assertTrue(cache.getSubSystemCodesByTenantId(newRel.tenantId).contains(newRel.systemCode))
        cache.reloadAll(true)
        assertEquals(3, cache.getTenantIdsBySubSystemCode(systemCodeA).size)
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        cache.reloadAll(true)
        val newRel = insertNewRecordToDb()
        cache.syncOnInsert(newRel.id)
        assertTrue(cache.getTenantIdsBySubSystemCode(newRel.systemCode).contains(newRel.tenantId))
        assertTrue(cache.getSubSystemCodesByTenantId(newRel.tenantId).contains(newRel.systemCode))
        cache.syncOnInsert(Any(), newRel.id)
        assertTrue(cache.getTenantIdsBySubSystemCode(newRel.systemCode).contains(newRel.tenantId))
    }

    @Test
    fun syncOnUpdate() {
        cache.reloadAll(true)
        val id = "b3846388-5e61-4b58-8fd8-aaaaaaaa8781"
        assertTrue(cache.getTenantIdsBySubSystemCode(systemCodeA).contains(tenantId1))
        cache.syncOnUpdate(id)
        assertTrue(cache.getTenantIdsBySubSystemCode(systemCodeA).contains(tenantId1))
    }

    @Test
    fun syncOnDelete() {
        cache.reloadAll(true)
        val id = "b3846388-5e61-4b58-8fd8-eeeeeeee8781"
        assertTrue(cache.getTenantIdsBySubSystemCode(systemCodeA).contains("218772a0-c053-4634-a5e5-333333338781"))
        sysTenantSystemDao.deleteById(id)
        cache.syncOnDelete(id)
        val tenantIds = cache.getTenantIdsBySubSystemCode(systemCodeA)
        assertFalse(tenantIds.contains("218772a0-c053-4634-a5e5-333333338781"))
    }

    @Test
    fun syncOnBatchDelete() {
        cache.reloadAll(true)
        val id1 = "b3846388-5e61-4b58-8fd8-ffffffff8781"
        val id2 = "b3846388-5e61-4b58-8fd8-ggggggg_5246"
        val systemCode1 = "subSys-a"
        val systemCode2 = "subSys-d"
        assertTrue(cache.getTenantIdsBySubSystemCode(systemCode1).contains("218772a0-c053-4634-a5e5-444444448781"))
        assertTrue(cache.getTenantIdsBySubSystemCode(systemCode2).contains("218772a0-c053-4634-a5e5-555555558781"))
        sysTenantSystemDao.batchDelete(listOf(id1, id2))
        cache.syncOnBatchDelete(listOf(id1, id2))
        assertFalse(cache.getTenantIdsBySubSystemCode(systemCode1).contains("218772a0-c053-4634-a5e5-444444448781"))
        assertFalse(cache.getTenantIdsBySubSystemCode(systemCode2).contains("218772a0-c053-4634-a5e5-555555558781"))
    }

    private fun insertNewRecordToDb(): SysTenantSystem {
        val rel = SysTenantSystem().apply {
            tenantId = "218772a0-c053-4634-a5e5-111111118781"
            systemCode = "subSys-new-hash-test"
        }
        rel.id = sysTenantSystemDao.insert(rel)
        return rel
    }
}
