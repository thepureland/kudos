package io.kudos.ms.sys.api.admin.controller.accessrule

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormCreate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormUpdate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleDetail
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleEdit
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 访问规则管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/accessRule")
class SysAccessRuleAdminController:
    BaseCrudController<String, ISysAccessRuleService, SysAccessRuleQuery, SysAccessRuleRow, SysAccessRuleDetail, SysAccessRuleEdit, SysAccessRuleFormCreate, SysAccessRuleFormUpdate>() {

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
