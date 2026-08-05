package io.kudos.ms.sys.core.system.service

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.common.system.vo.request.SysSystemFormCreate
import io.kudos.ms.sys.common.system.vo.request.SysSystemFormUpdate
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysSystemService
 *
 * Test data source: `SysSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysSystemService: ISysSystemService

    @Test
    fun getSystemFromCache_and_getAllSystemsFromCache() {
        val code = "svc-system-test-1_6368"
        val cacheItem = sysSystemService.getSystemFromCache(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem.code)

        val allSystems = sysSystemService.getAllSystemsFromCache()
        assertTrue(allSystems.any { it.code == code })
    }

    @Test
    fun getSubSystemsFromCache() {
        val systemCode = "svc-system-test-1_6368"
        val subSystems = sysSystemService.getSubSystemsFromCache(systemCode)
        assertTrue(subSystems.any { it.code == "svc-subsystem-test-system-1_6368" })
    }

    @Test
    fun getSystemsExcludeSubSystemFromCache() {
        val rootCode = "svc-system-test-1_6368"
        val subCode = "svc-subsystem-test-system-1_6368"
        val list = sysSystemService.getSystemsExcludeSubSystemFromCache()
        assertTrue(list.any { it.code == rootCode })
        assertFalse(list.any { it.code == subCode })
        assertTrue(list.all { !it.subSystem })
    }

    @Test
    fun updateActive() {
        val code = "svc-system-test-1_6368"
        val success = sysSystemService.updateActive(code, false)
        assertTrue(success)

        // After update, can update back to true again to verify the write path works
        val success2 = sysSystemService.updateActive(code, true)
        assertTrue(success2)
    }

    private val rootCode = "svc-system-test-1_6368"
    private val subCode = "svc-subsystem-test-system-1_6368"

    /** get(code, SysSystemCacheEntry::class) delegates to the hash cache. */
    @Test
    fun get_withCacheEntryReturnType_delegatesToCache() {
        val fromGet = sysSystemService.get(rootCode, SysSystemCacheEntry::class)
        val fromCache = sysSystemService.getSystemFromCache(rootCode)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.code, fromGet.code)
    }

    /** get(code) with the default (PO) return type goes through the super path. */
    @Test
    fun get_withDefaultReturnType_returnsPo() {
        val po = sysSystemService.get(rootCode)
        assertNotNull(po)
        assertEquals(rootCode, po.code)
    }

    /** getFullSystemTree builds a tree where the root has the sub-system as a child. */
    @Test
    fun getFullSystemTree() {
        val tree = sysSystemService.getFullSystemTree()
        val root = assertNotNull(tree.find { it.id == rootCode })
        assertTrue(root.children.any { it._getId() == subCode })
    }

    /** getActiveSubSystemCodes returns only active sub-systems. */
    @Test
    fun getActiveSubSystemCodes() {
        val codes = sysSystemService.getActiveSubSystemCodes()
        assertTrue(codes.contains(subCode))
        assertFalse(codes.contains(rootCode))
    }

    /** getActiveSystemCodes returns only active non-sub-systems. */
    @Test
    fun getActiveSystemCodes() {
        val codes = sysSystemService.getActiveSystemCodes()
        assertTrue(codes.contains(rootCode))
        assertFalse(codes.contains(subCode))
    }

    /** insert publishes the inserted event; the new code is retrievable. */
    @Test
    fun insert() {
        val code = "svc-system-insert-test_6368"
        val returned = sysSystemService.insert(
            SysSystemFormCreate(
                code = code,
                name = "insert-name",
                subSystem = false,
                parentCode = null,
                remark = null,
            )
        )
        assertEquals(code, returned)
        assertNotNull(sysSystemService.get(code))
    }

    /** update succeeds for an existing code and rejects unsupported payloads. */
    @Test
    fun update() {
        val success = sysSystemService.update(
            SysSystemFormUpdate(
                code = rootCode,
                name = "renamed-name",
                subSystem = false,
                parentCode = null,
                remark = null,
            )
        )
        assertTrue(success)
        assertEquals("renamed-name", sysSystemService.get(rootCode)?.name)

        val ex = assertFailsWith<IllegalStateException> { sysSystemService.update(Any()) }
        assertTrue(ex.message!!.contains("Unsupported payload type when updating system"))
    }

    /** deleteById removes an existing system; deleting a missing code returns false (warn branch). */
    @Test
    fun deleteById() {
        val code = "svc-system-delete-test_6368"
        sysSystemService.insert(
            SysSystemFormCreate(code = code, name = "del-name", subSystem = false, parentCode = null, remark = null)
        )
        assertTrue(sysSystemService.deleteById(code))
        assertNull(sysSystemService.get(code))
        assertFalse(sysSystemService.deleteById("no-such-system-code"))
    }

    /** getSubSystemsFromCache returns an empty list for a system with no children (after a cache reload retry). */
    @Test
    fun getSubSystemsFromCache_noChildrenTriggersReload() {
        // subCode is a leaf sub-system: it has no children, so the first filter is empty and the
        // reload-then-refilter branch is exercised, still returning an empty list.
        val subs = sysSystemService.getSubSystemsFromCache(subCode)
        assertTrue(subs.isEmpty())
    }

    /** batchDelete removes multiple systems and reports the actual count. */
    @Test
    fun batchDelete() {
        val c1 = "svc-system-batch-del-1_6368"
        val c2 = "svc-system-batch-del-2_6368"
        sysSystemService.insert(SysSystemFormCreate(code = c1, name = "b1", subSystem = false, parentCode = null, remark = null))
        sysSystemService.insert(SysSystemFormCreate(code = c2, name = "b2", subSystem = false, parentCode = null, remark = null))
        val count = sysSystemService.batchDelete(listOf(c1, c2))
        assertEquals(2, count)
        assertNull(sysSystemService.get(c1))
        assertNull(sysSystemService.get(c2))
    }
}
