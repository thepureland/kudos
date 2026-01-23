package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthDeptUserService
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.cache.UserIdsByDeptIdCacheHandler
import io.kudos.ams.auth.provider.cache.DeptIdsByUserIdCacheHandler
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 部门-用户关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthDeptUserService : BaseCrudService<String, AuthDeptUser, AuthDeptUserDao>(), IAuthDeptUserService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userIdsByDeptIdCacheHandler: UserIdsByDeptIdCacheHandler

    @Autowired
    private lateinit var deptIdsByUserIdCacheHandler: DeptIdsByUserIdCacheHandler

    private val log = LogFactory.getLog(this)

    override fun getUserIdsByDeptId(deptId: String): Set<String> {
        return userIdsByDeptIdCacheHandler.getUserIds(deptId).toSet()
    }

    override fun getDeptIdsByUserId(userId: String): Set<String> {
        return deptIdsByUserIdCacheHandler.getDeptIds(userId).toSet()
    }

    @Transactional
    override fun batchBind(deptId: String, userIds: Collection<String>, deptAdmin: Boolean): Int {
        if (userIds.isEmpty()) {
            return 0
        }
        var count = 0
        userIds.forEach { userId ->
            if (!exists(deptId, userId)) {
                val relation = AuthDeptUser {
                    this.deptId = deptId
                    this.userId = userId
                    this.deptAdmin = deptAdmin
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定部门${deptId}与${userIds.size}个用户的关系，成功绑定${count}条。")
        // 同步缓存
        userIdsByDeptIdCacheHandler.syncOnDeptUserChange(deptId)
        userIds.forEach { userId ->
            deptIdsByUserIdCacheHandler.syncOnDeptUserChange(userId)
        }
        return count
    }

    @Transactional
    override fun unbind(deptId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDeptUser::userId.name, OperatorEnum.EQ, userId)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑部门${deptId}与用户${userId}的关系。")
            // 同步缓存
            userIdsByDeptIdCacheHandler.syncOnDeptUserChange(deptId)
            deptIdsByUserIdCacheHandler.syncOnDeptUserChange(userId)
        } else {
            log.warn("解绑部门${deptId}与用户${userId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(deptId: String, userId: String): Boolean {
        return dao.exists(deptId, userId)
    }

    @Transactional
    override fun setDeptAdmin(deptId: String, userId: String, isAdmin: Boolean): Boolean {
        val criteria = Criteria.of(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDeptUser::userId.name, OperatorEnum.EQ, userId)
        val relations = dao.search(criteria)
        if (relations.isEmpty()) {
            log.warn("设置部门${deptId}的用户${userId}为管理员失败，关系不存在。")
            return false
        }
        val relation = relations.first()
        val updated = AuthDeptUser {
            this.id = relation.id
            this.deptId = deptId
            this.userId = userId
            this.deptAdmin = isAdmin
        }
        val success = dao.update(updated)
        if (success) {
            log.debug("设置部门${deptId}的用户${userId}为管理员：${isAdmin}。")
            // 同步缓存（虽然缓存不包含deptAdmin字段，但为了保持一致性，仍然同步）
            userIdsByDeptIdCacheHandler.syncOnUpdate(updated, relation.id!!)
        } else {
            log.error("设置部门${deptId}的用户${userId}为管理员失败！")
        }
        return success
    }

    //endregion your codes 2

}
