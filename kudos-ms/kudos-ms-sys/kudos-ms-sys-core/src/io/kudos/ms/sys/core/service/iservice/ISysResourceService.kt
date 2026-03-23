package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.api.ISysResourceApi
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.common.vo.resource.request.SysResourceQuery
import io.kudos.ms.sys.common.vo.resource.response.SysResourceRow
import io.kudos.ms.sys.common.vo.resource.response.SysResourceTreeRow
import io.kudos.ms.sys.core.model.po.SysResource


/**
 * 资源业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysResourceService : IBaseCrudService<String, SysResource>, ISysResourceApi {


    /**
     * 根据id从缓存获取资源信息
     *
     * @param id 资源id
     * @return SysResourceCacheEntry，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getResourceById(id: String): SysResourceCacheEntry?

    /**
     * 根据子系统编码和URL从缓存获取资源ID
     *
     * @param subSystemCode 子系统编码
     * @param url 资源URL
     * @return 资源ID，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getResourceBySubSystemAndUrl(subSystemCode: String, url: String): String?

    /**
     * 根据子系统编码和资源类型从缓存获取资源ID列表
     *
     * @param subSystemCode 子系统编码
     * @param resourceTypeDictCode 资源类型字典代码
     * @return 资源ID列表
     * @author K
     * @since 1.0.0
     */
    fun getResourceIdsBySubSystemAndType(subSystemCode: String, resourceTypeDictCode: String): List<String>

    /**
     * 获取子系统的资源列表
     *
     * @param subSystemCode 子系统编码
     * @return 资源记录列表
     * @author K
     * @since 1.0.0
     */
    fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRow>

    /**
     * 获取子资源列表
     *
     * @param parentId 父资源id
     * @return 子资源记录列表
     * @author K
     * @since 1.0.0
     */
    fun getChildResources(parentId: String): List<SysResourceRow>

    /**
     * 获取资源树（递归结构）
     *
     * @param subSystemCode 子系统编码
     * @param parentId 父资源id，为null时返回顶级资源
     * @return 资源树节点列表（树形结构，包含children字段）
     * @author K
     * @since 1.0.0
     */
    fun getResourceTree(subSystemCode: String, parentId: String? = null): List<SysResourceTreeRow>

    /**
     * 按资源类型(0层)->子系统(1层)->资源(>=2层)逐层加载资源树的直接孩子结点
     *
     * @param sysResourceQuery 资源查询条件
     * @return List<IdAndNameTreeNode>
     */
    fun loadDirectChildrenForTree(sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 资源id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 移动资源（调整父节点和排序）
     *
     * @param id 资源id
     * @param newParentId 新的父资源id，为null表示移动到顶级
     * @param newOrderNum 新的排序号
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun moveResource(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * 获取所有祖先id
     *
     * @param id 当前资源id
     * @return List(祖先id)
     */
    fun fetchAllParentIds(id: String): List<String>


}
