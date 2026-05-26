package io.kudos.ms.sys.api.admin.controller.locale

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.common.locale.vo.request.SysLocaleFormCreate
import io.kudos.ms.sys.common.locale.vo.request.SysLocaleFormUpdate
import io.kudos.ms.sys.common.locale.vo.request.SysLocaleQuery
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleDetail
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleEdit
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleRow
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Language/locale dictionary management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/locale")
class SysLocaleAdminController :
    BaseCrudController<String, ISysLocaleService, SysLocaleQuery, SysLocaleRow, SysLocaleDetail, SysLocaleEdit, SysLocaleFormCreate, SysLocaleFormUpdate>() {

    /**
     * List all enabled languages (in ascending order of sort_no).
     */
    @GetMapping("/listActive")
    fun listActive(): List<SysLocaleCacheEntry> = service.listActiveLocales()

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
