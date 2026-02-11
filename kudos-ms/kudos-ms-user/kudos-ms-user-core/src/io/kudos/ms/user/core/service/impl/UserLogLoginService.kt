package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
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
        val logins = dao.searchByUserId(userId)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun getLoginsByTenantId(tenantId: String, limit: Int): List<UserLogLogin> {
        val logins = dao.searchByTenantId(tenantId)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun getLoginsByTimeRange(tenantId: String?, userId: String?, startTime: LocalDateTime, endTime: LocalDateTime): List<UserLogLogin> {
        val logins = dao.searchByFilters(tenantId, userId, startTime, endTime)
        return logins.sortedByDescending { it.loginTime }
    }

    override fun getRecentLogins(tenantId: String?, userId: String?, limit: Int): List<UserLogLogin> {
        val logins = dao.searchByFilters(tenantId, userId, null, null)
        return logins.sortedByDescending { it.loginTime }.take(limit)
    }

    override fun countLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        return dao.countByFilters(tenantId, userId, startTime, endTime).toLong()
    }

    override fun countSuccessLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        return dao.countByLoginSuccess(true, tenantId, userId, startTime, endTime).toLong()
    }

    override fun countFailureLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long {
        return dao.countByLoginSuccess(false, tenantId, userId, startTime, endTime).toLong()
    }

    //endregion your codes 2

}