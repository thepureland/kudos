package io.kudos.ms.sys.api.internal.controller.resource

import io.kudos.ms.sys.common.resource.api.ISysResourceApi
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.core.resource.api.SysResourceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 资源 内部 RPC 控制器。路径继承自 [ISysResourceApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysResourceInternalController(
    private val sysResourceApi: SysResourceApi,
) : ISysResourceApi {

    override fun getResource(resourceId: String): SysResourceCacheEntry? =
        sysResourceApi.getResource(resourceId)

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry> =
        sysResourceApi.getResources(resourceIds)

    override fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> = sysResourceApi.getResources(resourceType, subSystemCode)

    override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> =
        sysResourceApi.getSimpleMenus(subSystemCode)

    override fun getMenus(subSystemCode: String): List<MenuTreeNode> =
        sysResourceApi.getMenus(subSystemCode)

    override fun getResourceId(subSysDictCode: String, url: String): String? =
        sysResourceApi.getResourceId(subSysDictCode, url)

    override fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> =
        sysResourceApi.getDirectChildrenResources(resourceType, parentId, subSystemCode)

    override fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String,
    ): List<SysResourceCacheEntry> =
        sysResourceApi.getChildrenResources(subSystemCode, resourceType, parentId)

}
