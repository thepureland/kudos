package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.provider.dao.SysResourceDao
import io.kudos.ams.sys.provider.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for ResourceIdBySubSysAndUrlCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdBySubSysAndUrlCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdBySubSysAndUrlCacheHandler

    @Resource
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    @Resource
    private lateinit var dao: SysResourceDao

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        var subSystemCode = "subSys-a"
        var url = "/a/b/c"
        val resId = cacheHandler.getResourceId(subSystemCode, url)

        // 插入新的记录到数据库
        val sysResourceNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "bb76084a-ceaa-44f1-9c9d-222222222222"
        dao.updateProperties(idUpdate, mapOf(SysResource::url.name to "/a/b/new_url"))

        // 从数据库中删除记录
        val idDelete = "bb76084a-ceaa-44f1-9c9d-333333333333"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的不变
        assert(resId == cacheHandler.getResourceId(subSystemCode, url))

        // 数据库中新增的记录在缓存应该要存在
        assertNotNull(cacheHandler.getResourceId(sysResourceNew.subSystemCode, sysResourceNew.url!!))

        // 数据库中更新的记录在缓存中应该存在
        subSystemCode = "subSys-a"
        url = "/a/b/d"
        assertNotNull(cacheHandler.getResourceId(subSystemCode, url))

        // 数据库中删除的记录在缓存中应该还存在
        subSystemCode = "subSys-a"
        url = "/a/b/e"
        assertNotNull(cacheHandler.getResourceId(subSystemCode, url))

        // 清除旧缓存，并重载缓存
        cacheHandler.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在
        subSystemCode = "subSys-a"
        url = "/a/b/e"
        assertNull(cacheHandler.getResourceId(subSystemCode, url))
    }

    @Test
    fun getResourceId() {
        var subSystemCode = "subSys-a"
        var url = "/a/b/c"
        assertNotNull(cacheHandler.getResourceId(subSystemCode, url))

        // active为false的应该没有在缓存中
        subSystemCode = "subSys-e"
        url = "/a/b/k"
        assertNull(cacheHandler.getResourceId(subSystemCode, url))

        // url为null的应该没有在缓存中
        subSystemCode = "subSys-e"
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), cacheHandler.getKey(subSystemCode, null)))
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysResource = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysResource, sysResource.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(sysResource.subSystemCode, sysResource.url)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getResourceId(sysResource.subSystemCode, sysResource.url!!))
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "bb76084a-ceaa-44f1-9c9d-444444444444"
        val sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        val oldUrl = sysResource.url
        val newUrl = "/a/b/ee"
        val success = dao.updateProperties(id, mapOf(SysResource::url.name to newUrl))
        assert(success)
        sysResource.url = newUrl

        // 同步缓存
        cacheHandler.syncOnUpdate(sysResource, id, oldUrl)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.url)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getResourceId(sysResource.subSystemCode!!, sysResource.url!!))
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "bb76084a-ceaa-44f1-9c9d-555555555555"
        var success = dao.updateProperties(id, mapOf(SysResource::active.name to false))
        assert(success)
        var sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        cacheHandler.syncOnUpdateActive(id, false)
        var key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.url)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getResourceId(sysResource.subSystemCode!!, sysResource.url!!))

        // 由false更新为true
        id = "bb76084a-ceaa-44f1-9c9d-000000000000"
        success = dao.updateProperties(id, mapOf(SysResource::active.name to true))
        assert(success)
        sysResource = resourceByIdCacheHandler.getResourceById(id)!!
        cacheHandler.syncOnUpdateActive(id, true)
        key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.url)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
    }

    @Test
    fun syncOnDelete() {
        val id = "bb76084a-ceaa-44f1-9c9d-666666666666"
        val sysResource = resourceByIdCacheHandler.getResourceById(id)!!

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id, sysResource.subSystemCode!!, sysResource.url)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(sysResource.subSystemCode!!, sysResource.url)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getResourceId(sysResource.subSystemCode!!, sysResource.url!!))
    }

    private fun insertNewRecordToDb(): SysResource {
        val sysResource = SysResource().apply {
            subSystemCode = "subSys-a"
            url = "/a/b/n"
            name = "sysResource-n"
            resourceTypeDictCode = "2"
        }
        dao.insert(sysResource)
        return sysResource
    }

}