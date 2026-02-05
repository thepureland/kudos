package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.user.core.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.dao.UserOrgUserDao
import io.kudos.ms.user.core.model.po.UserOrgUser
import io.kudos.ms.user.core.service.iservice.IUserOrgUserService
import org.springframework.beans.factory.annotation.Autowired
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
//region your codes 1
open class UserOrgUserService : BaseCrudService<String, UserOrgUser, UserOrgUserDao>(), IUserOrgUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userIdsByOrgIdCache: UserIdsByOrgIdCache

    @Autowired
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    private val log = LogFactory.getLog(this)

    override fun getUserIdsByOrgId(orgId: String): Set<String> {
        return userIdsByOrgIdCache.getUserIds(orgId).toSet()
    }

    override fun getOrgIdsByUserId(userId: String): Set<String> {
        return orgIdsByUserIdCache.getOrgIds(userId).toSet()
    }

    @Transactional
    override fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        var count = 0
        userIds.forEach { userId ->
            if (!exists(orgId, userId)) {
                val relation = UserOrgUser {
                    this.orgId = orgId
                    this.userId = userId
                    this.orgAdmin = orgAdmin
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定机构${orgId}与${userIds.size}个用户的关系，成功绑定${count}条。")
        // 同步缓存
        userIdsByOrgIdCache.syncOnOrgUserChange(orgId)
        userIds.forEach { userId ->
            orgIdsByUserIdCache.syncOnOrgUserChange(userId)
        }
        return count
    }

    @Transactional
    override fun unbind(orgId: String, userId: String): Boolean {
        val criteria = Criteria.of(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑机构${orgId}与用户${userId}的关系。")
            // 同步缓存
            userIdsByOrgIdCache.syncOnOrgUserChange(orgId)
            orgIdsByUserIdCache.syncOnOrgUserChange(userId)
        } else {
            log.warn("解绑机构${orgId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(orgId: String, userId: String): Boolean {
        return dao.exists(orgId, userId)
    }

    @Transactional
    override fun setOrgAdmin(orgId: String, userId: String, isAdmin: Boolean): Boolean {
        val criteria = Criteria.of(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        val relations = dao.search(criteria)
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
            // 同步缓存（虽然缓存不包含orgAdmin字段，但为了保持一致性，仍然同步）
            userIdsByOrgIdCache.syncOnUpdate(updated, relation.id!!)
        } else {
            log.error("设置机构${orgId}的用户${userId}为管理员失败！")
        }
        return success
    }

    //endregion your codes 2

}
