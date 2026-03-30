package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.model.vo.IdAndName
import io.kudos.ms.sys.common.vo.tenant.request.SysTenantFormCreate
import io.kudos.ms.sys.common.vo.tenant.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.vo.tenant.request.SysTenantQuery
import io.kudos.ms.sys.common.vo.tenant.response.SysTenantDetail
import io.kudos.ms.sys.common.vo.tenant.response.SysTenantEdit
import io.kudos.ms.sys.common.vo.tenant.response.SysTenantRow
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 租户管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/tenant")
class SysTenantAdminController: 
    BaseCrudController<String, ISysTenantService, SysTenantQuery, SysTenantRow, SysTenantDetail, SysTenantEdit, SysTenantFormCreate, SysTenantFormUpdate>() {

    /**
     * 返回指定子系统的所有租户(仅启用的)
     *
     * @param subSystemCode 子系统代码
     * @return List<IdAndName>
     */
    @GetMapping("/getTenantsBySubSystemCode")
    fun getTenantsBySubSystemCode(subSystemCode: String): List<IdAndName<String>> {
       return service.getTenantsForSubSystemFromCache(subSystemCode).filter { it.active }
            .map { IdAndName(it.id, it.name) }
    }

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
