package io.kudos.ms.sys.api.admin.controller.microservice

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.microservice.vo.request.SysMicroServiceFormCreate
import io.kudos.ms.sys.common.microservice.vo.request.SysMicroServiceFormUpdate
import io.kudos.ms.sys.common.microservice.vo.request.SysMicroServiceQuery
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceDetail
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceEdit
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceRow
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Atomic service management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/microService")
class SysMicroServiceAdminController :
    BaseCrudController<String, ISysMicroServiceService, SysMicroServiceQuery, SysMicroServiceRow, SysMicroServiceDetail, SysMicroServiceEdit, SysMicroServiceFormCreate, SysMicroServiceFormUpdate>() {

    /**
     * Return all active atomic service codes.
     *
     * @return List<atomic service code>
     */
    @GetMapping("/getAllActiveAtomicServiceCodes")
    fun getAllActiveAtomicServiceCodes(): List<String> = service.getActiveAtomicServiceCodes()

    /**
     * Return all active microservice (excluding atomic services) codes.
     *
     * @return List<microservice code>
     */
    @GetMapping("/getAllActiveMicroServiceCodes")
    fun getAllActiveMicroServiceCodes(): List<String> = service.getActiveMicroServiceCodes()

    /**
     * Return the whole microservice tree.
     *
     * @return List<IdAndNameTreeNode>
     */
    @GetMapping("/getFullMicroServiceTree")
    fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>> {
        return service.getFullMicroServiceTree()
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
