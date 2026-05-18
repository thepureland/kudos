package io.kudos.ms.sys.api.admin.controller.outline

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.outline.vo.request.SysOutLineFormCreate
import io.kudos.ms.sys.common.outline.vo.request.SysOutLineFormUpdate
import io.kudos.ms.sys.common.outline.vo.request.SysOutLineQuery
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineDetail
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineEdit
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineRow
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 出网白名单管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/outLine")
class SysOutLineAdminController :
    BaseCrudController<String, ISysOutLineService, SysOutLineQuery, SysOutLineRow, SysOutLineDetail, SysOutLineEdit, SysOutLineFormCreate, SysOutLineFormUpdate>() {

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
