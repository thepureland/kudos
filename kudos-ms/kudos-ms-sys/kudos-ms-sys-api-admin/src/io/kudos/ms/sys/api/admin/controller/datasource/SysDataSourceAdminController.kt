package io.kudos.ms.sys.api.admin.controller.datasource

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.security.CryptoKit
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
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dataSource")
class SysDataSourceAdminController:
    BaseCrudController<String, ISysDataSourceService, SysDataSourceQuery, SysDataSourceRow, SysDataSourceDetail, SysDataSourceEdit, SysDataSourceFormCreate, SysDataSourceFormUpdate>() {

    /**
     * Get the data source list for the given tenant.
     *
     * @param tenantId tenant id
     */
    @GetMapping("/listByTenantId")
    fun listByTenantId(tenantId: String): List<SysDataSourceRow> =
        service.getDataSourcesByTenantId(tenantId)

    /**
     * Get the data source list for the given sub-system.
     */
    @GetMapping("/listBySubSystemCode")
    fun listBySubSystemCode(subSystemCode: String): List<SysDataSourceRow> =
        service.getDataSourcesBySubSystemCode(subSystemCode)

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
