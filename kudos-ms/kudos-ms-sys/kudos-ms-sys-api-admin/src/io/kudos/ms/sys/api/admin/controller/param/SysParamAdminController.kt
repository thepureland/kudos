package io.kudos.ms.sys.api.admin.controller.param

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.param.vo.request.SysParamFormCreate
import io.kudos.ms.sys.common.param.vo.request.SysParamFormUpdate
import io.kudos.ms.sys.common.param.vo.request.SysParamQuery
import io.kudos.ms.sys.common.param.vo.response.SysParamDetail
import io.kudos.ms.sys.common.param.vo.response.SysParamEdit
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import io.kudos.ms.sys.core.param.service.iservice.ISysParamService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Parameter management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/param")
class SysParamAdminController:
    BaseCrudController<String, ISysParamService, SysParamQuery, SysParamRow, SysParamDetail, SysParamEdit, SysParamFormCreate, SysParamFormUpdate>() {

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
