package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.common.vo.domain.SysDomainCacheEntry
import io.kudos.ms.sys.common.vo.domain.response.SysDomainDetail
import io.kudos.ms.sys.core.cache.DomainByNameCache
import io.kudos.ms.sys.core.cache.TenantByIdCache
import io.kudos.ms.sys.core.model.po.SysDomain
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
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
 * 测试数据来源：`SysDomainServiceTest.sql`
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

    /** 按主键 `get(id)` 取到实体且 id 一致。 */
    @Test
    fun get_byId_entity() {
        val row = sysDomainService.get(seededId)
        assertNotNull(row)
        assertEquals(seededId, row.id)
    }

    /** `get(id, SysDomainCacheEntry::class)` 走 DAO 映射为缓存载体类型，字段与种子数据一致。 */
    @Test
    fun get_withCacheEntryReturnType_usesDao() {
        val entry = sysDomainService.get(seededId, SysDomainCacheEntry::class)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(seededDomainName, entry.domain)
        assertEquals(seededSystemCode, entry.systemCode)
        assertEquals(seededTenantId, entry.tenantId)
    }

    /** 从按名称缓存读取启用域名；全量 reload 后与库一致。 */
    @Test
    fun getDomainFromCache_byName() {
        domainByNameCache.reloadAll(clear = true)
        val entry = sysDomainService.getDomainFromCache(seededDomainName)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(seededDomainName, entry.domain)
    }

    /** 启用域名下，按 id 的 [SysDomainCacheEntry] 与按名称缓存项主键一致。 */
    @Test
    fun getDomainFromCache_matchesGetByIdForActiveRow() {
        domainByNameCache.reloadAll(clear = true)
        val byId = sysDomainService.get(seededId, SysDomainCacheEntry::class)
        val byName = sysDomainService.getDomainFromCache(seededDomainName)
        assertNotNull(byId)
        assertNotNull(byName)
        assertEquals(byId.id, byName.id)
    }

    /** 详情 [SysDomainDetail] 补充租户名称。 */
    @Test
    fun getDetail_tenantNamePopulated() {
        tenantByIdCache.reloadAll(clear = true)
        val detail = sysDomainService.get(seededId, SysDomainDetail::class)
        assertNotNull(detail)
        assertEquals(seededTenantId, detail.tenantId)
        assertEquals("svc-tenant-domain-test-1", detail.tenantName)
    }

    /** 按租户 id 条件查询列表行。 */
    @Test
    fun getDomainsByTenantId() {
        val domains = sysDomainService.getDomainsByTenantId(seededTenantId)
        assertTrue(domains.any { it.domain == seededDomainName })
    }

    /** 按系统编码条件查询列表行。 */
    @Test
    fun getDomainsBySystemCode() {
        val domains = sysDomainService.getDomainsBySystemCode(seededSystemCode)
        assertTrue(domains.any { it.systemCode == seededSystemCode })
    }

    /** 停用后按名称缓存应取不到；恢复启用后应可再次命中。 */
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

    /** `updateActive` 在主键不存在时返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDomainService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 删除不存在的主键时返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDomainService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** 批量删除后按名称缓存不再命中对应域名。 */
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

    /** 新增启用域名后可从按名称缓存读到；按 id 删除后缓存侧应不再命中。 */
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
