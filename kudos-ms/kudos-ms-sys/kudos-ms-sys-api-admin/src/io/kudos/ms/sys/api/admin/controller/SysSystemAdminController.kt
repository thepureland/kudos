package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.system.SysSystemDetail
import io.kudos.ms.sys.common.vo.system.SysSystemForm
import io.kudos.ms.sys.common.vo.system.SysSystemRow
import io.kudos.ms.sys.common.vo.system.SysSystemQuery
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 系统管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/system")
class SysSystemAdminController:
    BaseCrudController<String, ISysSystemService, SysSystemQuery, SysSystemRow, SysSystemDetail, SysSystemForm>() {

    /**
     * 返回所有启用的子系统编码
     *
     * @return List<子系统编码>
     */
    @GetMapping("/getAllActiveSubSystemCodes")
    fun getAllActiveSubSystemCodes(): List<String> {
        return service.getAllActiveSystems().filter { it.subSystem }.map { it.code }
    }
    
}