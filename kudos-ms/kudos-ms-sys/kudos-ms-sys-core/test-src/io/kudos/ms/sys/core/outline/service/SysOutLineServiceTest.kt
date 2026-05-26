package io.kudos.ms.sys.core.outline.service

import io.kudos.ms.sys.core.outline.cache.OutLineBySystemAndTenantCache
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for SysOutLineService
 *
 * Test data source: `SysOutLineServiceTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysOutLineServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysOutLineService: ISysOutLineService

    @Resource
    private lateinit var outLineBySystemAndTenantCache: OutLineBySystemAndTenantCache

    private val seededIdPlatform = "30000000-0000-0000-0000-000000001001"
    private val seededIdTenant = "30000000-0000-0000-0000-000000001002"
    private val seededIdInactive = "30000000-0000-0000-0000-000000001003"
    private val seededSystemCode = "sys-outline-svc-test"
    private val seededTenantId = "30000000-0000-0000-0000-000000002001"

    /** Fetch an entity via `get(id)`; the id matches. */
    @Test
    fun get_byId_entity() {
        val po = sysOutLineService.get(seededIdPlatform)
        assertNotNull(po)
        assertEquals(seededIdPlatform, po.id)
        assertEquals("example.com", po.host)
        assertNull(po.tenantId) // platform-level
    }

    /** Platform-level (tenantId=null) list read: only hits active rows where tenant_id IS NULL. */
    @Test
    fun listActiveOutLines_platformLevel() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = null)
        assertTrue(list.any { it.id == seededIdPlatform && it.host == "example.com" })
        // Should not include tenant-level or inactive rows
        assertTrue(list.none { it.id == seededIdTenant })
        assertTrue(list.none { it.id == seededIdInactive })
    }

    /** Tenant-level list: only hits active rows matching the tenantId. */
    @Test
    fun listActiveOutLines_tenantLevel() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = seededTenantId)
        assertTrue(list.any { it.id == seededIdTenant })
        assertTrue(list.none { it.id == seededIdPlatform })
    }

    /** Inactive rules should not appear in cache results. */
    @Test
    fun listActiveOutLines_excludesInactive() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = null)
        assertTrue(list.none { it.id == seededIdInactive })
    }

    /** updateActive: after disabling, the cache should no longer hit; after re-enabling, it hits again. */
    @Test
    fun updateActive_syncsCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.id == seededIdPlatform })

        assertTrue(sysOutLineService.updateActive(seededIdPlatform, false))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).none { it.id == seededIdPlatform })

        assertTrue(sysOutLineService.updateActive(seededIdPlatform, true))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.id == seededIdPlatform })
    }

    /** updateActive returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysOutLineService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** Returns false when deleting a non-existent primary key. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysOutLineService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** insert + deleteById sync the cache. */
    @Test
    fun insert_and_deleteById_syncCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(8)
        val host = "svc-insert-$unique.example.com"

        val id = sysOutLineService.insert(
            SysOutLine().apply {
                name = "outline-svc-insert-$unique"
                this.host = host
                port = 8080
                protocol = "https"
                systemCode = seededSystemCode
                tenantId = null
                active = true
                builtIn = false
            }
        )
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, null)
        assertTrue(list.any { it.host == host })

        assertTrue(sysOutLineService.deleteById(id))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).none { it.host == host })
    }

    /** Batch delete syncs the cache. */
    @Test
    fun batchDelete_syncCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val u1 = UUID.randomUUID().toString().take(8)
        val u2 = UUID.randomUUID().toString().take(8)
        val host1 = "svc-bd1-$u1.example.com"
        val host2 = "svc-bd2-$u2.example.com"
        val id1 = sysOutLineService.insert(SysOutLine().apply {
            name = "bd1-$u1"; this.host = host1; port = 8080; protocol = "https"
            systemCode = seededSystemCode; tenantId = null; active = true; builtIn = false
        })
        val id2 = sysOutLineService.insert(SysOutLine().apply {
            name = "bd2-$u2"; this.host = host2; port = 8080; protocol = "https"
            systemCode = seededSystemCode; tenantId = null; active = true; builtIn = false
        })
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.host == host1 })
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.host == host2 })

        assertEquals(2, sysOutLineService.batchDelete(listOf(id1, id2)))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val after = sysOutLineService.listActiveOutLines(seededSystemCode, null)
        assertTrue(after.none { it.host == host1 })
        assertTrue(after.none { it.host == host2 })
    }
}
