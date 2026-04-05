package io.kudos.ms.sys.core.resource.api
import io.kudos.ms.sys.common.resource.api.ISysResourceApi
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
import org.springframework.stereotype.Component


/**
 * 资源 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysResourceApi(
    private val sysResourceService: ISysResourceService,
) : ISysResourceApi {

    override fun getResource(resourceId: String): SysResourceCacheEntry? = sysResourceService.getResourceFromCache(resourceId)

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry> =
        sysResourceService.getResourcesFromCacheByIds(resourceIds)

    override fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> = sysResourceService.getResourcesFromCacheBySubSystemAndType(resourceType, subSystemCode)

    override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> = sysResourceService.getSimpleMenusFromCache(subSystemCode)

    override fun getMenus(subSystemCode: String): List<MenuTreeNode> = sysResourceService.getMenusFromCache(subSystemCode)

    override fun getResourceId(subSysDictCode: String, url: String): String? = sysResourceService.getResourceIdFromCache(subSysDictCode, url)

    override fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> =
        sysResourceService.getDirectChildrenResourcesFromCache(resourceType, parentId, subSystemCode)

    override fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry> = sysResourceService.getChildrenResourcesFromCache(subSystemCode, resourceType, parentId)
}
