package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictItemDao
 *
 * 测试数据来源：`SysDictItemDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysDictItemDao: SysDictItemDao

    @Test
    fun searchActiveItemByDictId() {
        val dictId = "40000000-0000-0000-0000-000000000050"
        val items = sysDictItemDao.searchActiveItemByDictId(dictId)
        assertTrue(items.size >= 2)
        assertTrue(items.any { it.itemCode == "svc-item-ditem-dao-test-1" })
        assertTrue(items.any { it.itemCode == "svc-item-ditem-dao-test-2" })
        // 验证按orderNum排序
        assertTrue(items[0].orderNum!! <= items[1].orderNum!!)
    }

    @Test
    fun pagingSearch() {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.dictType = "svc-dict-ditem-dao-test-1"
            this.pageNo = 1
            this.pageSize = 10
        }
        val records = sysDictItemDao.pagingSearch(searchPayload)
        assertTrue(records.isNotEmpty())
        assertTrue(records.any { it.itemCode == "svc-item-ditem-dao-test-1" })
    }

    @Test
    fun count() {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.dictType = "svc-dict-ditem-dao-test-1"
            this.active = true
        }
        val count = sysDictItemDao.count(searchPayload)
        assertTrue(count >= 2)
    }

    @Test
    fun leftJoinSearch() {
        val searchPayload = SysDictItemSearchPayload().apply {
            this.itemCode = "svc-item-ditem-dao-test-1"
        }
        val query = sysDictItemDao.leftJoinSearch(searchPayload)
        assertNotNull(query)
        // 验证查询对象可以正常使用
        val totalRecords = query.totalRecordsInAllPages
        assertTrue(totalRecords >= 1)
    }
}
