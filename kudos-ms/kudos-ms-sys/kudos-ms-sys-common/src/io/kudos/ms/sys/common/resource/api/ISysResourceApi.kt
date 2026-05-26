package io.kudos.ms.sys.common.resource.api

import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * External resource API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceApi {


    /**
     * Return the resource matching the given resource id.
     *
     * @param resourceId resource id
     * @return resource object
     */
    @GetMapping("/api/internal/sys/resource/getResource")
    fun getResource(@RequestParam resourceId: String): SysResourceCacheEntry?

    /**
     * Return the resources matching the given resource id collection.
     *
     * @param resourceIds resource id collection
     * @return Map(id, resource object)
     */
    @PostMapping("/api/internal/sys/resource/getResources")
    fun getResources(@RequestBody resourceIds: Collection<String>): Map<String, SysResourceCacheEntry>

    /**
     * Return resources matching the given sub-system and resource type.
     *
     * @param resourceType resource type enum
     * @param subSystemCode sub-system code
     */
    @GetMapping("/api/internal/sys/resource/getResourcesByType")
    fun getResources(
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * Return the base menu tree for the given sub-system.
     */
    @GetMapping("/api/internal/sys/resource/getSimpleMenus")
    fun getSimpleMenus(@RequestParam subSystemCode: String): List<BaseMenuTreeNode>

    /**
     * Return the full menu tree for the given sub-system.
     */
    @GetMapping("/api/internal/sys/resource/getMenus")
    fun getMenus(@RequestParam subSystemCode: String): List<MenuTreeNode>

    /**
     * Return the resource id matching the given sub-system and URL.
     */
    @GetMapping("/api/internal/sys/resource/getResourceId")
    fun getResourceId(
        @RequestParam subSysDictCode: String,
        @RequestParam url: String,
    ): String?

    /**
     * Return the direct (active) child menus of the given parent menu id.
     */
    @GetMapping("/api/internal/sys/resource/getDirectChildrenResources")
    fun getDirectChildrenResources(
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam(required = false) parentId: String?,
        @RequestParam subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * Return the child resources for the given parameters.
     */
    @GetMapping("/api/internal/sys/resource/getChildrenResources")
    fun getChildrenResources(
        @RequestParam subSystemCode: String,
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam parentId: String,
    ): List<SysResourceCacheEntry>


}
