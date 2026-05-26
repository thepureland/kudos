package io.kudos.ms.sys.core.outline.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for OutLineBySystemAndTenantCache
 *
 * Test data source: `OutLineBySystemAndTenantCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class OutLineBySystemAndTenantCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: OutLineBySystemAndTenantCache

    @Resource
    private lateinit var dao: SysOutLineDao

    private val systemCode = "sys-outline-cache-test"
    private val tenantId = "30000000-0000-0000-0000-000000003001"

    /** After reloadAll, the platform-level list can be retrieved by (systemCode, null). */
    @Test
    fun reloadAll_thenListPlatform() {
        cacheHandler.reloadAll(clear = true)
        val list = cacheHandler.listOutLines(systemCode, null)
        assertTrue(list.any { it.host == "platform.example.com" })
        assertTrue(list.none { it.host == "tenant.example.com" })
        // Should not include active=false rows
        assertTrue(list.none { it.host == "inactive.example.com" })
    }

    /** After reloadAll, the tenant-level list can be retrieved by (systemCode, tenantId). */
    @Test
    fun reloadAll_thenListTenant() {
        cacheHandler.reloadAll(clear = true)
        val list = cacheHandler.listOutLines(systemCode, tenantId)
        assertTrue(list.any { it.host == "tenant.example.com" })
        assertTrue(list.none { it.host == "platform.example.com" })
    }

    /** After refreshDimension, the underlying KV store is cleared and the next read refills on demand. */
    @Test
    fun refreshDimension_evictsAndReloads() {
        cacheHandler.reloadAll(clear = true)
        val before = cacheHandler.listOutLines(systemCode, null)
        assertTrue(before.any { it.host == "platform.example.com" })

        cacheHandler.refreshDimension(systemCode, null)
        // Still retrievable (refilled on write or on the next read)
        val after = cacheHandler.listOutLines(systemCode, null)
        assertTrue(after.any { it.host == "platform.example.com" })
    }

    /** Insert directly via dao and after refreshDimension the new row is hit. */
    @Test
    fun refreshDimension_picksUpNewRowFromDb() {
        cacheHandler.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(8)
        val newHost = "cache-new-$unique.example.com"

        dao.insert(SysOutLine().apply {
            name = "cache-test-$unique"
            host = newHost
            port = 443
            protocol = "https"
            this.systemCode = this@OutLineBySystemAndTenantCacheTest.systemCode
            tenantId = null
            active = true
            builtIn = false
        })

        cacheHandler.refreshDimension(systemCode, null)
        val list = cacheHandler.listOutLines(systemCode, null)
        assertTrue(list.any { it.host == newHost })

        // Verify via the low-level KV API that the underlying key also hits
        val key = OutLineSystemTenantKey.compositeKey(systemCode, null)
        @Suppress("UNCHECKED_CAST")
        val raw = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as? List<SysOutLineCacheEntry>
        assertNotNull(raw)
        assertEquals(true, raw.any { it.host == newHost })
    }

    /** After a hit, reloadAll(clear=true) resets the contents. */
    @Test
    fun reloadAll_clearTrue_rewrites() {
        cacheHandler.reloadAll(clear = true)
        assertTrue(cacheHandler.listOutLines(systemCode, null).any { it.host == "platform.example.com" })

        // Delete an active platform-level row (host=platform.example.com), then reloadAll
        val toDelete = dao.searchAs<SysOutLineCacheEntry>().firstOrNull { it.host == "platform.example.com" }
        assertNotNull(toDelete)
        dao.deleteById(toDelete.id)

        cacheHandler.reloadAll(clear = true)
        assertNull(
            cacheHandler.listOutLines(systemCode, null).find { it.host == "platform.example.com" }
        )
    }
}
