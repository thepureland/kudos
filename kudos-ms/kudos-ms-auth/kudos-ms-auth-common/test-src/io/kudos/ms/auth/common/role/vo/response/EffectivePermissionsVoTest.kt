package io.kudos.ms.auth.common.role.vo.response

import io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for EffectivePermissionsVo
 *
 * Covers the [EffectivePermissionsVo.empty] factory, populated construction with maps,
 * Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class EffectivePermissionsVoTest {

    private fun role(id: String) = AuthRoleCacheEntry(
        id = id, code = id, name = id, tenantId = "t-1", subsysCode = "sys",
        remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    private fun group(id: String) = AuthGroupCacheEntry(
        id = id, code = id, name = id, tenantId = "t-1", subsysCode = "sys",
        remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    private fun resource(id: String) = SysResourceCacheEntry(
        id = id, name = id, url = "/$id", resourceTypeDictCode = "menu", parentId = null,
        orderNum = 1, icon = null, subSystemCode = "sys", remark = null, active = true, builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun emptyFactory() {
        val vo = EffectivePermissionsVo.empty()
        assertTrue(vo.directRoles.isEmpty())
        assertTrue(vo.groups.isEmpty())
        assertTrue(vo.rolesByGroup.isEmpty())
        assertTrue(vo.resourcesByRole.isEmpty())
    }

    @Test
    fun populated() {
        val vo = EffectivePermissionsVo(
            directRoles = listOf(role("r-1")),
            groups = listOf(group("g-1")),
            rolesByGroup = mapOf("g-1" to listOf(role("r-2"))),
            resourcesByRole = mapOf("r-1" to listOf(resource("res-1"), resource("res-2"))),
        )
        assertEquals("r-1", vo.directRoles.single().id)
        assertEquals("g-1", vo.groups.single().id)
        assertEquals("r-2", vo.rolesByGroup.getValue("g-1").single().id)
        assertEquals(2, vo.resourcesByRole.getValue("r-1").size)
        assertEquals("res-1", vo.resourcesByRole.getValue("r-1")[0].id)
    }

    @Test
    fun isSerializable() {
        assertTrue(EffectivePermissionsVo.empty() is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = EffectivePermissionsVo(
            directRoles = listOf(role("r-1")),
            groups = emptyList(),
            rolesByGroup = emptyMap(),
            resourcesByRole = emptyMap(),
        )
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(directRoles = listOf(role("r-2"))))
        assertNotEquals(EffectivePermissionsVo.empty(), a)
    }
}
