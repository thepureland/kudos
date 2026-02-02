package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.common.vo.resource.SysResourceRecord
import io.kudos.ms.sys.common.vo.resource.SysResourceTreeRecord


/**
 * 资源业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysResourceService : IBaseCrudService<String, SysResource> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存获取资源信息
     *
     * @param id 资源id
     * @return SysResourceCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getResourceById(id: String): SysResourceCacheItem?

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
    fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRecord>

    /**
     * 获取子资源列表
     *
     * @param parentId 父资源id
     * @return 子资源记录列表
     * @author K
     * @since 1.0.0
     */
    fun getChildResources(parentId: String): List<SysResourceRecord>

    /**
     * 获取资源树（递归结构）
     *
     * @param subSystemCode 子系统编码
     * @param parentId 父资源id，为null时返回顶级资源
     * @return 资源树节点列表（树形结构，包含children字段）
     * @author K
     * @since 1.0.0
     */
    fun getResourceTree(subSystemCode: String, parentId: String? = null): List<SysResourceTreeRecord>

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

    //endregion your codes 2

}