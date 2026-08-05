package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleFormUpdate
 *
 * Covers property accessors (id + ISysLocaleFormBase overrides), IIdEntity contract,
 * nullable remark and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleFormUpdateTest {

    @Test
    fun properties() {
        val f = SysLocaleFormUpdate(
            id = "l-1",
            code = "zh_CN",
            displayName = "简体中文",
            englishName = "Simplified Chinese",
            sortNo = 5,
            remark = "r",
        )
        assertTrue(f is IIdEntity<*>)
        assertEquals("l-1", f.id)
        assertEquals("zh_CN", f.code)
        assertEquals("简体中文", f.displayName)
        assertEquals("Simplified Chinese", f.englishName)
        assertEquals(5, f.sortNo)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableRemark() {
        val f = SysLocaleFormUpdate("l-1", "en_US", "English", "English", 0, null)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleFormUpdate("l-1", "zh_CN", "简体中文", "Simplified Chinese", 1, null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "l-2"))
        assertTrue(a.toString().contains("l-1"))
    }

}
