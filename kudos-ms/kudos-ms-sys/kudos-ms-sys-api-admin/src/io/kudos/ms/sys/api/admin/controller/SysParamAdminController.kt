package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.param.SysParamDetail
import io.kudos.ms.sys.common.vo.param.SysParamForm
import io.kudos.ms.sys.common.vo.param.SysParamQuery
import io.kudos.ms.sys.common.vo.param.SysParamRow
import io.kudos.ms.sys.core.service.iservice.ISysParamService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 参数管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/param")
class SysParamAdminController:
    BaseCrudController<String, ISysParamService, SysParamQuery, SysParamRow, SysParamDetail, SysParamForm>() {

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