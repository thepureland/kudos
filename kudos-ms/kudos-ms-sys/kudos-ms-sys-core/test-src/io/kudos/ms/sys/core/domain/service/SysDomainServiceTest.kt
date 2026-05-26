package io.kudos.ms.sys.core.domain.service

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.common.domain.vo.response.SysDomainDetail
import io.kudos.ms.sys.core.domain.cache.DomainByNameCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import io.kudos.ms.sys.core.domain.service.iservice.ISysDomainService
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
 * junit test for SysDomainService
 *
 * Test data source: `SysDomainServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDomainServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDomainService: ISysDomainService

    @Resource
    private lateinit var domainByNameCache: DomainByNameCache

    @Resource
    private lateinit var tenantByIdCache: TenantByIdCache

    private val seededId = "20000000-0000-0000-0000-000000001461"
    private val seededTenantId = "20000000-0000-0000-0000-000000001461"
    private val seededDomainName = "svc-domain-test-1.com"
    private val seededSystemCode = "svc-system-domain-test-1"

    /** Fetch entity via `get(id)`; the id matches. */
    @Test
    fun get_byId_entity() {
        val row = sysDomainService.get(seededId)
        assertNotNull(row)
        assertEquals(seededId, row.id)
    }

    /** `get(id, SysDomainCacheEntry::class)` maps via DAO to the cache VO; fields match the seed data. */
    @Test
    fun get_withCacheEntryReturnType_usesDao() {
        val entry = sysDomainService.get(seededId, SysDomainCacheEntry::class)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(seededDomainName, entry.domain)
        assertEquals(seededSystemCode, entry.systemCode)
        assertEquals(seededTenantId, entry.tenantId)
    }

    /** Read active domains from the by-name cache; after a full reload, contents match the DB. */
    @Test
    fun getDomainFromCache_byName() {
        domainByNameCache.reloadAll(clear = true)
        val entry = sysDomainService.getDomainFromCache(seededDomainName)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(seededDomainName, entry.domain)
    }

    /** For active domains, the by-id [SysDomainCacheEntry] has the same primary key as the by-name cache entry. */
    @Test
    fun getDomainFromCache_matchesGetByIdForActiveRow() {
        domainByNameCache.reloadAll(clear = true)
        val byId = sysDomainService.get(seededId, SysDomainCacheEntry::class)
        val byName = sysDomainService.getDomainFromCache(seededDomainName)
        assertNotNull(byId)
        assertNotNull(byName)
        assertEquals(byId.id, byName.id)
    }

    /** [SysDomainDetail] is enriched with the tenant name. */
    @Test
    fun getDetail_tenantNamePopulated() {
        tenantByIdCache.reloadAll(clear = true)
        val detail = sysDomainService.get(seededId, SysDomainDetail::class)
        assertNotNull(detail)
        assertEquals(seededTenantId, detail.tenantId)
        assertEquals("svc-tenant-domain-test-1", detail.tenantName)
    }

    /** Query list rows by tenant id. */
    @Test
    fun getDomainsByTenantId() {
        val domains = sysDomainService.getDomainsByTenantId(seededTenantId)
        assertTrue(domains.any { it.domain == seededDomainName })
    }

    /** Query list rows by system code. */
    @Test
    fun getDomainsBySystemCode() {
        val domains = sysDomainService.getDomainsBySystemCode(seededSystemCode)
        assertTrue(domains.any { it.systemCode == seededSystemCode })
    }

    /** After disabling, the by-name cache should miss; after re-enabling, it should hit again. */
    @Test
    fun updateActive_syncsDomainByNameCache() {
        domainByNameCache.reloadAll(clear = true)
        assertNotNull(sysDomainService.getDomainFromCache(seededDomainName))

        assertTrue(sysDomainService.updateActive(seededId, false))
        domainByNameCache.reloadAll(clear = true)
        assertNull(sysDomainService.getDomainFromCache(seededDomainName))

        assertTrue(sysDomainService.updateActive(seededId, true))
        domainByNameCache.reloadAll(clear = true)
        assertNotNull(sysDomainService.getDomainFromCache(seededDomainName))
    }

    /** `updateActive` returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDomainService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** Returns false when deleting a non-existent primary key. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDomainService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** After batch delete, the by-name cache no longer hits the deleted domains. */
    @Test
    fun batchDelete_syncCache() {
        domainByNameCache.reloadAll(clear = true)
        val u1 = UUID.randomUUID().toString().take(8)
        val u2 = UUID.randomUUID().toString().take(8)
        val name1 = "svc-domain-svc-bd1-$u1.com"
        val name2 = "svc-domain-svc-bd2-$u2.com"
        val id1 = sysDomainService.insert(
            SysDomain().apply {
                domain = name1
                systemCode = seededSystemCode
                tenantId = seededTenantId
                active = true
                builtIn = false
            }
        )
        val id2 = sysDomainService.insert(
            SysDomain().apply {
                domain = name2
                systemCode = seededSystemCode
                tenantId = seededTenantId
                active = true
                builtIn = false
            }
        )
        domainByNameCache.reloadAll(clear = true)
        assertNotNull(sysDomainService.getDomainFromCache(name1))
        assertNotNull(sysDomainService.getDomainFromCache(name2))

        assertEquals(2, sysDomainService.batchDelete(listOf(id1, id2)))
        domainByNameCache.reloadAll(clear = true)
        assertNull(sysDomainService.getDomainFromCache(name1))
        assertNull(sysDomainService.getDomainFromCache(name2))
    }

    /** After inserting an active domain, it is readable from the by-name cache; after deleting by id, the cache no longer hits. */
    @Test
    fun insert_and_deleteById_syncCache() {
        domainByNameCache.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(8)
        val domainName = "svc-domain-svc-insert-$unique.com"

        val id = sysDomainService.insert(
            SysDomain().apply {
                domain = domainName
                systemCode = seededSystemCode
                tenantId = seededTenantId
                active = true
                builtIn = false
                remark = "SysDomainServiceTest insert"
            }
        )
        domainByNameCache.reloadAll(clear = true)
        assertNotNull(sysDomainService.getDomainFromCache(domainName))

        assertTrue(sysDomainService.deleteById(id))
        domainByNameCache.reloadAll(clear = true)
        assertNull(sysDomainService.getDomainFromCache(domainName))
    }
}
