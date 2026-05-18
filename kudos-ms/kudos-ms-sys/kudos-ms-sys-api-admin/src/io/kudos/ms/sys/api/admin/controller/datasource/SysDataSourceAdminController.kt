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
 * 数据源管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dataSource")
class SysDataSourceAdminController:
    BaseCrudController<String, ISysDataSourceService, SysDataSourceQuery, SysDataSourceRow, SysDataSourceDetail, SysDataSourceEdit, SysDataSourceFormCreate, SysDataSourceFormUpdate>() {

    /**
     * 获取指定租户的数据源列表
     *
     * @param tenantId 租户id
     */
    @GetMapping("/listByTenantId")
    fun listByTenantId(tenantId: String): List<SysDataSourceRow> =
        service.getDataSourcesByTenantId(tenantId)

    /**
     * 获取指定子系统的数据源列表
     */
    @GetMapping("/listBySubSystemCode")
    fun listBySubSystemCode(subSystemCode: String): List<SysDataSourceRow> =
        service.getDataSourcesBySubSystemCode(subSystemCode)

    /**
     * 重置密码
     *
     * @param id 主键
     * @param newPassword 新密码
     */
    @PostMapping("/resetPassword")
    fun resetPassword(@RequestParam id: String, @RequestParam newPassword: String) {
        service.resetPassword(id, newPassword)
    }

    /**
     * 测试数据源连通性（不写库）。
     *
     * @param req JDBC url/username/password
     * @return true 连接 + ping 成功
     */
    @PostMapping("/datasourceTest")
    fun datasourceTest(@RequestBody @Valid req: SysDataSourceTestRequest): Boolean =
        service.testConnection(req.url, req.username, req.password)

    /**
     * 加密敏感字段（AES-GCM）。返回密文前缀含「┼」标识。
     *
     * @param plain 待加密明文
     * @return 加密结果
     */
    @PostMapping("/encrypt")
    fun encrypt(@RequestParam plain: String): String = CryptoKit.aesEncrypt(plain)

    /**
     * 更新active状态
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

}
