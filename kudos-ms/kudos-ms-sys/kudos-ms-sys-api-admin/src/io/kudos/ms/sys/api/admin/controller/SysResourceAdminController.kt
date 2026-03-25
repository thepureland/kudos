package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.enums.resource.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.common.vo.resource.request.SysResourceFormCreate
import io.kudos.ms.sys.common.vo.resource.request.SysResourceFormUpdate
import io.kudos.ms.sys.common.vo.resource.request.SysResourceQuery
import io.kudos.ms.sys.common.vo.resource.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.MenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.SysResourceDetail
import io.kudos.ms.sys.common.vo.resource.response.SysResourceEdit
import io.kudos.ms.sys.common.vo.resource.response.SysResourceRow
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import org.springframework.web.bind.annotation.*

/**
 * 资源管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/resource")
open class SysResourceAdminController :
    BaseCrudController<String, ISysResourceService, SysResourceQuery, SysResourceRow, SysResourceDetail, SysResourceEdit, SysResourceFormCreate, SysResourceFormUpdate>() {

    /**
     * 根据id获取资源详情信息，可以指定是否要获取所有父结点的id
     *
     * @param id 主键
     * @param fetchAllParentIds 是否要获取所有父结点的id
     * @return SysResourceDetail
     */
    @GetMapping("/getResourceDetail")
    fun getResourceDetail(id: String, fetchAllParentIds: Boolean = false): SysResourceDetail {
        val detail = super.getDetail(id)
        if (fetchAllParentIds) {
            detail.parentIds = service.fetchAllParentIds(id)
        }
        return detail
    }

    /**
     * 根据资源类型和子系统，返回对应的资源
     *
     * @param resourceType 资源类型枚举
     * @param subSystemCode 子系统代码, 缺省为 SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(资源对象)
     */
    @GetMapping("/getResourcesByType")
    fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE
    ): List<SysResourceCacheEntry> {
        return service.getResources(resourceType, subSystemCode)
    }

    /**
     * 根据子系统，返回对应的基础菜单树
     *
     * @param subSystemCode 子系统编码, 缺省为 SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(基础的菜单树结点)
     */
    @GetMapping("/getSimpleMenus")
    fun getSimpleMenus(subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE): List<BaseMenuTreeNode> {
        return service.getSimpleMenus(subSystemCode)
    }

    /**
     * 根据子系统，返回对应的菜单树
     *
     * @param subSystemCode 子系统编码, 缺省为 SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(菜单树结点)
     */
    @GetMapping("/getMenus")
    fun getMenus(subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE): List<MenuTreeNode> {
        return service.getMenus(subSystemCode)
    }

    /**
     * 返回指定父菜单id的直接孩子菜单(active的)
     *
     * @param resourceType 资源类型枚举
     * @param parentId 父菜单id，为null时返回第一层菜单
     * @param subSystemCode 子系统编码, 缺省为 SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(资源对象)
     */
    @GetMapping("/getDirectChildrenResources")
    fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE,
    ): List<SysResourceCacheEntry> {
        return service.getDirectChildrenResources(resourceType, parentId, subSystemCode)
    }

    /**
     * 返回指定父菜单id的所有孩子菜单(active的)
     *
     * @param resourceType 资源类型枚举
     * @param parentId 父菜单id
     * @param subSystemCode 子系统编码, 缺省为 SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(资源对象)
     */
    @GetMapping("/getChildrenResources")
    fun getChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        return service.getChildrenResources(subSystemCode, resourceType, parentId)
    }

    /**
     * 按资源类型(0层)->子系统(1层)->资源(>=2层)逐层加载资源树的直接孩子结点
     *
     * @param sysResourceQuery 资源查询条件
     * @return List<IdAndNameTreeNode>
     */
    @PostMapping("/loadDirectChildrenForTree")
    fun loadDirectChildrenForTree(@RequestBody sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>> {
        return service.loadDirectChildrenForTree(sysResourceQuery)
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
