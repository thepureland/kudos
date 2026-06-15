package io.kudos.ms.sys.client.resource.fallback

import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysResourceFallback].
 *
 * Coverage:
 *  - Single resource read returns null.
 *  - Map-returning batch read returns empty map.
 *  - All list-returning read methods (by type, simple/full menus, direct/all children)
 *    return empty lists, exercised across every [ResourceTypeEnum] value.
 *  - getResourceId returns null.
 *  - Nullable parentId both present and absent.
 *  - Edge inputs: empty strings/collections, null, Unicode.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceFallbackTest {

    private val fallback = SysResourceFallback()

    @Test
    fun getResource_returnsNull() {
        assertNull(fallback.getResource("res-1"))
        assertNull(fallback.getResource(""))
        assertNull(fallback.getResource("資源-ünï"))
    }

    @Test
    fun getResources_byIds_returnsEmptyMap() {
        assertTrue(fallback.getResources(listOf("a", "b")).isEmpty())
        assertTrue(fallback.getResources(emptyList()).isEmpty())
    }

    @Test
    fun getResources_byType_returnsEmptyList() {
        for (type in ResourceTypeEnum.entries) {
            assertTrue(fallback.getResources(type, "sub-1").isEmpty())
        }
        assertTrue(fallback.getResources(ResourceTypeEnum.MENU, "").isEmpty())
    }

    @Test
    fun getSimpleMenus_returnsEmptyList() {
        assertTrue(fallback.getSimpleMenus("sub-1").isEmpty())
        assertTrue(fallback.getSimpleMenus("").isEmpty())
    }

    @Test
    fun getMenus_returnsEmptyList() {
        assertTrue(fallback.getMenus("sub-1").isEmpty())
        assertTrue(fallback.getMenus("").isEmpty())
    }

    @Test
    fun getResourceId_returnsNull() {
        assertNull(fallback.getResourceId("dict-1", "/a/b"))
        assertNull(fallback.getResourceId("", ""))
        assertNull(fallback.getResourceId("字典", "/路徑/ünï"))
    }

    @Test
    fun getDirectChildrenResources_returnsEmptyList() {
        for (type in ResourceTypeEnum.entries) {
            assertTrue(fallback.getDirectChildrenResources(type, "parent-1", "sub-1").isEmpty())
            assertTrue(fallback.getDirectChildrenResources(type, null, "sub-1").isEmpty())
        }
    }

    @Test
    fun getChildrenResources_returnsEmptyList() {
        for (type in ResourceTypeEnum.entries) {
            assertTrue(fallback.getChildrenResources("sub-1", type, "parent-1").isEmpty())
        }
        assertTrue(fallback.getChildrenResources("", ResourceTypeEnum.ACTION, "").isEmpty())
    }
}
