package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceDetail
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceForm
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceQuery
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceRow
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
    BaseCrudController<String, ISysMicroServiceService, SysMicroServiceQuery, SysMicroServiceRow, SysMicroServiceDetail, SysMicroServiceForm>() {

    /**
     * 返回所有启用的原子服务编码
     *
     * @return List<原子服务编码>
     */
    @GetMapping("/getAllActiveAtomicServiceCodes")
    fun getAllActiveAtomicServiceCodes(): List<String> {
        return service.getAllActiveAtomicServices().map { it.code }
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