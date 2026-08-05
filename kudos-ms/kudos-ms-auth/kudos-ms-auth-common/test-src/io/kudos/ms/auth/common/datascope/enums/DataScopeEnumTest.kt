package io.kudos.ms.auth.common.datascope.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for DataScopeEnum
 *
 * Covers every entry's code, the count, valueOf, and the [DataScopeEnum.fromCode] parser
 * including trimming, case-insensitivity, null/blank/unknown inputs.
 *
 * @author K
 * @since 1.0.0
 */
internal class DataScopeEnumTest {

    @Test
    fun codesAndEntries() {
        assertEquals("ALL", DataScopeEnum.ALL.code)
        assertEquals("ORG_AND_CHILD", DataScopeEnum.ORG_AND_CHILD.code)
        assertEquals("ORG", DataScopeEnum.ORG.code)
        assertEquals("SELF", DataScopeEnum.SELF.code)
        assertEquals("CUSTOM", DataScopeEnum.CUSTOM.code)
        assertEquals(5, DataScopeEnum.entries.size)
        // each code matches its enum name in this enum
        DataScopeEnum.entries.forEach { assertEquals(it.name, it.code) }
        // codes are unique
        assertEquals(5, DataScopeEnum.entries.map { it.code }.toSet().size)
    }

    @Test
    fun fromCodeExactMatch() {
        assertSame(DataScopeEnum.ALL, DataScopeEnum.fromCode("ALL"))
        assertSame(DataScopeEnum.CUSTOM, DataScopeEnum.fromCode("CUSTOM"))
        assertSame(DataScopeEnum.ORG_AND_CHILD, DataScopeEnum.fromCode("ORG_AND_CHILD"))
    }

    @Test
    fun fromCodeIsCaseInsensitiveAndTrims() {
        assertSame(DataScopeEnum.SELF, DataScopeEnum.fromCode("self"))
        assertSame(DataScopeEnum.ORG, DataScopeEnum.fromCode("  org  "))
        assertSame(DataScopeEnum.ALL, DataScopeEnum.fromCode("\tAll\n"))
    }

    @Test
    fun fromCodeUnknownOrNullReturnsNull() {
        assertNull(DataScopeEnum.fromCode(null))
        assertNull(DataScopeEnum.fromCode(""))
        assertNull(DataScopeEnum.fromCode("   "))
        assertNull(DataScopeEnum.fromCode("UNKNOWN"))
        assertNull(DataScopeEnum.fromCode("org_only"))
    }

    @Test
    fun valueOf() {
        assertEquals(DataScopeEnum.ORG, DataScopeEnum.valueOf("ORG"))
    }
}
