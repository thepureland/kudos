package io.kudos.ms.user.core.loglogin.service.impl
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.loglogin.dao.UserLogLoginDao
import io.kudos.ms.user.core.loglogin.model.po.UserLogLogin
import io.kudos.ms.user.core.loglogin.service.iservice.IUserLogLoginService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 登录日志业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserLogLoginService(
    dao: UserLogLoginDao
) : BaseCrudService<String, UserLogLogin, UserLogLoginDao>(dao), IUserLogLoginService {


    private val log = LogFactory.getLog(this::class)

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


}