package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.service.dao.SysResourceDao
import io.kudos.ams.sys.service.model.po.SysResource
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * junit test for ResourceIdsBySubSysAndTypeCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class ResourceIdsBySubSysAndTypeCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: ResourceIdsBySubSysAndTypeCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    @Autowired
    private lateinit var dao: SysResourceDao

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 插入新的记录到数据库
        val sysResourceNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "cb76084a-ceaa-44f1-9c9d-222222222222"
        dao.updateProperties(idUpdate, mapOf(SysResource::url.name to "/a/b/new_url"))

        // 从数据库中删除记录
        val idDelete = "cb76084a-ceaa-44f1-9c9d-333333333333"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 数据库中新增的记录在缓存应该要存在
        var resIds = cacheHandler.getResourceIds(sysResourceNew.subSystemCode, sysResourceNew.resourceTypeDictCode)
        assert(resIds.contains(sysResourceNew.id))

        // 数据库中更新的记录在缓存中应该存在
        var subSystemCode = "subSys-aaa"
        var resourceTypeDictCode = "1"
        resIds = cacheHandler.getResourceIds(subSystemCode, resourceTypeDictCode)
        assert(resIds.contains(idUpdate))

        // 数据库中删除的记录在缓存中应该不存在
        subSystemCode = "subSys-a"
        resourceTypeDictCode = "1"
        resIds = cacheHandler.getResourceIds(subSystemCode, resourceTypeDictCode)
        assertFalse(resIds.contains(idDelete))
    }

    @Test
    fun getResourceIds() {
        // 存在的
        var subSystemCode = "subSys-aaa"
        var resourceTypeDictCode = "1"
        var resIds = cacheHandler.getResourceIds(subSystemCode, resourceTypeDictCode)
        assert(resIds.contains("cb76084a-ceaa-44f1-9c9d-111111111111"))

        // 不存在的
        subSystemCode = "subSys-nnn"
        resourceTypeDictCode = "1"
        resIds = cacheHandler.getResourceIds(subSystemCode, resourceTypeDictCode)
        assertEquals(0, resIds.size)

        // active为false的
        subSystemCode = "subSys-eee"
        resourceTypeDictCode = "2"
        resIds = cacheHandler.getResourceIds(subSystemCode, resourceTypeDictCode)
        assertEquals(1, resIds.size)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysResource = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysResource, sysResource.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(sysResource.subSystemCode, sysResource.resourceTypeDictCode)
        @Suppress("UNCHECKED_CAST")
        val resIds1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<String>
        assert(resIds1.contains(sysResource.id))
        val resIds2 = cacheHandler.getResourceIds(sysResource.subSystemCode, sysResource.resourceTypeDictCode)
        assert(resIds1 == resIds2)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "cb76084a-ceaa-44f1-9c9d-444444444444"
        val sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        val newUrl = "/a/b/ee"
        val success = dao.updateProperties(id, mapOf(SysResource::url.name to newUrl))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(sysResource, id, sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        @Suppress("UNCHECKED_CAST")
        val resIds1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<String>
        assert(resIds1.contains(sysResource.id))
        val resIds2 = cacheHandler.getResourceIds(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        assert(resIds1 == resIds2)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "cb76084a-ceaa-44f1-9c9d-555555555555"
        var success = dao.updateProperties(id, mapOf(SysResource::active.name to false))
        assert(success)
        var sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        cacheHandler.syncOnUpdateActive(id)
        var key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        @Suppress("UNCHECKED_CAST")
        var resIds1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<String>
        assertFalse(resIds1.contains(sysResource.id))
        var resIds2 = cacheHandler.getResourceIds(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        assert(resIds1 == resIds2)

        // 由false更新为true
        id = "cb76084a-ceaa-44f1-9c9d-000000000000"
        success = dao.updateProperties(id, mapOf(SysResource::active.name to true))
        assert(success)
        sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        cacheHandler.syncOnUpdateActive(id)
        key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        @Suppress("UNCHECKED_CAST")
        resIds1 = CacheKit.getValue(cacheHandler.cacheName(), key) as List<String>
        assert(resIds1.contains(sysResource.id))
        resIds2 = cacheHandler.getResourceIds(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        assert(resIds1 == resIds2)
    }

    @Test
    fun syncOnDelete() {
        val id = "cb76084a-ceaa-44f1-9c9d-666666666666"
        val sysResource = resourceByIdCacheHandler.getResourceById(id)!!

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id, sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        val resIds1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNull(resIds1)
        val resIds2 = cacheHandler.getResourceIds(sysResource.subSystemCode!!, sysResource.resourceTypeDictCode!!)
        assert(resIds2.isEmpty())
    }

    private fun insertNewRecordToDb(): SysResource {
        val sysResource = SysResource().apply {
            subSystemCode = "subSys-aaa"
            url = "/a/b/n"
            name = "sysResource-nnn"
            resourceTypeDictCode = "2"
        }
        dao.insert(sysResource)
        return sysResource
    }

}