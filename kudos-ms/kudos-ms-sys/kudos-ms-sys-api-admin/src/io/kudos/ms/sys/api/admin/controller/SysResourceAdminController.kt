package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.*
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 资源管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/resource")
open class SysResourceAdminController :
    BaseCrudController<String, ISysResourceService, SysResourceSearchPayload, SysResourceRecord, SysResourceDetail, SysResourcePayload>() {

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
    ): List<SysResourceCacheItem> {
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
    ): List<SysResourceCacheItem> {
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
    ): List<SysResourceCacheItem> {
        return service.getChildrenResources(subSystemCode, resourceType, parentId)
    }


}