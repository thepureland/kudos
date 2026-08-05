package io.kudos.ms.sys.common.resource.enums

import io.kudos.base.enums.ienums.IDictEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for ResourceTypeEnum
 *
 * Covers code/displayText of every entry, IDictEnum contract, valueOf round-trip
 * and entry count / code uniqueness.
 *
 * @author K
 * @since 1.0.0
 */
internal class ResourceTypeEnumTest {

    @Test
    fun codesAndDisplayTexts() {
        assertEquals("1", ResourceTypeEnum.MENU.code)
        assertEquals("Menu", ResourceTypeEnum.MENU.displayText)
        assertEquals("2", ResourceTypeEnum.FUNCTION.code)
        assertEquals("Function", ResourceTypeEnum.FUNCTION.displayText)
        assertEquals("3", ResourceTypeEnum.ACTION.code)
        assertEquals("Action", ResourceTypeEnum.ACTION.displayText)
    }

    @Test
    fun isDictEnum() {
        ResourceTypeEnum.entries.forEach { assertTrue(it is IDictEnum) }
    }

    @Test
    fun entriesAndValueOf() {
        assertEquals(3, ResourceTypeEnum.entries.size)
        assertEquals(3, ResourceTypeEnum.entries.map { it.code }.toSet().size)
        assertEquals(ResourceTypeEnum.FUNCTION, ResourceTypeEnum.valueOf("FUNCTION"))
    }

}
