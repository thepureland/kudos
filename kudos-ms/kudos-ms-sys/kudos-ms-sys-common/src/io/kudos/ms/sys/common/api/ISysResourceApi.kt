package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.enums.resource.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.common.vo.resource.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.MenuTreeNode


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
     * @author K
     * @since 1.0.0
     */
    fun getResource(resourceId: String): SysResourceCacheEntry?

    /**
     * 返回资源id集合对应的资源
     *
     * @param resourceIds 资源id集合
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry>

    /**
     * 根据子系统和资源类型，返回对应的资源
     *
     * @param resourceType 资源类型枚举
     * @param subSystemCode 子系统编码
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getResources(resourceType: ResourceTypeEnum, subSystemCode: String): List<SysResourceCacheEntry>

//    /**
//     * 根据资源id返回对应的资源
//     *
//     * @param subSysDictCode 子系统编码
//     * @param resourceType 资源类型枚举
//     * @param resourceIds 资源id可变数组
//     * @return List(资源对象)
//     * @author K
//     * @since 1.0.0
//     */
//    fun getResources(
//        subSysDictCode: String, resourceType: ResourceType, vararg resourceIds: String
//    ): List<SysResourceCacheEntry>

    /**
     * 根据子系统，返回对应的基础菜单树
     *
     * @param subSystemCode 子系统编码
     * @return List(基础的菜单树结点)
     */
    fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode>

    /**
     * 根据子系统，返回对应的菜单树
     *
     * @param subSystemCode 子系统编码
     * @return List(菜单树结点)
     */
    fun getMenus(subSystemCode: String): List<MenuTreeNode>

    /**
     * 返回指定子系统和url对应的资源的id
     *
     * @param subSysDictCode 子系统编码
     * @param url 资源URL
     * @return 资源id
     * @author K
     * @since 1.0.0
     */
    fun getResourceId(subSysDictCode: String, url: String): String?

    /**
     * 返回指定父菜单id的直接孩子菜单(active的)
     *
     * @param resourceType 资源类型枚举
     * @param parentId 父菜单id，为null时返回第一层菜单
     * @param subSystemCode 子系统编码
     * @return List(资源对象)
     */
    fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry>

    /**
     * 返回指定参数的孩子资源
     *
     * @param subSystemCode 子系统编码
     * @param resourceType 资源类型枚举
     * @param parentId 父资源id
     * @return List(资源对象)
     */
    fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry>


}
