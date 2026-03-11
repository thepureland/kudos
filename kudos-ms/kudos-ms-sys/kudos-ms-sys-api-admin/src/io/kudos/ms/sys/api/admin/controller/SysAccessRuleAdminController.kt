package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleDetail
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleForm
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleQuery
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleRow
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleService
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
    BaseCrudController<String, ISysAccessRuleService, SysAccessRuleQuery, SysAccessRuleRow, SysAccessRuleDetail, SysAccessRuleForm>() {



}