package io.kudos.ms.sys.core.outline.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for OutLineBySystemAndTenantCache
 *
 * 测试数据来源：`OutLineBySystemAndTenantCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class OutLineBySystemAndTenantCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: OutLineBySystemAndTenantCache

    @Resource
    private lateinit var dao: SysOutLineDao

    private val systemCode = "sys-outline-cache-test"
    private val tenantId = "30000000-0000-0000-0000-000000003001"

    /** reloadAll 后能按 (systemCode, null) 取到平台级列表。 */
    @Test
    fun reloadAll_thenListPlatform() {
        cacheHandler.reloadAll(clear = true)
        val list = cacheHandler.listOutLines(systemCode, null)
        assertTrue(list.any { it.host == "platform.example.com" })
        assertTrue(list.none { it.host == "tenant.example.com" })
        // 不应包含 active=false
        assertTrue(list.none { it.host == "inactive.example.com" })
    }

    /** reloadAll 后能按 (systemCode, tenantId) 取到租户级列表。 */
    @Test
    fun reloadAll_thenListTenant() {
        cacheHandler.reloadAll(clear = true)
        val list = cacheHandler.listOutLines(systemCode, tenantId)
        assertTrue(list.any { it.host == "tenant.example.com" })
        assertTrue(list.none { it.host == "platform.example.com" })
    }

    /** refreshDimension 后底层 KV 存储被清空，下次读取按需回填。 */
    @Test
    fun refreshDimension_evictsAndReloads() {
        cacheHandler.reloadAll(clear = true)
        val before = cacheHandler.listOutLines(systemCode, null)
        assertTrue(before.any { it.host == "platform.example.com" })

        cacheHandler.refreshDimension(systemCode, null)
        // 仍能查到（写时回填或下次读时回填）
        val after = cacheHandler.listOutLines(systemCode, null)
        assertTrue(after.any { it.host == "platform.example.com" })
    }

    /** 直接通过 dao 插入并 refreshDimension 后能命中新行。 */
    @Test
    fun refreshDimension_picksUpNewRowFromDb() {
        cacheHandler.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(8)
        val newHost = "cache-new-$unique.example.com"

        dao.insert(SysOutLine().apply {
            name = "cache-test-$unique"
            host = newHost
            port = 443
            protocol = "https"
            this.systemCode = this@OutLineBySystemAndTenantCacheTest.systemCode
            tenantId = null
            active = true
            builtIn = false
        })

        cacheHandler.refreshDimension(systemCode, null)
        val list = cacheHandler.listOutLines(systemCode, null)
        assertTrue(list.any { it.host == newHost })

        // 用底层 KV API 校验底层 key 也命中
        val key = OutLineSystemTenantKey.compositeKey(systemCode, null)
        @Suppress("UNCHECKED_CAST")
        val raw = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as? List<SysOutLineCacheEntry>
        assertNotNull(raw)
        assertEquals(true, raw.any { it.host == newHost })
    }

    /** 命中后再 reloadAll(clear=true) 会重置内容。 */
    @Test
    fun reloadAll_clearTrue_rewrites() {
        cacheHandler.reloadAll(clear = true)
        assertTrue(cacheHandler.listOutLines(systemCode, null).any { it.host == "platform.example.com" })

        // 删除一条平台级启用的（host=platform.example.com）然后 reloadAll
        val toDelete = dao.searchAs<SysOutLineCacheEntry>().firstOrNull { it.host == "platform.example.com" }
        assertNotNull(toDelete)
        dao.deleteById(toDelete.id)

        cacheHandler.reloadAll(clear = true)
        assertNull(
            cacheHandler.listOutLines(systemCode, null).find { it.host == "platform.example.com" }
        )
    }
}
