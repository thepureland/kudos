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
 * 资源 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceApi {


    /**
     * 返回资源id对应的资源
     *
     * @param resourceId 资源id
     * @return 资源对象
     */
    @GetMapping("/api/internal/sys/resource/getResource")
    fun getResource(@RequestParam resourceId: String): SysResourceCacheEntry?

    /**
     * 返回资源id集合对应的资源
     *
     * @param resourceIds 资源id集合
     * @return Map(id, 资源对象)
     */
    @PostMapping("/api/internal/sys/resource/getResources")
    fun getResources(@RequestBody resourceIds: Collection<String>): Map<String, SysResourceCacheEntry>

    /**
     * 根据子系统和资源类型，返回对应的资源
     *
     * @param resourceType 资源类型枚举
     * @param subSystemCode 子系统编码
     */
    @GetMapping("/api/internal/sys/resource/getResourcesByType")
    fun getResources(
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * 根据子系统，返回对应的基础菜单树
     */
    @GetMapping("/api/internal/sys/resource/getSimpleMenus")
    fun getSimpleMenus(@RequestParam subSystemCode: String): List<BaseMenuTreeNode>

    /**
     * 根据子系统，返回对应的菜单树
     */
    @GetMapping("/api/internal/sys/resource/getMenus")
    fun getMenus(@RequestParam subSystemCode: String): List<MenuTreeNode>

    /**
     * 返回指定子系统和url对应的资源的id
     */
    @GetMapping("/api/internal/sys/resource/getResourceId")
    fun getResourceId(
        @RequestParam subSysDictCode: String,
        @RequestParam url: String,
    ): String?

    /**
     * 返回指定父菜单id的直接孩子菜单(active的)
     */
    @GetMapping("/api/internal/sys/resource/getDirectChildrenResources")
    fun getDirectChildrenResources(
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam(required = false) parentId: String?,
        @RequestParam subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * 返回指定参数的孩子资源
     */
    @GetMapping("/api/internal/sys/resource/getChildrenResources")
    fun getChildrenResources(
        @RequestParam subSystemCode: String,
        @RequestParam resourceType: ResourceTypeEnum,
        @RequestParam parentId: String,
    ): List<SysResourceCacheEntry>


}
