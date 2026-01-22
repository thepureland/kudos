package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.cache.ResourceIdsByUserIdCacheHandler
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.ams.auth.provider.service.iservice.IAuthUserService
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.sys.provider.cache.ResourceByIdCacheHandler
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 用户业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthUserService : BaseCrudService<String, AuthUser, AuthUserDao>(), IAuthUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var resourceIdsByUserIdCacheHandler: ResourceIdsByUserIdCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    override fun getResources(userId: String): List<SysResourceCacheItem> {
        // 通过用户ID获取资源ID列表
        val resourceIds = resourceIdsByUserIdCacheHandler.getResourceIds(userId)
        
        // 如果没有资源，返回空列表
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取资源缓存对象
        val resourcesMap = resourceByIdCacheHandler.getResourcesByIds(resourceIds)
        
        // 返回资源列表（按原始ID顺序）
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    //endregion your codes 2

}
