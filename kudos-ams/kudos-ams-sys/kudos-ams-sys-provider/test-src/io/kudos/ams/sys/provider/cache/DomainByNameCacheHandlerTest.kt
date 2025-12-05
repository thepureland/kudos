package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ams.sys.provider.dao.SysDomainDao
import io.kudos.ams.sys.provider.model.po.SysDomain
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for DomainByNameCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class DomainByNameCacheHandlerTest : CacheHandlerTestBase() {
    
    @Autowired
    private lateinit var cacheHandler: DomainByNameCacheHandler
    
    @Autowired
    private lateinit var dao: SysDomainDao

    private val newName = "newName.com"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        val cacheItem = cacheHandler.getDomain("domain1.com")

        // 插入新的记录到数据库
        val sysDomainNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "8309fe9a-8810-4a79-9cff-222222222222"
        dao.updateProperties(idUpdate, mapOf(SysDomain::domain.name to newName))

        // 从数据库中删除记录
        val idDelete = "8309fe9a-8810-4a79-9cff-333333333333"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItem1 = cacheHandler.getDomain("domain1.com")
        assert(cacheItem !== cacheItem1)

        // 数据库中新增的记录在缓存应该要存在
        val cacheItemNew = cacheHandler.getDomain(sysDomainNew.domain)
        assertNotNull(cacheItemNew)

        // 改名后，新的域名在缓存中应该要存在
        val cacheItemUpdate = cacheHandler.getDomain(newName)
        assertNotNull(cacheItemUpdate)

        // 改名后，旧的域名在缓存中应该还存在
        var cacheItemOld = cacheHandler.getDomain("domain2.com")
        assertNotNull(cacheItemOld)

        // 数据库中删除的记录在缓存中应该还存在
        var cacheItemDelete = cacheHandler.getDomain("domain3.com")
        assertNotNull(cacheItemDelete)


        // 清除旧缓存，并重载缓存
        cacheHandler.reloadAll(true)

        // 改名后，旧的域名在缓存中应该还存在
        cacheItemOld = cacheHandler.getDomain("domain2.com")
        assertNull(cacheItemOld)

        // 数据库中删除的记录在缓存中应该不存在
        cacheItemDelete = cacheHandler.getDomain("domain3.com")
        assertNull(cacheItemDelete)
    }

    @Test
    fun getDomain() {
        var cacheItem = cacheHandler.getDomain("domain1.com")
        assertNotNull(cacheItem)

        // active为false的应该没有在缓存中
        cacheItem = cacheHandler.getDomain("domain0.com")
        assertNull(cacheItem)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysDomain = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysDomain, sysDomain.id!!)

        // 验证新记录是否在缓存中
        val cacheItem = CacheKit.getValue(cacheHandler.cacheName(), sysDomain.domain)
        assertNotNull(cacheItem)
        val cacheItem2 = cacheHandler.getDomain(sysDomain.domain)
        assertNotNull(cacheItem2)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "8309fe9a-8810-4a79-9cff-444444444444"
        val success = dao.updateProperties(id, mapOf(SysDomain::domain.name to newName))
        assert(success)


        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存中的记录
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), newName) as SysDomainCacheItem?
        assertNotNull(cacheItem)
        assertEquals(newName, cacheItem!!.domain)
        cacheItem = cacheHandler.getDomain(newName)
        assertNotNull(cacheItem)
        assertEquals(newName, cacheItem!!.domain)
    }

    @Test
    fun syncOnDelete() {
        val id = "8309fe9a-8810-4a79-9cff-555555555555"
        val sysDomain = dao.get(id)!!

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(sysDomain, id)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), sysDomain.domain)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(sysDomain.domain)
        assertNull(cacheItem)
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "8309fe9a-8810-4a79-9cff-666666666666"
        val id2 = "8309fe9a-8810-4a79-9cff-777777777777"
        val ids = listOf(id1, id2)
        val domain1 = "domain6.com"
        val domain2 = "domain7.com"
        val domains = setOf(domain1, domain2)

        // 批量删除数据库中的记录
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, domains)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), domain1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(domain1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), domain2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(domain2)
        assertNull(cacheItem)
    }
    
    private fun insertNewRecordToDb(): SysDomain {
        val sysDomain = SysDomain().apply { 
            domain = "a_new_domain.com"
            portalCode = "default"
        }
        dao.insert(sysDomain)
        return sysDomain
    }

}