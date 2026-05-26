package io.kudos.ms.sys.api.admin.controller.resource

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.request.SysResourceFormCreate
import io.kudos.ms.sys.common.resource.vo.request.SysResourceFormUpdate
import io.kudos.ms.sys.common.resource.vo.request.SysResourceQuery
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.SysResourceDetail
import io.kudos.ms.sys.common.resource.vo.response.SysResourceEdit
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
import org.springframework.web.bind.annotation.*

/**
 * Resource management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/resource")
open class SysResourceAdminController :
    BaseCrudController<String, ISysResourceService, SysResourceQuery, SysResourceRow, SysResourceDetail, SysResourceEdit, SysResourceFormCreate, SysResourceFormUpdate>() {

    /**
     * Get resource detail by id; when `fetchAllParentIds = true`, the service back-fills the ancestor id list in one call.
     *
     * @param id primary key
     * @param fetchAllParentIds whether to fetch the ids of all parent nodes
     * @return SysResourceDetail; falls back to BaseCrudController.getDetail when the primary key has no matching record (preserves old behavior)
     */
    @GetMapping("/getResourceDetail")
    fun getResourceDetail(id: String, fetchAllParentIds: Boolean = false): SysResourceDetail =
        service.getDetailWithOptionalParents(id, fetchAllParentIds) ?: super.getDetail(id)

    /**
     * Return resources matching the resource type and sub-system.
     *
     * @param resourceType resource type enum
     * @param subSystemCode sub-system code; defaults to SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(resource object)
     */
    @GetMapping("/getResourcesByType")
    fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE
    ): List<SysResourceCacheEntry> {
        return service.getResourcesFromCacheBySubSystemAndType(resourceType, subSystemCode)
    }

    /**
     * Return the base menu tree for the given sub-system.
     *
     * @param subSystemCode sub-system code; defaults to SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(base menu tree node)
     */
    @GetMapping("/getSimpleMenus")
    fun getSimpleMenus(subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE): List<BaseMenuTreeNode> {
        return service.getSimpleMenusFromCache(subSystemCode)
    }

    /**
     * Return the menu tree for the given sub-system.
     *
     * @param subSystemCode sub-system code; defaults to SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(menu tree node)
     */
    @GetMapping("/getMenus")
    fun getMenus(subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE): List<MenuTreeNode> {
        return service.getMenusFromCache(subSystemCode)
    }

    /**
     * Return direct child menus (active) for the given parent menu id.
     *
     * @param resourceType resource type enum
     * @param parentId parent menu id; null returns the first-level menus
     * @param subSystemCode sub-system code; defaults to SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(resource object)
     */
    @GetMapping("/getDirectChildrenResources")
    fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String = SysConsts.DEFAULT_SUB_SYSTEM_CODE,
    ): List<SysResourceCacheEntry> {
        return service.getDirectChildrenResourcesFromCache(resourceType, parentId, subSystemCode)
    }

    /**
     * Return all descendant menus (active) for the given parent menu id.
     *
     * @param resourceType resource type enum
     * @param parentId parent menu id
     * @param subSystemCode sub-system code; defaults to SysConsts.DEFAULT_SUB_SYSTEM_CODE
     * @return List(resource object)
     */
    @GetMapping("/getChildrenResources")
    fun getChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        return service.getChildrenResourcesFromCache(subSystemCode, resourceType, parentId)
    }

    /**
     * Load direct child nodes of the resource tree level by level: resource type (level 0) -> sub-system (level 1) -> resource (>= level 2).
     *
     * @param sysResourceQuery resource query conditions
     * @return List<IdAndNameTreeNode>
     */
    @PostMapping("/loadDirectChildrenForTree")
    fun loadDirectChildrenForTree(@RequestBody sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>> {
        return service.loadDirectChildrenForTree(sysResourceQuery)
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
