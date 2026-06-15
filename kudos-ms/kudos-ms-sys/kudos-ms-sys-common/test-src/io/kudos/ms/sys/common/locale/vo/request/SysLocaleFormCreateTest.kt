package io.kudos.ms.sys.common.locale.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleFormCreate
 *
 * Covers property accessors (ISysLocaleFormBase overrides), the sortNo default (0),
 * nullable remark and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleFormCreateTest {

    @Test
    fun properties() {
        val f = SysLocaleFormCreate(
            code = "zh_CN",
            displayName = "简体中文",
            englishName = "Simplified Chinese",
            sortNo = 5,
            remark = "r",
        )
        assertTrue(f is ISysLocaleFormBase)
        assertEquals("zh_CN", f.code)
        assertEquals("简体中文", f.displayName)
        assertEquals("Simplified Chinese", f.englishName)
        assertEquals(5, f.sortNo)
        assertEquals("r", f.remark)
    }

    @Test
    fun sortNoDefaultAndNullableRemark() {
        val f = SysLocaleFormCreate(
            code = "en_US",
            displayName = "English",
            englishName = "English",
            remark = null,
        )
        assertEquals(0, f.sortNo)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleFormCreate("zh_CN", "简体中文", "Simplified Chinese", 1, null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "en_US"))
        assertTrue(a.toString().contains("zh_CN"))
    }

}
