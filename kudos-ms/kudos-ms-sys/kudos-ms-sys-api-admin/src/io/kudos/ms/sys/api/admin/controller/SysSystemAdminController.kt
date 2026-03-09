package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.system.SysSystemDetail
import io.kudos.ms.sys.common.vo.system.SysSystemPayload
import io.kudos.ms.sys.common.vo.system.SysSystemRecord
import io.kudos.ms.sys.common.vo.system.SysSystemSearchPayload
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
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
    BaseCrudController<String, ISysSystemService, SysSystemSearchPayload, SysSystemRecord, SysSystemDetail, SysSystemPayload>() {
    
    
}