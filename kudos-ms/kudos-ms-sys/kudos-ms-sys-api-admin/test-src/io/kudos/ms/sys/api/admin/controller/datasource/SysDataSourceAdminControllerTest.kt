package io.kudos.ms.sys.api.admin.controller.datasource

import io.kudos.base.query.PagingSearchResult
import io.kudos.base.security.CryptoKit
import io.kudos.ms.sys.common.datasource.consts.SysDataSourceConsts
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceQuery
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceTestRequest
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceDetail
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceEdit
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysDataSourceAdminController]'s extra endpoints (listByTenantId,
 * listBySubSystemCode, resetPassword, datasourceTest, encrypt, updateActive). It verifies the
 * tenant/sub-system list endpoints mask passwords in every returned row, that resetPassword /
 * datasourceTest / updateActive delegate verbatim, and that encrypt produces real reversible
 * ciphertext. The service is mocked and injected via reflection; no Spring context.
 *
 * The masking-helper functions themselves are covered separately in
 * [SysDataSourcePasswordMaskingTest].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDataSourceAdminControllerTest {

    private val service = mock(ISysDataSourceService::class.java)
    private val controller = SysDataSourceAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    private fun row(id: String, password: String?) = SysDataSourceRow(
        id = id,
        name = "ds-$id",
        url = "jdbc:postgresql://localhost:5432/db",
        username = "admin",
        password = password,
    )

    @Test
    fun listByTenantIdMasksEveryRowPassword() {
        val rows = listOf(row("1", "┼secret"), row("2", "plain"), row("3", null), row("4", ""))
        whenCalled(service.getDataSourcesByTenantId("t-1")).thenReturn(rows)

        val result = controller.listByTenantId("t-1")
        assertEquals(4, result.size)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result[0].password)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result[1].password)
        assertEquals(null, result[2].password) // null passes through
        assertEquals("", result[3].password)   // blank passes through
        // non-password fields preserved
        assertEquals("ds-1", result[0].name)
        assertEquals(listOf("1", "2", "3", "4"), result.map { it.id })
        verify(service).getDataSourcesByTenantId("t-1")
    }

    @Test
    fun listByTenantIdEmpty() {
        whenCalled(service.getDataSourcesByTenantId("none")).thenReturn(emptyList())
        assertTrue(controller.listByTenantId("none").isEmpty())
    }

    @Test
    fun listBySubSystemCodeMasksEveryRowPassword() {
        whenCalled(service.getDataSourcesBySubSystemCode("sub-a"))
            .thenReturn(listOf(row("9", "┼cipher")))

        val result = controller.listBySubSystemCode("sub-a")
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result.single().password)
        verify(service).getDataSourcesBySubSystemCode("sub-a")
    }

    @Test
    fun resetPasswordDelegates() {
        controller.resetPassword("ds-1", "newPwd")
        verify(service).resetPassword("ds-1", "newPwd")
    }

    @Test
    fun datasourceTestDelegatesBothOutcomes() {
        val okReq = SysDataSourceTestRequest("jdbc:url-ok", "u", "p")
        val badReq = SysDataSourceTestRequest("jdbc:url-bad", "u", null)
        whenCalled(service.testConnection("jdbc:url-ok", "u", "p")).thenReturn(true)
        whenCalled(service.testConnection("jdbc:url-bad", "u", null)).thenReturn(false)

        assertTrue(controller.datasourceTest(okReq))
        assertFalse(controller.datasourceTest(badReq))
        verify(service).testConnection("jdbc:url-ok", "u", "p")
        verify(service).testConnection("jdbc:url-bad", "u", null)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("ds-1", true)).thenReturn(true)
        whenCalled(service.updateActive("ds-2", false)).thenReturn(false)
        assertTrue(controller.updateActive("ds-1", true))
        assertFalse(controller.updateActive("ds-2", false))
        verify(service).updateActive("ds-1", true)
        verify(service).updateActive("ds-2", false)
    }

    @Test
    fun encryptProducesReversiblePrefixedCiphertext() {
        val plain = "p@ss-wörd-密码"
        val cipher = controller.encrypt(plain)

        // marker prefix present, value actually encrypted (not echoed back as plaintext)
        assertTrue(cipher.startsWith("┼"), "ciphertext must carry the ┼ marker")
        assertFalse(cipher == plain)
        // round-trips back to the original via the same key
        assertEquals(plain, CryptoKit.aesDecrypt(cipher))
    }

    @Test
    fun encryptEmptyStringStillEncrypts() {
        val cipher = controller.encrypt("")
        assertTrue(cipher.startsWith("┼"))
        assertEquals("", CryptoKit.aesDecrypt(cipher))
    }

    // ----- overridden read endpoints: must delegate to super then mask the password -----

    @Test
    fun pagingSearchMasksEveryRowPasswordAndKeepsTotalCount() {
        val query = SysDataSourceQuery(name = "main")
        val raw = PagingSearchResult(
            data = listOf(row("1", "┼secret"), row("2", "plain"), row("3", null), row("4", "")),
            totalCount = 42,
        )
        // BaseReadOnlyController.pagingSearch delegates to the non-generic
        // service.pagingSearch(listSearchPayload) overload.
        whenCalled(service.pagingSearch(query)).thenReturn(raw)

        val result = controller.pagingSearch(query)

        assertEquals(42, result.totalCount) // totalCount preserved by copy()
        assertEquals(4, result.data.size)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result.data[0].password)
        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result.data[1].password)
        assertEquals(null, result.data[2].password) // null passes through
        assertEquals("", result.data[3].password)   // blank passes through
        assertEquals("ds-1", result.data[0].name)    // non-password fields preserved
        verify(service).pagingSearch(query)
    }

    @Test
    fun getDetailMasksPassword() {
        val detail = SysDataSourceDetail(
            id = "ds-1",
            name = "main",
            url = "jdbc:postgresql://localhost:5432/db",
            username = "admin",
            password = "┼AES-ciphertext",
        )
        // Stub the generic service.get(id, KClass) overload with raw values (no eq() matcher).
        whenCalled(service.get("ds-1", SysDataSourceDetail::class)).thenReturn(detail)

        val result = controller.getDetail("ds-1")

        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result.password)
        assertEquals("main", result.name) // other fields untouched
        verify(service).get("ds-1", SysDataSourceDetail::class)
    }

    @Test
    fun getEditMasksPassword() {
        val edit = SysDataSourceEdit(
            id = "ds-2",
            name = "edit-one",
            url = "jdbc:postgresql://localhost:5432/db",
            username = "admin",
            password = "plain-pwd",
        )
        whenCalled(service.get("ds-2", SysDataSourceEdit::class)).thenReturn(edit)

        val result = controller.getEdit("ds-2")

        assertEquals(SysDataSourceConsts.PASSWORD_MASK, result.password)
        assertEquals("edit-one", result.name)
        verify(service).get("ds-2", SysDataSourceEdit::class)
    }
}
