package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysResourceApi
import io.kudos.ms.sys.common.enums.resource.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.common.vo.resource.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.MenuTreeNode
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 资源 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysResourceApi : ISysResourceApi {


    @Resource
    protected lateinit var sysResourceService: ISysResourceService

    override fun getResource(resourceId: String): SysResourceCacheEntry? {
        return sysResourceService.getResource(resourceId)
    }

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry> {
        return sysResourceService.getResources(resourceIds)
    }

    override fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        return sysResourceService.getResources(resourceType, subSystemCode)
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
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        return sysResourceService.getDirectChildrenResources(resourceType, parentId, subSystemCode)
    }

    override fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry> {
        return sysResourceService.getChildrenResources(subSystemCode, resourceType, parentId)
    }


}
