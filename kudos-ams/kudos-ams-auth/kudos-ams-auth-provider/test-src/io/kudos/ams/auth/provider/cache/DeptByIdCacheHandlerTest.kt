package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.provider.dao.AuthDeptDao
import io.kudos.ams.auth.provider.model.po.AuthDept
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for DeptByIdCacheHandler
 *
 * 测试数据来源：`V1.0.0.1__DeptByIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DeptByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: DeptByIdCacheHandler

    @Resource
    private lateinit var authDeptDao: AuthDeptDao

    private val newDeptName = "新部门名称"

    @Test
    fun getDeptById() {
        // 存在的
        var id = "11111111-1111-1111-1111-111111111111"
        val cacheItem2 = cacheHandler.getDeptById(id)
        val cacheItem3 = cacheHandler.getDeptById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getDeptById(id))
    }

    @Test
    fun getDeptsByIds() {
        // 都存在的
        var id1 = "11111111-1111-1111-1111-111111111111"
        var id2 = "22222222-2222-2222-2222-222222222222"
        val result2 = cacheHandler.getDeptsByIds(listOf(id1, id2))
        val result3 = cacheHandler.getDeptsByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getDeptsByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getDeptsByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val id = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDeptById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getDeptById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "22222222-2222-2222-2222-222222222222"
        val success = authDeptDao.updateProperties(id, mapOf(AuthDept::name.name to newDeptName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as AuthDeptCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newDeptName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getDeptById(id)
        assertNotNull(cacheItem2)
        assertEquals(newDeptName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = authDeptDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDeptById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = authDeptDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDeptById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDeptById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val authDept = AuthDept().apply {
            name = "测试部门_${System.currentTimeMillis()}"
            tenantId = "11111111-1111-1111-1111-111111111111"
            deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            active = true
        }
        return authDeptDao.insert(authDept)
    }

}
