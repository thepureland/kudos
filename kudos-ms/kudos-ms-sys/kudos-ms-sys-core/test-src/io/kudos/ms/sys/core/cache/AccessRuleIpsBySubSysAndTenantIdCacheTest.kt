package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheItem
import io.kudos.ms.sys.core.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.model.po.SysAccessRuleIp
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * junit test for AccessRuleIpsBySubSysAndTenantIdCacheHandler
 *
 * 测试数据来源：`AccessRuleIpsBySubSysAndTenantIdCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AccessRuleIpsBySubSysAndTenantIdCacheTest : RdbAndRedisCacheTestBase() {
    
    @Resource
    private lateinit var cacheHandler: AccessRuleIpsBySubSysAndTenantIdCache
    
    @Resource
    private lateinit var dao: SysAccessRuleIpDao

    @Resource
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    private val newExpirationTime = LocalDateTime.now().plus(1L, ChronoUnit.YEARS)

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        val systemCode = "subSys-a"
        val tenantId  = "tenantId-2"
        val cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)

        // 插入新的记录到数据库
        val sysAccessRuleIpNew = insertNewRecordToDb("8026f3ac-563b-4545-88dc-b8f70ea44847")

        // 更新数据库的记录
        val idUpdate = "3a443825-4896-49e4-a304-e4e2ddad4847"
        dao.updateProperties(idUpdate, mapOf(SysAccessRuleIp::expirationTime.name to newExpirationTime))

        // 从数据库中删除记录（删除另一条，保留 idUpdate 以便下面断言“更新的记录在缓存中”）
        val idDelete = "3a443825-4896-49e4-a304-e4e2ddad4848"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItemsNew = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(cacheItems.first() !== cacheItemsNew.first())

        // 数据库中新增的记录在缓存应该要存在(新增条，删除1条)
        assertEquals(cacheItems.size, cacheItemsNew.size)
        assert(cacheItemsNew.any { it.id == sysAccessRuleIpNew.id })

        // 数据库中更新的记录在缓存中应该也更新了
        val cacheItemsUpdate = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        val exceptionTime = cacheItemsUpdate.first { it.id == idUpdate }.expirationTime
        assertEquals(newExpirationTime, exceptionTime)

        // 数据库中删除的记录在缓存中应该不存在
        val cacheItemsDelete = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assertFalse(cacheItemsDelete.any { it.id == idDelete })
    }

    @Test
    fun getAccessRuleIps() {
        var systemCode = "subSys-a"
        var tenantId: String? = "tenantId-2"
        var cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(cacheItems.size >= 2)

        // active为false的ruleIp应该没有在缓存中
        tenantId = "tenantId-4"
        cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assertFalse(cacheItems.any { it.id == "3a443825-4896-49e4-a304-e4e2ddad4847" })

        // 只有rule，没有ruleIp的，也要在缓存中
        tenantId = "tenantId-1"
        assert(cacheHandler.getAccessRuleIps(systemCode, tenantId).isNotEmpty())

        // tenantId为null
        systemCode = "subSys-c"
        tenantId = null
        assert(cacheHandler.getAccessRuleIps(systemCode, tenantId).isNotEmpty())

        // active为false的rule, 应该不会在缓存中
        systemCode = "subSys-f"
        tenantId = null
        val ruleIps = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(ruleIps.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val ipRule = insertNewRecordToDb("8026f3ac-563b-4545-88dc-b8f70ea44847")

        val accessRule = sysAccessRuleDao.getAs(ipRule.parentRuleId)

        // 同步缓存
        cacheHandler.syncOnInsert(accessRule!!, ipRule.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheItem>
        assert(cacheItems.any { it.id == ipRule.id })
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assert(cacheItems.size == cacheItems2.size)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        val tenantId = "tenantId-2"
        val success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::expirationTime.name to newExpirationTime))
        assert(success)

        val ipRule = dao.getAs(ipRuleId)!!
        val accessRule = sysAccessRuleDao.getAs(ipRule.parentRuleId)

        // 同步缓存
        cacheHandler.syncOnUpdate(accessRule!!, ipRuleId)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(accessRule.systemCode, tenantId)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as? List<SysAccessRuleIpCacheItem>
        assertNotNull(cacheItems, "Cache should contain key $key after syncOnUpdate")
        assertEquals(newExpirationTime, cacheItems.first { it.id == ipRuleId }.expirationTime)
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, tenantId)
        assertEquals(newExpirationTime, cacheItems2.first { it.id == ipRuleId }.expirationTime)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        var success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::active.name to false))
        assert(success)
        var ipRule = dao.getAs(ipRuleId)!!
        var accessRule = sysAccessRuleDao.getAs(ipRule.parentRuleId)!!
        cacheHandler.syncOnUpdateActive(ipRuleId, false)
        var key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        var cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheItem>
        assertFalse(cacheItems1.any { it.id == ipRuleId })
        var cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assertFalse(cacheItems2.any { it.id == ipRuleId })

        // 由false更新为true
        ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::active.name to true))
        assert(success)
        ipRule = dao.getAs(ipRuleId)!!
        accessRule = sysAccessRuleDao.getAs(ipRule.parentRuleId)!!
        cacheHandler.syncOnUpdateActive(ipRuleId, true)
        key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheItem>
        assert(cacheItems1.any { it.id == ipRuleId })
        cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assert(cacheItems2.any { it.id == ipRuleId })
    }

    @Test
    fun syncOnDelete() {
        val ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        val ipRule = dao.getAs(ipRuleId)!!
        val accessRule = sysAccessRuleDao.getAs(ipRule.parentRuleId)!!

        // 先同步缓存（handler 需根据 ipRuleId 查库得到 parent，故需在删除前调用）
        cacheHandler.syncOnDelete(ipRuleId)

        // 再删除数据库中的记录
        val deleteSuccess = dao.deleteById(ipRuleId)
        assert(deleteSuccess)

        // 删除后踢掉该 key 的缓存，使后续 getAccessRuleIps 从 DB 重载（syncOnDelete 已 evict 并 repopulate，当时记录还在，故此处需再 evict）
        val key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        CacheKit.evict(cacheHandler.cacheName(), key)

        // 验证缓存中有没有
        @Suppress("UNCHECKED_CAST")
        val cacheItems1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheItem>?
        assert(cacheItems1 == null || !cacheItems1.any { it.id == ipRuleId })
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assertFalse(cacheItems2.any { it.id == ipRuleId })
    }
    
    private fun insertNewRecordToDb(parentRuleIp: String): SysAccessRuleIp {
        val ipRule = SysAccessRuleIp().apply {
            this.parentRuleId = parentRuleIp
            ipStart = 3232235650
            ipEnd = 3232235650
            ipTypeDictCode = "1"
        }
        dao.insert(ipRule)
        return ipRule
    }

}
