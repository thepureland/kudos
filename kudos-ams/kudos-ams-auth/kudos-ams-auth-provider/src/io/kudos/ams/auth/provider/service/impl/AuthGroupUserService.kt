package io.kudos.ams.auth.provider.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.auth.provider.dao.AuthGroupUserDao
import io.kudos.ams.auth.provider.model.po.AuthGroupUser
import io.kudos.ams.auth.provider.service.iservice.IAuthGroupUserService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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
//region your codes 1
open class AuthGroupUserService : BaseCrudService<String, AuthGroupUser, AuthGroupUserDao>(),
    IAuthGroupUserService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getUserIdsByGroupId(groupId: String): Set<String> {
        return dao.searchUserIdsByGroupId(groupId)
    }

    override fun getGroupIdsByUserId(userId: String): Set<String> {
        return dao.searchGroupIdsByUserId(userId)
    }

    @Transactional
    override fun batchBind(groupId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        var count = 0
        userIds.forEach { userId ->
            if (!exists(groupId, userId)) {
                val relation = AuthGroupUser.Companion {
                    this.groupId = groupId
                    this.userId = userId
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定组${groupId}与${userIds.size}个用户的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(groupId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
            .addAnd(AuthGroupUser::userId.name, OperatorEnum.EQ, userId)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑组${groupId}与用户${userId}的关系。")
        } else {
            log.warn("解绑组${groupId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(groupId: String, userId: String): Boolean {
        return dao.exists(groupId, userId)
    }

    //endregion your codes 2

}
