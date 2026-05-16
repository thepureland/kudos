package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserAdminUpdated
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 机构-用户关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserOrgUserService(
    dao: UserOrgUserDao
) : BaseCrudService<String, UserOrgUser, UserOrgUserDao>(dao), IUserOrgUserService {


    @Autowired
    private lateinit var userIdsByOrgIdCache: UserIdsByOrgIdCache

    @Autowired
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByOrgId(orgId: String): Set<String> {
        return userIdsByOrgIdCache.getUserIds(orgId).toSet()
    }

    @Transactional(readOnly = true)
    override fun getOrgIdsByUserId(userId: String): Set<String> {
        return orgIdsByUserIdCache.getOrgIds(userId).toSet()
    }

    @Transactional
    override fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        var count = 0
        val boundUserIds = mutableListOf<String>()
        userIds.forEach { userId ->
            if (!exists(orgId, userId)) {
                val relation = UserOrgUser {
                    this.orgId = orgId
                    this.userId = userId
                    this.orgAdmin = orgAdmin
                }
                dao.insert(relation)
                boundUserIds += userId
                count++
            }
        }
        log.debug("批量绑定机构${orgId}与${userIds.size}个用户的关系，成功绑定${count}条。")
        if (boundUserIds.isNotEmpty()) {
            eventPublisher.publishEvent(UserOrgUserRelationsChanged(orgId, boundUserIds))
        }
        return count
    }

    @Transactional
    override fun unbind(orgId: String, userId: String): Boolean {
        val count = dao.deleteByOrgIdAndUserId(orgId, userId)
        val success = count > 0
        if (success) {
            log.debug("解绑机构${orgId}与用户${userId}的关系。")
            eventPublisher.publishEvent(UserOrgUserRelationsChanged(orgId, listOf(userId)))
        } else {
            log.warn("解绑机构${orgId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(orgId: String, userId: String): Boolean {
        return dao.exists(orgId, userId)
    }

    @Transactional
    override fun setOrgAdmin(orgId: String, userId: String, isAdmin: Boolean): Boolean {
        val relations = dao.searchByOrgIdAndUserId(orgId, userId)
        if (relations.isEmpty()) {
            log.warn("设置机构${orgId}的用户${userId}为管理员失败，关系不存在。")
            return false
        }
        val relation = relations.first()
        val updated = UserOrgUser {
            this.id = relation.id
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = isAdmin
        }
        val success = dao.update(updated)
        if (success) {
            log.debug("设置机构${orgId}的用户${userId}为管理员：${isAdmin}。")
            // 虽然缓存不包含 orgAdmin 字段，但仍发布事件以保持下游一致性扩展点
            eventPublisher.publishEvent(UserOrgUserAdminUpdated(relation.id, orgId))
        } else {
            log.error("设置机构${orgId}的用户${userId}为管理员失败！")
        }
        return success
    }


}
