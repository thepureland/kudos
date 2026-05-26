package io.kudos.ms.user.core.org.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * [UserOrgHashCache] unit tests (Hash cache, accessed by id, queried by tenantId).
 *
 * Coverage: single/batch fetch by id, fetching org list by tenant, full reload, sync after
 * insert/update/delete/batch delete; when local cache is enabled the second fetch returns the same
 * object reference.
 *
 * Test data: `UserOrgHashCacheTest.sql`.
 * Requires Docker to run Redis, and USER_ORG__HASH (hash=true) must be configured in sys_cache.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserOrgHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserOrgHashCache

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(UserOrgHashCache.CACHE_NAME)

    private val newOrgName = "New org name"

    private val tenant001 = "tenant-001-lVeGsiPZ"
    private val tenant002 = "tenant-002-lVeGsiPZ"
    private val orgId1 = "4637af03-1111-1111-1111-111111111111"
    private val orgId2 = "4637af03-2222-2222-2222-222222222222"

    @Test
    fun getOrgById() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getOrgById(orgId1)
        assertNotNull(item)
        assertEquals(orgId1, item.id)
        assertEquals("Tech Dept", item.name)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getOrgById(orgId1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getOrgById("no_exist_id"))
    }

    @Test
    fun getOrgsByIds() {
        cacheHandler.reloadAll(true)
        val result = cacheHandler.getOrgsByIds(listOf(orgId1, orgId2))
        assertEquals(2, result.size)
        assertNotNull(result[orgId1])
        assertNotNull(result[orgId2])
        assertEquals("Tech Dept", result[orgId1]?.name)
        assertEquals("Product Dept", result[orgId2]?.name)
        val resultAgain = cacheHandler.getOrgsByIds(listOf(orgId1, orgId2))
        if (isLocalCacheEnabled()) {
            assertSame(result[orgId1], resultAgain[orgId1])
            assertSame(result[orgId2], resultAgain[orgId2])
        }
        val partial = cacheHandler.getOrgsByIds(listOf("no_exist_id", orgId2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getOrgsByIds(emptyList()).isEmpty())
    }

    @Test
    fun getOrgsByTenantId() {
        cacheHandler.reloadAll(true)
        val list001 = cacheHandler.getOrgsByTenantId(tenant001)
        assertTrue(list001.size >= 7, "tenant-001 should have at least 7 orgs")
        assertTrue(list001.any { it.id == orgId1 }, "Should include Tech Dept")
        assertTrue(list001.any { it.id == orgId2 }, "Should include Product Dept")
        val list002 = cacheHandler.getOrgsByTenantId(tenant002)
        assertEquals(1, list002.size, "tenant-002 should have 1 org (headquarters)")
        assertEquals("4637af03-9999-9999-9999-999999999999", list002.first().id)
        val listEmpty = cacheHandler.getOrgsByTenantId("no_exist_tenant")
        assertTrue(listEmpty.isEmpty())
        val listAgain = cacheHandler.getOrgsByTenantId(tenant001)
        assertEquals(list001.size, listAgain.size)
        assertEquals(list001.map { it.id }.toSet(), listAgain.map { it.id }.toSet())
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        cacheHandler.syncOnInsert(id)
        val item = cacheHandler.getOrgById(id)
        assertNotNull(item)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getOrgById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val success = userOrgDao.updateProperties(orgId2, mapOf(UserOrg::name.name to newOrgName))
        assertTrue(success)
        cacheHandler.syncOnUpdate(orgId2)
        assertEquals(newOrgName, cacheHandler.getOrgById(orgId2)?.name)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        val count = userOrgDao.batchDelete(listOf(id))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(id)
        assertNull(cacheHandler.getOrgById(id))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = userOrgDao.batchDelete(ids)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(ids)
        assertNull(cacheHandler.getOrgById(id1))
        assertNull(cacheHandler.getOrgById(id2))
    }

    private fun insertNewRecordToDb(): String {
        val userOrg = UserOrg().apply {
            name = "Test org_${System.currentTimeMillis()}"
            tenantId = tenant001
            orgTypeDictCode = "ORG_TYPE_DEFAULT"
            active = true
        }
        return userOrgDao.insert(userOrg)
    }
}
