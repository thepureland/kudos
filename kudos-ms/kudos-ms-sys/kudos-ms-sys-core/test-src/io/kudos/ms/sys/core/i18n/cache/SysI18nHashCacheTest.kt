package io.kudos.ms.sys.core.i18n.cache
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.core.i18n.dao.SysI18nDao
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * [SysI18nHashCache] 单元测试（Hash 缓存，按 locale + atomicServiceCode + i18nTypeDictCode + namespace 查询）。
 *
 * 覆盖：getI18ns（带 namespace / 不带 namespace）、全量刷新、新增/更新/删除/批量删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`SysI18nHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_I18N__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysI18nHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SysI18nHashCache

    @Resource
    private lateinit var sysI18nDao: SysI18nDao

    override fun getTestDataSqlPath(): String {
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        return "sql/${rdbType.name.lowercase()}/i18n/cache/SysI18nHashCacheTest.sql"
    }

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysI18nHashCache.CACHE_NAME)

    private val locale = "zh_CN"
    private val atomicServiceCode = "as-i18n-test-1_8910"
    private val i18nTypeDictCode = "label"
    private val namespace = "i18n.key"
    private val newValue = "value-updated"

    // ---------- getI18ns ----------

    @Test
    fun getI18ns_withNamespace() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        // active=true 的为 key 1,2,4,5,6（3、7 为 active=false）
        assertEquals(5, list.size)
        assertTrue(list.any { it.key == "1" && it.value == "value-1" })
        assertTrue(list.any { it.key == "4" })
        assertFalse(list.any { it.key == "3" })
        assertFalse(list.any { it.key == "7" })
        val listAgain = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        if (isLocalCacheEnabled() && listAgain.isNotEmpty()) {
            val first = list.first { it.key == "1" }
            val firstAgain = listAgain.first { it.key == "1" }
            assertSame(first, firstAgain)
        }
    }

    @Test
    fun getI18ns_withoutNamespace() {
        cacheHandler.reloadAll(true)
        // namespace 传空：不按 namespace 过滤，应包含 i18n.key 与 msg.key 下所有 active 项
        val list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, "")
        // i18n.key: 1,2,4,5,6；msg.key: 1
        assertTrue(list.size >= 6)
        assertTrue(list.any { it.namespace == "i18n.key" && it.key == "1" })
        assertTrue(list.any { it.namespace == "msg.key" && it.key == "1" })
    }

    @Test
    fun getI18ns_emptyResult() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.getI18ns("ja_JP", atomicServiceCode, i18nTypeDictCode, namespace)
        assertTrue(list.isEmpty())
    }

    @Test
    fun getI18ns_requireParams() {
        assertFailsWith<IllegalArgumentException> { cacheHandler.getI18ns("", atomicServiceCode, i18nTypeDictCode, namespace) }
        assertFailsWith<IllegalArgumentException> { cacheHandler.getI18ns(locale, "", i18nTypeDictCode, namespace) }
        assertFailsWith<IllegalArgumentException> { cacheHandler.getI18ns(locale, atomicServiceCode, "", namespace) }
    }

    // ---------- reloadAll ----------

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)
        var list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertEquals(5, list.size)

        val newKey = "insert-reload"
        insertNewRecordToDb(locale, atomicServiceCode, i18nTypeDictCode, namespace, newKey, "value-reload")
        val idUpdate = "40000000-0000-0000-0000-000000008910"
        sysI18nDao.updateProperties(idUpdate, mapOf(SysI18n::value.name to newValue))
        val idDelete = "40000000-0000-0000-0000-000000008911"
        sysI18nDao.deleteById(idDelete)

        cacheHandler.reloadAll(false)
        list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertTrue(list.any { it.key == newKey })
        assertEquals(newValue, list.find { it.key == "4" }?.value)
        assertFalse(list.any { it.id == idDelete })
    }

    // ---------- sync ----------

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val i18n = insertNewRecordToDb(locale, atomicServiceCode, i18nTypeDictCode, namespace, "sync-insert", "value-insert")
        cacheHandler.syncOnInsert(i18n, i18n.id)
        val list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertTrue(list.any { it.key == "sync-insert" && it.value == "value-insert" })
        cacheHandler.syncOnInsert(Any(), i18n.id)
        val listAgain = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertTrue(listAgain.any { it.key == "sync-insert" })
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val id = "40000000-0000-0000-0000-000000008910"
        val success = sysI18nDao.updateProperties(id, mapOf(SysI18n::value.name to newValue))
        assertTrue(success)
        val item = sysI18nDao.get(id, SysI18nCacheEntry::class)
        assertNotNull(item)
        cacheHandler.syncOnUpdate(item, id)
        val list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertEquals(newValue, list.find { it.key == "4" }?.value)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = "40000000-0000-0000-0000-000000008911"
        cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        sysI18nDao.deleteById(id)
        cacheHandler.syncOnDelete(id)
        val list = cacheHandler.getI18ns(locale, atomicServiceCode, i18nTypeDictCode, namespace)
        assertFalse(list.any { it.id == id })
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val id1 = "40000000-0000-0000-0000-000000008581"
        val id2 = "40000000-0000-0000-0000-000000008582"
        val count = sysI18nDao.batchDelete(listOf(id1, id2))
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(listOf(id1, id2))
        val list = cacheHandler.getI18ns("en_US", "as-i18n-test-2_8910", i18nTypeDictCode, namespace)
        assertFalse(list.any { it.id == id1 || it.id == id2 })
    }

    private fun insertNewRecordToDb(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String,
        key: String,
        value: String
    ): SysI18n {
        val i18n = SysI18n {
            this.locale = locale
            this.atomicServiceCode = atomicServiceCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.namespace = namespace
            this.key = key
            this.value = value
            this.active = true
        }
        sysI18nDao.insert(i18n)
        return i18n
    }
}
