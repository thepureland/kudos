package io.kudos.ms.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.model.po.AuthRoleUser
import io.kudos.ms.auth.core.service.iservice.IAuthRoleUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 角色-用户关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleUserService : BaseCrudService<String, AuthRoleUser, AuthRoleUserDao>(),
    IAuthRoleUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Autowired
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Autowired
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    private val log = LogFactory.getLog(this)

    override fun getUserIdsByRoleId(roleId: String): Set<String> {
        return userIdsByRoleIdCache.getUserIds(roleId).toSet()
    }

    override fun getRoleIdsByUserId(userId: String): Set<String> {
        return roleIdsByUserIdCache.getRoleIds(userId).toSet()
    }

    @Transactional
    override fun batchBind(roleId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        var count = 0
        userIds.forEach { userId ->
            if (!exists(roleId, userId)) {
                val relation = AuthRoleUser.Companion {
                    this.roleId = roleId
                    this.userId = userId
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定角色${roleId}与${userIds.size}个用户的关系，成功绑定${count}条。")
        // 同步缓存
        userIdsByRoleIdCache.syncOnRoleUserChange(roleId)
        userIds.forEach { userId ->
            roleIdsByUserIdCache.syncOnRoleUserChange(userId)
            resourceIdsByUserIdCache.syncOnRoleUserChange(userId)
        }
        return count
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        val count = dao.deleteByRoleIdAndUserId(roleId, userId)
        val success = count > 0
        if (success) {
            log.debug("解绑角色${roleId}与用户${userId}的关系。")
            // 同步缓存
            userIdsByRoleIdCache.syncOnRoleUserChange(roleId)
            roleIdsByUserIdCache.syncOnRoleUserChange(userId)
            resourceIdsByUserIdCache.syncOnRoleUserChange(userId)
        } else {
            log.warn("解绑角色${roleId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(roleId: String, userId: String): Boolean {
        return dao.exists(roleId, userId)
    }

    //endregion your codes 2

}
