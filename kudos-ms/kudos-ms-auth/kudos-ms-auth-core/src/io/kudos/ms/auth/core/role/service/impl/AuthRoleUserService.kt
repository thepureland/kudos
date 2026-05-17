package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
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
@Transactional
open class AuthRoleUserService(
    dao: AuthRoleUserDao
) : BaseCrudService<String, AuthRoleUser, AuthRoleUserDao>(dao),
    IAuthRoleUserService {


    @Autowired
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Autowired
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Autowired
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByRoleId(roleId: String): Set<String> {
        return userIdsByRoleIdCache.getUserIds(roleId).toSet()
    }

    @Transactional(readOnly = true)
    override fun getRoleIdsByUserId(userId: String): Set<String> {
        return roleIdsByUserIdCache.getRoleIds(userId).toSet()
    }

    @Transactional
    override fun batchBind(roleId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        // 一次 SELECT 已存在的关系，差集对新增 ID 一次 batchInsert，把原 N+1 折叠到 2 次 SQL。
        val existing = dao.searchUserIdsByRoleId(roleId).toSet()
        val boundUserIds = userIds.toSet() - existing
        if (boundUserIds.isEmpty()) {
            log.debug("批量绑定角色${roleId}与${userIds.size}个用户的关系，全部已存在，无新增。")
            return 0
        }
        val relations = boundUserIds.map { userId ->
            AuthRoleUser.Companion {
                this.roleId = roleId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定角色${roleId}与${userIds.size}个用户的关系，成功绑定${boundUserIds.size}条。")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        val count = dao.deleteByRoleIdAndUserId(roleId, userId)
        val success = count > 0
        if (success) {
            log.debug("解绑角色${roleId}与用户${userId}的关系。")
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
        } else {
            log.warn("解绑角色${roleId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, userId: String): Boolean {
        return dao.exists(roleId, userId)
    }


}
