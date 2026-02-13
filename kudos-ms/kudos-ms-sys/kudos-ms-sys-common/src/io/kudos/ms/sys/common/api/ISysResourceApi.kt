package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.MenuTreeNode
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem


/**
 * 资源 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysResourceApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回资源id对应的资源
     *
     * @param resourceId 资源id
     * @return 资源对象
     * @author K
     * @since 1.0.0
     */
    fun getResource(resourceId: String): SysResourceCacheItem?

    /**
     * 返回资源id集合对应的资源
     *
     * @param resourceIds 资源id集合
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheItem>

    /**
     * 根据子系统和资源类型，返回对应的资源
     *
     * @param subSysDictCode 子系统编码
     * @param resourceType 资源类型枚举
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getResources(subSysDictCode: String, resourceType: ResourceTypeEnum): List<SysResourceCacheItem>

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
//    ): List<SysResourceCacheItem>

    /**
     * 根据子系统和资源类型，返回对应的资源
     *
     * @param subSysDictCode 子系统编码
     * @return List(基础的菜单树结点)
     * @author K
     * @since 1.0.0
     */
    fun getSimpleMenus(subSysDictCode: String): List<BaseMenuTreeNode>

    /**
     * 根据子系统和资源类型，返回对应的资源
     *
     * @param subSysDictCode 子系统编码
     * @return List(菜单树结点)
     * @author K
     * @since 1.0.0
     */
    fun getMenus(subSysDictCode: String): List<MenuTreeNode>

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
     * @param subSysDictCode 子系统编码
     *
     * @param parentId 父菜单id，为null时返回第一层菜单
     * @param resourceType 资源类型枚举
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getDirectChildrenResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String?
    ): List<SysResourceCacheItem>

    /**
     * 返回指定参数的孩子资源
     *
     * @param subSysDictCode 子系统编码
     * @param resourceType 资源类型枚举
     * @param parentId 父资源id
     * @return List(资源对象)
     * @author K
     * @since 1.0.0
     */
    fun getChildrenResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheItem>

    //endregion your codes 2

}