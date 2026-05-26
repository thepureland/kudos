package io.kudos.ms.sys.api.admin.controller.tenant

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.model.vo.IdAndName
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormCreate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantQuery
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantDetail
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantEdit
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Tenant management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/tenant")
class SysTenantAdminController: 
    BaseCrudController<String, ISysTenantService, SysTenantQuery, SysTenantRow, SysTenantDetail, SysTenantEdit, SysTenantFormCreate, SysTenantFormUpdate>() {

    /**
     * Return all tenants (active only) for the given sub-system.
     *
     * @param subSystemCode sub-system code
     * @return List<IdAndName>
     */
    @GetMapping("/getTenantsBySubSystemCode")
    fun getTenantsBySubSystemCode(subSystemCode: String): List<IdAndName<String>> =
        service.getActiveTenantIdAndNamesForSubSystem(subSystemCode)

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
