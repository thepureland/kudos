package io.kudos.ms.sys.api.admin.controller.datasource

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.security.CryptoKit
import io.kudos.ms.sys.common.datasource.consts.SysDataSourceConsts
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceFormCreate
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceFormUpdate
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceQuery
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceTestRequest
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceDetail
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceEdit
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Data source management controller.
 *
 * Every read endpoint that returns `SysDataSourceRow` / `SysDataSourceDetail` / `SysDataSourceEdit`
 * masks the password field with [SysDataSourceConsts.PASSWORD_MASK] -- even though the stored value
 * is AES ciphertext, it must never be echoed back to the admin console. On the write path, submitting
 * the mask (or a blank password) in an update form means "keep the stored password unchanged"
 * (handled by the service layer); real password changes go through [resetPassword].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dataSource")
class SysDataSourceAdminController:
    BaseCrudController<String, ISysDataSourceService, SysDataSourceQuery, SysDataSourceRow, SysDataSourceDetail, SysDataSourceEdit, SysDataSourceFormCreate, SysDataSourceFormUpdate>() {

    /**
     * Paged list query; passwords are masked in the response.
     *
     * @param searchPayload list query condition VO
     * @return current page's records (passwords masked) and the total record count
     */
    @PostMapping("/pagingSearch")
    override fun pagingSearch(@RequestBody searchPayload: SysDataSourceQuery): PagingSearchResult<SysDataSourceRow> {
        val result = super.pagingSearch(searchPayload)
        return result.copy(data = result.data.map(::maskRowPassword))
    }

    /**
     * Return the detail of the record with the given primary key; the password is masked.
     *
     * @param id primary key
     * @return record detail with masked password
     */
    @GetMapping("/getDetail")
    override fun getDetail(id: String): SysDataSourceDetail = maskDetailPassword(super.getDetail(id))

    /**
     * Return the edit record for the given primary key; the password is masked.
     * Submitting the mask back unchanged keeps the stored password.
     *
     * @param id primary key
     * @return edit VO with masked password
     */
    @GetMapping("/getEdit")
    override fun getEdit(id: String): SysDataSourceEdit = maskEditPassword(super.getEdit(id))

    /**
     * Get the data source list for the given tenant; passwords are masked.
     *
     * @param tenantId tenant id
     */
    @GetMapping("/listByTenantId")
    fun listByTenantId(tenantId: String): List<SysDataSourceRow> =
        service.getDataSourcesByTenantId(tenantId).map(::maskRowPassword)

    /**
     * Get the data source list for the given sub-system; passwords are masked.
     */
    @GetMapping("/listBySubSystemCode")
    fun listBySubSystemCode(subSystemCode: String): List<SysDataSourceRow> =
        service.getDataSourcesBySubSystemCode(subSystemCode).map(::maskRowPassword)

    /**
     * Reset the password.
     *
     * @param id primary key
     * @param newPassword new password
     */
    @PostMapping("/resetPassword")
    fun resetPassword(@RequestParam id: String, @RequestParam newPassword: String) {
        service.resetPassword(id, newPassword)
    }

    /**
     * Test data source connectivity (no DB writes).
     *
     * @param req JDBC url/username/password
     * @return true if connection + ping succeed
     */
    @PostMapping("/datasourceTest")
    fun datasourceTest(@RequestBody @Valid req: SysDataSourceTestRequest): Boolean =
        service.testConnection(req.url, req.username, req.password)

    /**
     * Encrypt a sensitive field (AES-GCM). The returned ciphertext is prefixed with the "┼" marker.
     *
     * @param plain plaintext to encrypt
     * @return encrypted result
     */
    @PostMapping("/encrypt")
    fun encrypt(@RequestParam plain: String): String = CryptoKit.aesEncrypt(plain)

    /**
     * Update the active status.
     *
     * @param id primary key
     * @param active whether enabled
     * @return whether the update succeeded
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

}

/**
 * Replace a non-blank password with [SysDataSourceConsts.PASSWORD_MASK]; `null` or blank values
 * pass through unchanged (nothing to hide, and the distinction lets the console show "no password set").
 *
 * Exposed as `internal` purely for unit testing.
 *
 * @param password stored password value (usually AES ciphertext)
 * @return masked value
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal fun maskDataSourcePassword(password: String?): String? =
    if (password.isNullOrBlank()) password else SysDataSourceConsts.PASSWORD_MASK

/** Copy of [row] with its password masked by [maskDataSourcePassword]. */
internal fun maskRowPassword(row: SysDataSourceRow): SysDataSourceRow =
    row.copy(password = maskDataSourcePassword(row.password))

/** Copy of [detail] with its password masked by [maskDataSourcePassword]. */
internal fun maskDetailPassword(detail: SysDataSourceDetail): SysDataSourceDetail =
    detail.copy(password = maskDataSourcePassword(detail.password))

/** Copy of [edit] with its password masked by [maskDataSourcePassword]. */
internal fun maskEditPassword(edit: SysDataSourceEdit): SysDataSourceEdit =
    edit.copy(password = maskDataSourcePassword(edit.password))
