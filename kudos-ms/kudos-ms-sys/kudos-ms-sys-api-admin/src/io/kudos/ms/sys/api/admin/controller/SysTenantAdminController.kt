package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheEntry
import io.kudos.ms.sys.common.vo.tenant.SysTenantDetail
import io.kudos.ms.sys.common.vo.tenant.SysTenantForm
import io.kudos.ms.sys.common.vo.tenant.SysTenantRow
import io.kudos.ms.sys.common.vo.tenant.SysTenantQuery
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import org.springframework.web.bind.annotation.GetMapping
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
    BaseCrudController<String, ISysTenantService, SysTenantQuery, SysTenantRow, SysTenantDetail, SysTenantForm>() {

    /**
     * 返回指定子系统的所有租户(仅启用的)
     *
     * @param subSystemCode 子系统代码
     * @return List(租户缓存对象)
     */
    @GetMapping("/getTenantsBySubSystemCode")
    fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> {
       return service.getTenantsBySubSystemCode(subSystemCode)
    }

}