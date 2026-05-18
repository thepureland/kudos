package io.kudos.ms.sys.core.outline.service

import io.kudos.ms.sys.core.outline.cache.OutLineBySystemAndTenantCache
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
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
 * junit test for SysOutLineService
 *
 * 测试数据来源：`SysOutLineServiceTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysOutLineServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysOutLineService: ISysOutLineService

    @Resource
    private lateinit var outLineBySystemAndTenantCache: OutLineBySystemAndTenantCache

    private val seededIdPlatform = "30000000-0000-0000-0000-000000001001"
    private val seededIdTenant = "30000000-0000-0000-0000-000000001002"
    private val seededIdInactive = "30000000-0000-0000-0000-000000001003"
    private val seededSystemCode = "sys-outline-svc-test"
    private val seededTenantId = "30000000-0000-0000-0000-000000002001"

    /** 按主键 `get(id)` 取到实体且 id 一致。 */
    @Test
    fun get_byId_entity() {
        val po = sysOutLineService.get(seededIdPlatform)
        assertNotNull(po)
        assertEquals(seededIdPlatform, po.id)
        assertEquals("example.com", po.host)
        assertNull(po.tenantId) // 平台级
    }

    /** 平台级（tenantId=null）列表读取：只命中 tenant_id IS NULL 的启用行。 */
    @Test
    fun listActiveOutLines_platformLevel() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = null)
        assertTrue(list.any { it.id == seededIdPlatform && it.host == "example.com" })
        // 不应包含租户级或未启用
        assertTrue(list.none { it.id == seededIdTenant })
        assertTrue(list.none { it.id == seededIdInactive })
    }

    /** 租户级列表：仅命中匹配 tenantId 的启用行。 */
    @Test
    fun listActiveOutLines_tenantLevel() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = seededTenantId)
        assertTrue(list.any { it.id == seededIdTenant })
        assertTrue(list.none { it.id == seededIdPlatform })
    }

    /** 未启用规则不应出现在缓存返回中。 */
    @Test
    fun listActiveOutLines_excludesInactive() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, tenantId = null)
        assertTrue(list.none { it.id == seededIdInactive })
    }

    /** updateActive：停用后缓存应不再命中，恢复启用后再次命中。 */
    @Test
    fun updateActive_syncsCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.id == seededIdPlatform })

        assertTrue(sysOutLineService.updateActive(seededIdPlatform, false))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).none { it.id == seededIdPlatform })

        assertTrue(sysOutLineService.updateActive(seededIdPlatform, true))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.id == seededIdPlatform })
    }

    /** updateActive 在主键不存在时返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysOutLineService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 删除不存在的主键时返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysOutLineService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** insert + deleteById 同步缓存。 */
    @Test
    fun insert_and_deleteById_syncCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(8)
        val host = "svc-insert-$unique.example.com"

        val id = sysOutLineService.insert(
            SysOutLine().apply {
                name = "outline-svc-insert-$unique"
                this.host = host
                port = 8080
                protocol = "https"
                systemCode = seededSystemCode
                tenantId = null
                active = true
                builtIn = false
            }
        )
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val list = sysOutLineService.listActiveOutLines(seededSystemCode, null)
        assertTrue(list.any { it.host == host })

        assertTrue(sysOutLineService.deleteById(id))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).none { it.host == host })
    }

    /** 批量删除同步缓存。 */
    @Test
    fun batchDelete_syncCache() {
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val u1 = UUID.randomUUID().toString().take(8)
        val u2 = UUID.randomUUID().toString().take(8)
        val host1 = "svc-bd1-$u1.example.com"
        val host2 = "svc-bd2-$u2.example.com"
        val id1 = sysOutLineService.insert(SysOutLine().apply {
            name = "bd1-$u1"; this.host = host1; port = 8080; protocol = "https"
            systemCode = seededSystemCode; tenantId = null; active = true; builtIn = false
        })
        val id2 = sysOutLineService.insert(SysOutLine().apply {
            name = "bd2-$u2"; this.host = host2; port = 8080; protocol = "https"
            systemCode = seededSystemCode; tenantId = null; active = true; builtIn = false
        })
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.host == host1 })
        assertTrue(sysOutLineService.listActiveOutLines(seededSystemCode, null).any { it.host == host2 })

        assertEquals(2, sysOutLineService.batchDelete(listOf(id1, id2)))
        outLineBySystemAndTenantCache.reloadAll(clear = true)
        val after = sysOutLineService.listActiveOutLines(seededSystemCode, null)
        assertTrue(after.none { it.host == host1 })
        assertTrue(after.none { it.host == host2 })
    }
}
