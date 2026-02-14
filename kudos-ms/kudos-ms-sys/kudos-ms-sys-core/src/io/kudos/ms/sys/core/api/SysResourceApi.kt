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
        TODO("Not yet implemented")
    }

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheItem> {
        TODO("Not yet implemented")
    }

    override fun getResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum
    ): List<SysResourceCacheItem> {
        TODO("Not yet implemented")
    }

    override fun getSimpleMenus(subSysDictCode: String): List<BaseMenuTreeNode> {
        TODO("Not yet implemented")
    }

    override fun getMenus(subSysDictCode: String): List<MenuTreeNode> {
        TODO("Not yet implemented")
    }

    override fun getResourceId(subSysDictCode: String, url: String): String? {
        TODO("Not yet implemented")
    }

    override fun getDirectChildrenResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String?
    ): List<SysResourceCacheItem> {
        TODO("Not yet implemented")
    }

    override fun getChildrenResources(
        subSysDictCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheItem> {
        TODO("Not yet implemented")
    }

    //endregion your codes 2

}