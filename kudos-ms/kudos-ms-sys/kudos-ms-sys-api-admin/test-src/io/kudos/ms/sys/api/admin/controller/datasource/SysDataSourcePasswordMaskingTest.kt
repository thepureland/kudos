package io.kudos.ms.sys.api.admin.controller.datasource

import io.kudos.ms.sys.common.datasource.consts.SysDataSourceConsts
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceDetail
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceEdit
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for the response-side password masking helpers of
 * `SysDataSourceAdminController`; no Spring context required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDataSourcePasswordMaskingTest {

    @Test
    fun nonBlankPasswordIsMasked() {
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, maskDataSourcePassword("┼AES-ciphertext"))
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, maskDataSourcePassword("plain"))
    }

    @Test
    fun nullOrBlankPasswordPassesThrough() {
        assertNull(maskDataSourcePassword(null))
        assertEquals("", maskDataSourcePassword(""))
        assertEquals("  ", maskDataSourcePassword("  "))
    }

    @Test
    fun rowMaskingKeepsOtherFields() {
        val row = SysDataSourceRow(
            id = "ds-1",
            name = "main",
            url = "jdbc:postgresql://localhost:5432/db",
            username = "admin",
            password = "┼AES-ciphertext",
        )
        val masked = maskRowPassword(row)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, masked.password)
        assertEquals(row.copy(password = SysDataSourceConsts.PASSWORD_MASK), masked)
    }

    @Test
    fun detailMaskingKeepsOtherFields() {
        val detail = SysDataSourceDetail(
            id = "ds-1",
            name = "main",
            url = "jdbc:postgresql://localhost:5432/db",
            username = "admin",
            password = "┼AES-ciphertext",
        )
        val masked = maskDetailPassword(detail)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, masked.password)
        assertEquals(detail.copy(password = SysDataSourceConsts.PASSWORD_MASK), masked)
    }

    @Test
    fun editMaskingKeepsOtherFields() {
        val edit = SysDataSourceEdit(
            id = "ds-1",
            name = "main",
            url = "jdbc:postgresql://localhost:5432/db",
            username = "admin",
            password = "┼AES-ciphertext",
        )
        val masked = maskEditPassword(edit)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, masked.password)
        assertEquals(edit.copy(password = SysDataSourceConsts.PASSWORD_MASK), masked)
    }
}
