package io.kudos.ms.sys.core.resource.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.request.SysResourceQuery
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import io.kudos.ms.sys.common.resource.vo.response.SysResourceTreeRow
import io.kudos.ms.sys.core.resource.model.po.SysResource


/**
 * 资源业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysResourceService : IBaseCrudService<String, SysResource> {

    /**
     * 按资源主键从 Hash 缓存加载资源（未命中则回库并回写）
     */
    fun getResourceFromCache(id: String): SysResourceCacheEntry?

    /**
     * 按子系统编码 + URL 从缓存解析资源主键
     */
    fun getResourceIdFromCacheBySubSystemAndUrl(subSystemCode: String, url: String): String?

    /**
     * 按子系统编码 + 资源类型字典码从缓存解析资源 id 列表
     */
    fun getResourceIdsFromCacheBySubSystemAndType(subSystemCode: String, resourceTypeDictCode: String): List<String>

    /**
     * 按主键集合批量从 Hash 缓存加载资源
     */
    fun getResourcesFromCacheByIds(ids: Collection<String>): Map<String, SysResourceCacheEntry>

    /**
     * 按资源类型枚举 + 子系统编码从缓存加载资源列表
     */
    fun getResourcesFromCacheBySubSystemAndType(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * 从缓存菜单资源组装基础菜单树
     */
    fun getSimpleMenusFromCache(subSystemCode: String): List<BaseMenuTreeNode>

    /**
     * 从缓存菜单资源组装菜单树（含 url、icon）
     */
    fun getMenusFromCache(subSystemCode: String): List<MenuTreeNode>

    /**
     * 按子系统 + URL 从缓存解析资源 id（与 [getResourceIdFromCacheBySubSystemAndUrl] 一致）
     */
    fun getResourceIdFromCache(subSysDictCode: String, url: String): String?

    /**
     * 指定父结点下的直接子资源（缓存命中启用资源）
     */
    fun getDirectChildrenResourcesFromCache(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * 递归收集指定父结点下的子资源（缓存）
     */
    fun getChildrenResourcesFromCache(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry>

    /**
     * 获取子系统的资源列表（直查库）
     */
    fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRow>

    /**
     * 获取子资源列表（直查库）
     */
    fun getChildResources(parentId: String): List<SysResourceRow>

    /**
     * 获取资源树（递归结构，直查库组装）
     */
    fun getResourceTree(subSystemCode: String, parentId: String? = null): List<SysResourceTreeRow>

    /**
     * 按资源类型 / 子系统 / 资源层加载资源树直接孩子结点
     */
    fun loadDirectChildrenForTree(sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>>

    /**
     * 更新启用状态，并同步缓存
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 移动资源（调整父节点和排序）
     */
    fun moveResource(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * 获取所有祖先 id（依赖缓存中的 parent 链）
     */
    fun fetchAllParentIds(id: String): List<String>

}
