package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.vo.microservice.request.SysMicroServiceFormCreate
import io.kudos.ms.sys.common.vo.microservice.request.SysMicroServiceFormUpdate
import io.kudos.ms.sys.common.vo.microservice.request.SysMicroServiceQuery
import io.kudos.ms.sys.common.vo.microservice.response.SysMicroServiceDetail
import io.kudos.ms.sys.common.vo.microservice.response.SysMicroServiceEdit
import io.kudos.ms.sys.common.vo.microservice.response.SysMicroServiceRow
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 原子服务管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/microService")
class SysMicroServiceAdminController :
    BaseCrudController<String, ISysMicroServiceService, SysMicroServiceQuery, SysMicroServiceRow, SysMicroServiceDetail, SysMicroServiceEdit, SysMicroServiceFormCreate, SysMicroServiceFormUpdate>() {

    /**
     * 返回所有启用的原子服务编码
     *
     * @return List<原子服务编码>
     */
    @GetMapping("/getAllActiveAtomicServiceCodes")
    fun getAllActiveAtomicServiceCodes(): List<String> {
        return service.getAtomicServicesFromCache().filter { it.active }.map { it.code }
    }

    /**
     * 返回所有启用的微服务（不含原子服务）编码
     *
     * @return List<微服务编码>
     */
    @GetMapping("/getAllActiveMicroServiceCodes")
    fun getAllActiveMicroServiceCodes(): List<String> {
        return service.getMicroServicesExcludeAtomicFromCache().filter { it.active }.map { it.code }
    }

    /**
     * 返回整棵微服务树
     *
     * @return List<IdAndNameTreeNode>
     */
    @GetMapping("/getFullMicroServiceTree")
    fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>> {
        return service.getFullMicroServiceTree()
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
