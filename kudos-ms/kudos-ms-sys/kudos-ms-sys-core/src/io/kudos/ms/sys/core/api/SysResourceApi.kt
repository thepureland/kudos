package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysResourceApi
import io.kudos.ms.sys.common.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.MenuTreeNode
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 资源 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysResourceApi : ISysResourceApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysResourceService: ISysResourceService

    override fun getResource(resourceId: String): SysResourceCacheItem? {
        return sysResourceService.getResource(resourceId)
    }

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheItem> {
        return sysResourceService.getResources(resourceIds)
    }

    override fun getResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum
    ): List<SysResourceCacheItem> {
        return sysResourceService.getResources(subSysDictCode, resourceType)
    }

    override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> {
        return sysResourceService.getSimpleMenus(subSystemCode)
    }

    override fun getMenus(subSystemCode: String): List<MenuTreeNode> {
        return sysResourceService.getMenus(subSystemCode)
    }

    override fun getResourceId(subSysDictCode: String, url: String): String? {
        return sysResourceService.getResourceId(subSysDictCode, url)
    }

    override fun getDirectChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String?
    ): List<SysResourceCacheItem> {
        return sysResourceService.getDirectChildrenResources(subSystemCode, resourceType, parentId)
    }

    override fun getChildrenResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheItem> {
        return sysResourceService.getChildrenResources(subSysDictCode, resourceType, parentId)
    }

    //endregion your codes 2

}