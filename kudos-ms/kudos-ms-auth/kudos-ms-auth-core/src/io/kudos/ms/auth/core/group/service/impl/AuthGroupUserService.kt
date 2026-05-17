package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 组-用户关系业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupUserService(
    dao: AuthGroupUserDao
) : BaseCrudService<String, AuthGroupUser, AuthGroupUserDao>(dao),
    IAuthGroupUserService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByGroupId(groupId: String): Set<String> {
        return dao.searchUserIdsByGroupId(groupId)
    }

    @Transactional(readOnly = true)
    override fun getGroupIdsByUserId(userId: String): Set<String> {
        return dao.searchGroupIdsByUserId(userId)
    }

    @Transactional
    override fun batchBind(groupId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        // 一次 SELECT 已存在的关系，差集对新增 ID 一次 batchInsert，把原 N+1 折叠到 2 次 SQL。
        val existing = dao.searchUserIdsByGroupId(groupId)
        val boundUserIds = userIds.toSet() - existing
        if (boundUserIds.isEmpty()) {
            log.debug("批量绑定组${groupId}与${userIds.size}个用户的关系，全部已存在，无新增。")
            return 0
        }
        val relations = boundUserIds.map { userId ->
            AuthGroupUser.Companion {
                this.groupId = groupId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定组${groupId}与${userIds.size}个用户的关系，成功绑定${boundUserIds.size}条。")
        eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(groupId: String, userId: String): Boolean {
        val count = dao.deleteByGroupIdAndUserId(groupId, userId)
        val success = count > 0
        if (success) {
            log.debug("解绑组${groupId}与用户${userId}的关系。")
            eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
        } else {
            log.warn("解绑组${groupId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, userId: String): Boolean {
        return dao.exists(groupId, userId)
    }


}
