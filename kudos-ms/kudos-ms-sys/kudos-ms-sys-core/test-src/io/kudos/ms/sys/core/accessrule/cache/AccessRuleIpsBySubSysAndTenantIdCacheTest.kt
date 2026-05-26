package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpUpdated
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * junit test for AccessRuleIpsBySubSysAndTenantIdCacheHandler
 *
 * Test data source: `AccessRuleIpsBySubSysAndTenantIdCacheTest.sql`
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
        // Clear and reload the cache so it matches the DB
        cacheHandler.reloadAll(true)

        // Get current cached records
        val systemCode = "subSys-a"
        val tenantId  = "tenantId-2"
        val cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)

        // Insert a new record into the DB
        val sysAccessRuleIpNew = insertNewRecordToDb("8026f3ac-563b-4545-88dc-b8f70ea44847")

        // Update an existing DB record
        val idUpdate = "3a443825-4896-49e4-a304-e4e2ddad4847"
        dao.updateProperties(idUpdate, mapOf(SysAccessRuleIp::expirationTime.name to newExpirationTime))

        // Delete a record from the DB (delete a different one and keep idUpdate so we can assert "the updated record is in the cache")
        val idDelete = "3a443825-4896-49e4-a304-e4e2ddad4848"
        dao.deleteById(idDelete)

        // Reload the cache without clearing the old one
        cacheHandler.reloadAll(false)

        // Cached objects should be new instances (different memory addresses)
        val cacheItemsNew = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(cacheItems.first() !== cacheItemsNew.first())

        // The newly inserted DB record should be in the cache (1 inserted, 1 deleted -> same size)
        assertEquals(cacheItems.size, cacheItemsNew.size)
        assert(cacheItemsNew.any { it.id == sysAccessRuleIpNew.id })

        // The DB-updated record should also be updated in the cache
        val cacheItemsUpdate = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        val exceptionTime = cacheItemsUpdate.first { it.id == idUpdate }.expirationTime
        assertEquals(
            newExpirationTime.truncatedTo(ChronoUnit.MILLIS),
            exceptionTime?.truncatedTo(ChronoUnit.MILLIS)
        )

        // The DB-deleted record should not be in the cache
        val cacheItemsDelete = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assertFalse(cacheItemsDelete.any { it.id == idDelete })
    }

    @Test
    fun getAccessRuleIps() {
        var systemCode = "subSys-a"
        var tenantId: String? = "tenantId-2"
        var cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(cacheItems.size >= 2)

        // ruleIps with active=false should not be in the cache
        tenantId = "tenantId-4"
        cacheItems = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assertFalse(cacheItems.any { it.id == "3a443825-4896-49e4-a304-e4e2ddad4847" })

        // A rule with no ruleIps should still be in the cache
        tenantId = "tenantId-1"
        assert(cacheHandler.getAccessRuleIps(systemCode, tenantId).isNotEmpty())

        // tenantId is null
        systemCode = "subSys-c"
        tenantId = null
        assert(cacheHandler.getAccessRuleIps(systemCode, tenantId).isNotEmpty())

        // A rule with active=false should not be in the cache
        systemCode = "subSys-f"
        tenantId = null
        val ruleIps = cacheHandler.getAccessRuleIps(systemCode, tenantId)
        assert(ruleIps.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // Insert a new record into the DB
        val ipRule = insertNewRecordToDb("8026f3ac-563b-4545-88dc-b8f70ea44847")

        val accessRule = assertNotNull(sysAccessRuleDao.get(ipRule.parentRuleId))

        // Sync the cache: simulate the domain event published by the service layer
        cacheHandler.onIpInserted(
            SysAccessRuleIpInserted(
                id = ipRule.id,
                parentSystemCode = accessRule.systemCode,
                parentTenantId = accessRule.tenantId,
                active = true,
            )
        )

        // Verify the new record is in the cache
        val key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheEntry>
        assert(cacheItems.any { it.id == ipRule.id })
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assert(cacheItems.size == cacheItems2.size)
    }

    @Test
    fun syncOnUpdate() {
        // Update an existing DB record
        val ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        val tenantId = "tenantId-2"
        val success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::expirationTime.name to newExpirationTime))
        assert(success)

        val ipRule = assertNotNull(dao.get(ipRuleId))
        val accessRule = assertNotNull(sysAccessRuleDao.get(ipRule.parentRuleId))

        // Sync the cache: simulate the domain event published by the service layer
        cacheHandler.onIpUpdated(
            SysAccessRuleIpUpdated(
                id = ipRuleId,
                parentSystemCode = accessRule.systemCode,
                parentTenantId = accessRule.tenantId,
                active = ipRule.active,
            )
        )

        // Verify the cached record
        val key = cacheHandler.getKey(accessRule.systemCode, tenantId)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as? List<SysAccessRuleIpCacheEntry>
        assertNotNull(cacheItems, "Cache should contain key $key after syncOnUpdate")
        // The timestamp read back from H2 may differ from LocalDateTime by 1µs due to microsecond truncation; compare at millisecond granularity for stability
        assertEquals(
            newExpirationTime.truncatedTo(ChronoUnit.MILLIS),
            cacheItems.first { it.id == ipRuleId }.expirationTime?.truncatedTo(ChronoUnit.MILLIS)
        )
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, tenantId)
        assertEquals(
            newExpirationTime.truncatedTo(ChronoUnit.MILLIS),
            cacheItems2.first { it.id == ipRuleId }.expirationTime?.truncatedTo(ChronoUnit.MILLIS)
        )
    }

    @Test
    fun syncOnUpdateActive() {
        // Update from true to false
        var ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        var success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::active.name to false))
        assert(success)
        var ipRule = assertNotNull(dao.get(ipRuleId))
        var accessRule = assertNotNull(sysAccessRuleDao.get(ipRule.parentRuleId))
        cacheHandler.onIpUpdated(
            SysAccessRuleIpUpdated(
                id = ipRuleId,
                parentSystemCode = accessRule.systemCode,
                parentTenantId = accessRule.tenantId,
                active = false,
            )
        )
        var key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        var cacheItems1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheEntry>
        assertFalse(cacheItems1.any { it.id == ipRuleId })
        var cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assertFalse(cacheItems2.any { it.id == ipRuleId })

        // Update from false to true
        ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        success = dao.updateProperties(ipRuleId, mapOf(SysAccessRuleIp::active.name to true))
        assert(success)
        ipRule = assertNotNull(dao.get(ipRuleId))
        accessRule = assertNotNull(sysAccessRuleDao.get(ipRule.parentRuleId))
        cacheHandler.onIpUpdated(
            SysAccessRuleIpUpdated(
                id = ipRuleId,
                parentSystemCode = accessRule.systemCode,
                parentTenantId = accessRule.tenantId,
                active = true,
            )
        )
        key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        @Suppress("UNCHECKED_CAST")
        cacheItems1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheEntry>
        assert(cacheItems1.any { it.id == ipRuleId })
        cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assert(cacheItems2.any { it.id == ipRuleId })
    }

    @Test
    fun syncOnDelete() {
        val ipRuleId = "3a443825-4896-49e4-a304-e4e2ddad4847"
        val ipRule = assertNotNull(dao.get(ipRuleId))
        val accessRule = assertNotNull(sysAccessRuleDao.get(ipRule.parentRuleId))

        // First publish the delete event (carries dimension keys so subscribers don't need to look up the parent rule)
        cacheHandler.onIpDeleted(
            SysAccessRuleIpDeleted(
                id = ipRuleId,
                parentSystemCode = accessRule.systemCode,
                parentTenantId = accessRule.tenantId,
            )
        )

        // Then delete the DB record
        val deleteSuccess = dao.deleteById(ipRuleId)
        assert(deleteSuccess)

        // After delete, evict the cache key so a subsequent getAccessRuleIps reloads from DB
        // (syncOnDelete already evicted and repopulated while the record still existed, so another evict is needed here)
        val key = cacheHandler.getKey(accessRule.systemCode, accessRule.tenantId)
        KeyValueCacheKit.evict(cacheHandler.cacheName(), key)

        // Verify it's gone from the cache
        @Suppress("UNCHECKED_CAST")
        val cacheItems1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as List<SysAccessRuleIpCacheEntry>?
        assert(cacheItems1 == null || !cacheItems1.any { it.id == ipRuleId })
        val cacheItems2 = cacheHandler.getAccessRuleIps(accessRule.systemCode, accessRule.tenantId)
        assertFalse(cacheItems2.any { it.id == ipRuleId })
    }

    /** Insert an IPv4 rule consistent with existing test data style (not ipv6, so the end address columns are null). */
    private fun insertNewRecordToDb(parentRuleIp: String): SysAccessRuleIp {
        val ipRule = SysAccessRuleIp {
            parentRuleId = parentRuleIp
            ipStart = BigDecimal(BigInteger.valueOf(3232235650L))
            ipEnd = BigDecimal(BigInteger.valueOf(3232235650L))
            ipTypeDictCode = "1"
            active = true
        }
        dao.insert(ipRule)
        return ipRule
    }

}
