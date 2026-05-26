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
 * Resource service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysResourceService : IBaseCrudService<String, SysResource> {

    /**
     * Loads a resource from the Hash cache by primary key (queries the database and writes back on miss).
     */
    fun getResourceFromCache(id: String): SysResourceCacheEntry?

    /**
     * Resolves the resource primary key from cache by sub-system code + URL.
     */
    fun getResourceIdFromCacheBySubSystemAndUrl(subSystemCode: String, url: String): String?

    /**
     * Resolves the resource id list from cache by sub-system code + resource-type dict code.
     */
    fun getResourceIdsFromCacheBySubSystemAndType(subSystemCode: String, resourceTypeDictCode: String): List<String>

    /**
     * Batch loads resources from the Hash cache by id collection.
     */
    fun getResourcesFromCacheByIds(ids: Collection<String>): Map<String, SysResourceCacheEntry>

    /**
     * Loads the resource list from cache by resource-type enum + sub-system code.
     */
    fun getResourcesFromCacheBySubSystemAndType(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * Assembles a basic menu tree from menu resources in the cache.
     */
    fun getSimpleMenusFromCache(subSystemCode: String): List<BaseMenuTreeNode>

    /**
     * Assembles a menu tree from menu resources in the cache (including url and icon).
     */
    fun getMenusFromCache(subSystemCode: String): List<MenuTreeNode>

    /**
     * Resolves the resource id from cache by sub-system + URL (alias of [getResourceIdFromCacheBySubSystemAndUrl]).
     */
    fun getResourceIdFromCache(subSysDictCode: String, url: String): String?

    /**
     * Returns the direct child resources of the given parent node (enabled resources hit in cache).
     */
    fun getDirectChildrenResourcesFromCache(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * Recursively collects descendant resources under the given parent node (from cache).
     */
    fun getChildrenResourcesFromCache(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry>

    /**
     * Returns the resource list of the sub-system (queried directly from the database).
     */
    fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRow>

    /**
     * Returns the child resource list (queried directly from the database).
     */
    fun getChildResources(parentId: String): List<SysResourceRow>

    /**
     * Returns the resource tree (recursive structure assembled directly from the database).
     */
    fun getResourceTree(subSystemCode: String, parentId: String? = null): List<SysResourceTreeRow>

    /**
     * Loads the direct children of the resource tree by resource type / sub-system / resource layer.
     */
    fun loadDirectChildrenForTree(sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>>

    /**
     * Updates active status and synchronizes the cache.
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Moves a resource (adjusts parent node and order).
     */
    fun moveResource(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * Returns all ancestor ids (depends on the parent chain in cache).
     */
    fun fetchAllParentIds(id: String): List<String>

    /**
     * Returns the resource detail; when `includeParents = true`, also populates `parentIds`.
     *
     * Moves the admin controller's previous composition ("getDetail -> if includeParents -> a second call to fetchAllParentIds")
     * down into the service, so the controller only needs to make a single call.
     */
    fun getDetailWithOptionalParents(
        id: String,
        includeParents: Boolean,
    ): io.kudos.ms.sys.common.resource.vo.response.SysResourceDetail?

}
