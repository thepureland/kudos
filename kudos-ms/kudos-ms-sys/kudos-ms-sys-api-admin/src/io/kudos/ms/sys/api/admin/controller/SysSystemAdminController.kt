package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.vo.system.request.SysSystemFormCreate
import io.kudos.ms.sys.common.vo.system.request.SysSystemFormUpdate
import io.kudos.ms.sys.common.vo.system.request.SysSystemQuery
import io.kudos.ms.sys.common.vo.system.response.SysSystemDetail
import io.kudos.ms.sys.common.vo.system.response.SysSystemEdit
import io.kudos.ms.sys.common.vo.system.response.SysSystemRow
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
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
    BaseCrudController<String, ISysSystemService, SysSystemQuery, SysSystemRow, SysSystemDetail, SysSystemEdit, SysSystemFormCreate, SysSystemFormUpdate>() {

    /**
     * 返回所有启用的子系统编码
     *
     * @return List<子系统编码>
     */
    @GetMapping("/getAllActiveSubSystemCodes")
    fun getAllActiveSubSystemCodes(): List<String> {
        return service.getAllActiveSystems().filter { it.subSystem }.map { it.code }
    }

    /**
     * 返回整棵系统树
     *
     * @return 系统树节点列表（根节点及其 children）
     */
    @GetMapping("/getFullSystemTree")
    fun getFullSystemTree(): List<IdAndNameTreeNode<String>> {
        return service.getFullSystemTree()
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
