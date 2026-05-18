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
 * 语言/区域字典管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/locale")
class SysLocaleAdminController :
    BaseCrudController<String, ISysLocaleService, SysLocaleQuery, SysLocaleRow, SysLocaleDetail, SysLocaleEdit, SysLocaleFormCreate, SysLocaleFormUpdate>() {

    /**
     * 列出所有启用的语言（按 sort_no 升序）
     */
    @GetMapping("/listActive")
    fun listActive(): List<SysLocaleCacheEntry> = service.listActiveLocales()

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
