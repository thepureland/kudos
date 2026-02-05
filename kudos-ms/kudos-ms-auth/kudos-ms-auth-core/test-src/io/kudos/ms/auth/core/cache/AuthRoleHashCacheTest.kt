package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.auth.core.dao.AuthRoleDao
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * [AuthRoleHashCache] 单元测试（Hash 缓存，按 id 存取、按 tenantId+code 查询，不区分 active）。
 *
 * 覆盖：按 id 单条/批量获取、按租户+角色编码获取角色、全量刷新、新增/更新/删除/批量删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`sql/h2/cache/AuthRoleHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 AUTH_ROLE__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(AuthRoleHashCache.CACHE_NAME)

    private val newRoleName = "新角色名称_${System.currentTimeMillis()}"

    private val tenant001 = "tenant-001-hashRole"
    private val tenant002 = "tenant-002-hashRole"
    private val roleId1 = "ar-hash-1111-1111-1111-1111111111111"
    private val roleId2 = "ar-hash-2222-2222-2222-2222222222222"

    @Test
    fun getRoleById() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getRoleById(roleId1)
        assertNotNull(item)
        assertEquals(roleId1, item.id)
        assertEquals("ROLE_ADMIN", item.code)
        assertEquals("系统管理员", item.name)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getRoleById(roleId1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getRoleById("no_exist_id"))
    }

    @Test
    fun getRolesByIds() {
        cacheHandler.reloadAll(true)
        val result = cacheHandler.getRolesByIds(listOf(roleId1, roleId2))
        assertEquals(2, result.size)
        assertNotNull(result[roleId1])
        assertNotNull(result[roleId2])
        assertEquals("ROLE_ADMIN", result[roleId1]?.code)
        assertEquals("ROLE_USER", result[roleId2]?.code)
        val resultAgain = cacheHandler.getRolesByIds(listOf(roleId1, roleId2))
        if (isLocalCacheEnabled()) {
            assertSame(result[roleId1], resultAgain[roleId1])
            assertSame(result[roleId2], resultAgain[roleId2])
        }
        val partial = cacheHandler.getRolesByIds(listOf("no_exist_id", roleId2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getRolesByIds(emptyList()).isEmpty())
    }

    @Test
    fun getRoleByTenantIdAndRoleCode() {
        cacheHandler.reloadAll(true)
        val admin = cacheHandler.getRoleByTenantIdAndRoleCode(tenant001, "ROLE_ADMIN")
        assertNotNull(admin)
        assertEquals(roleId1, admin.id)
        assertEquals("ROLE_ADMIN", admin.code)
        val user = cacheHandler.getRoleByTenantIdAndRoleCode(tenant001, "ROLE_USER")
        assertNotNull(user)
        assertEquals(roleId2, user.id)
        val tenant2Admin = cacheHandler.getRoleByTenantIdAndRoleCode(tenant002, "ROLE_ADMIN")
        assertNotNull(tenant2Admin)
        assertEquals("ar-hash-5555-5555-5555-5555555555555", tenant2Admin.id)
        assertNull(cacheHandler.getRoleByTenantIdAndRoleCode(tenant001, "ROLE_NO_EXIST"))
        assertNull(cacheHandler.getRoleByTenantIdAndRoleCode("no_exist_tenant", "ROLE_ADMIN"))
        val adminAgain = cacheHandler.getRoleByTenantIdAndRoleCode(tenant001, "ROLE_ADMIN")
        if (isLocalCacheEnabled()) assertSame(admin, adminAgain)
    }

    @Test
    fun getRoleByTenantIdAndRoleCodeInactiveAlsoReturned() {
        cacheHandler.reloadAll(true)
        val inactive = cacheHandler.getRoleByTenantIdAndRoleCode(tenant001, "ROLE_TEST")
        assertNotNull(inactive, "不区分 active 时，inactive 角色也应能按 tenantId+code 查到")
        assertEquals("ar-hash-4444-4444-4444-4444444444444", inactive.id)
        assertEquals(false, inactive.active)
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        cacheHandler.syncOnInsert(id)
        val item = cacheHandler.getRoleById(id)
        assertNotNull(item)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getRoleById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val success = authRoleDao.updateProperties(roleId2, mapOf(AuthRole::name.name to newRoleName))
        assertTrue(success)
        cacheHandler.syncOnUpdate(roleId2)
        assertEquals(newRoleName, cacheHandler.getRoleById(roleId2)?.name)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        val count = authRoleDao.batchDelete(listOf(id))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(id)
        assertNull(cacheHandler.getRoleById(id))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = authRoleDao.batchDelete(ids)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(ids)
        assertNull(cacheHandler.getRoleById(id1))
        assertNull(cacheHandler.getRoleById(id2))
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val authRole = AuthRole.Companion().apply {
            code = "TEST_ROLE_${timestamp}"
            name = "测试角色_${timestamp}"
            tenantId = tenant001
            subsysCode = "default"
            active = true
        }
        return authRoleDao.insert(authRole)
    }
}
