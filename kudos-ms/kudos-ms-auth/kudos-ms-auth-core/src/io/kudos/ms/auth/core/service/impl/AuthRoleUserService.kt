package io.kudos.ms.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.auth.core.cache.ResourceIdsByUserIdCacheHandler
import io.kudos.ms.auth.core.cache.RoleIdsByUserIdCacheHandler
import io.kudos.ms.auth.core.cache.UserIdsByRoleIdCacheHandler
import io.kudos.ms.auth.core.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.model.po.AuthRoleUser
import io.kudos.ms.auth.core.service.iservice.IAuthRoleUserService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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
    private lateinit var userIdsByRoleIdCacheHandler: UserIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var roleIdsByUserIdCacheHandler: RoleIdsByUserIdCacheHandler

    @Autowired
    private lateinit var resourceIdsByUserIdCacheHandler: ResourceIdsByUserIdCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getUserIdsByRoleId(roleId: String): Set<String> {
        return userIdsByRoleIdCacheHandler.getUserIds(roleId).toSet()
    }

    override fun getRoleIdsByUserId(userId: String): Set<String> {
        return roleIdsByUserIdCacheHandler.getRoleIds(userId).toSet()
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
        userIdsByRoleIdCacheHandler.syncOnRoleUserChange(roleId)
        userIds.forEach { userId ->
            roleIdsByUserIdCacheHandler.syncOnRoleUserChange(userId)
            resourceIdsByUserIdCacheHandler.syncOnRoleUserChange(userId)
        }
        return count
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
            .addAnd(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑角色${roleId}与用户${userId}的关系。")
            // 同步缓存
            userIdsByRoleIdCacheHandler.syncOnRoleUserChange(roleId)
            roleIdsByUserIdCacheHandler.syncOnRoleUserChange(userId)
            resourceIdsByUserIdCacheHandler.syncOnRoleUserChange(userId)
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
