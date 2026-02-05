package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.user.core.dao.UserLogLoginDao
import io.kudos.ms.user.core.model.po.UserLogLogin
import io.kudos.ms.user.core.service.iservice.IUserLogLoginService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 登录日志业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class UserLogLoginService : BaseCrudService<String, UserLogLogin, UserLogLoginDao>(), IUserLogLoginService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getLoginsByUserId(userId: String, limit: Int): List<UserLogLogin> {
        val criteria = Criteria.Companion.of(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        val logins = dao.search(criteria)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun getLoginsByTenantId(tenantId: String, limit: Int): List<UserLogLogin> {
        val criteria = Criteria.Companion.of(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        val logins = dao.search(criteria)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun getLoginsByTimeRange(tenantId: String?, userId: String?, startTime: LocalDateTime, endTime: LocalDateTime): List<UserLogLogin> {
        val criteria = Criteria.Companion.of(UserLogLogin::loginTime.name, OperatorEnum.GE, startTime)
            .addAnd(UserLogLogin::loginTime.name, OperatorEnum.LE, endTime)
        if (tenantId != null) {
            criteria.addAnd(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        }
        if (userId != null) {
            criteria.addAnd(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        }
        val logins = dao.search(criteria)
        return logins.sortedByDescending { it.loginTime }
    }

    override fun getRecentLogins(tenantId: String?, userId: String?, limit: Int): List<UserLogLogin> {
        val criteria = Criteria()
        if (tenantId != null) {
            criteria.addAnd(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        }
        if (userId != null) {
            criteria.addAnd(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        }
        val logins = dao.search(criteria)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun countLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        val criteria = Criteria()
        if (tenantId != null) {
            criteria.addAnd(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        }
        if (userId != null) {
            criteria.addAnd(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        }
        if (startTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.GE, startTime)
        }
        if (endTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.LE, endTime)
        }
        return dao.count(criteria).toLong()
    }

    override fun countSuccessLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        val criteria = Criteria.Companion.of(UserLogLogin::loginSuccess.name, OperatorEnum.EQ, true)
        if (tenantId != null) {
            criteria.addAnd(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        }
        if (userId != null) {
            criteria.addAnd(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        }
        if (startTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.GE, startTime)
        }
        if (endTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.LE, endTime)
        }
        return dao.count(criteria).toLong()
    }

    override fun countFailureLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        val criteria = Criteria.Companion.of(UserLogLogin::loginSuccess.name, OperatorEnum.EQ, false)
        if (tenantId != null) {
            criteria.addAnd(UserLogLogin::tenantId.name, OperatorEnum.EQ, tenantId)
        }
        if (userId != null) {
            criteria.addAnd(UserLogLogin::userId.name, OperatorEnum.EQ, userId)
        }
        if (startTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.GE, startTime)
        }
        if (endTime != null) {
            criteria.addAnd(UserLogLogin::loginTime.name, OperatorEnum.LE, endTime)
        }
        return dao.count(criteria).toLong()
    }

    //endregion your codes 2

}