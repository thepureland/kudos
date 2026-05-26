package io.kudos.ms.sys.api.admin.controller.system

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.system.vo.request.SysSystemFormCreate
import io.kudos.ms.sys.common.system.vo.request.SysSystemFormUpdate
import io.kudos.ms.sys.common.system.vo.request.SysSystemQuery
import io.kudos.ms.sys.common.system.vo.response.SysSystemDetail
import io.kudos.ms.sys.common.system.vo.response.SysSystemEdit
import io.kudos.ms.sys.common.system.vo.response.SysSystemRow
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * System management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/system")
class SysSystemAdminController:
    BaseCrudController<
            String,
            ISysSystemService,
            SysSystemQuery,
            SysSystemRow,
            SysSystemDetail,
            SysSystemEdit,
            SysSystemFormCreate,
            SysSystemFormUpdate
            >() {

    /**
     * Return all active sub-system codes.
     *
     * @return List<sub-system code>
     */
    @GetMapping("/getAllActiveSubSystemCodes")
    fun getAllActiveSubSystemCodes(): List<String> = service.getActiveSubSystemCodes()

    /**
     * Return all active system (excluding sub-systems) codes.
     *
     * @return List<system code>
     */
    @GetMapping("/getAllActiveSystemCodes")
    fun getAllActiveSystemCodes(): List<String> = service.getActiveSystemCodes()

    /**
     * Return the whole system tree.
     *
     * @return system tree node list (root nodes and their children)
     */
    @GetMapping("/getFullSystemTree")
    fun getFullSystemTree(): List<IdAndNameTreeNode<String>> {
        return service.getFullSystemTree()
    }

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
