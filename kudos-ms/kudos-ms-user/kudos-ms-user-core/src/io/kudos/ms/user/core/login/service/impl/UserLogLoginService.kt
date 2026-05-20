package io.kudos.ms.user.core.login.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.login.dao.UserLogLoginDao
import io.kudos.ms.user.core.login.model.po.UserLogLogin
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
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

    @Transactional(readOnly = true)
    override fun getLoginsByUserId(userId: String, limit: Int): List<UserLogLogin> =
        dao.searchByUserId(userId).sortedByDescending { it.loginTime }.take(limit)

    @Transactional(readOnly = true)
    override fun getLoginsByTenantId(tenantId: String, limit: Int): List<UserLogLogin> =
        dao.searchByTenantId(tenantId).sortedByDescending { it.loginTime }.take(limit)

    @Transactional(readOnly = true)
    override fun getLoginsByTimeRange(
        tenantId: String?, userId: String?,
        startTime: LocalDateTime, endTime: LocalDateTime,
    ): List<UserLogLogin> =
        dao.searchByFilters(tenantId, userId, startTime, endTime).sortedByDescending { it.loginTime }

    @Transactional(readOnly = true)
    override fun getRecentLogins(tenantId: String?, userId: String?, limit: Int): List<UserLogLogin> =
        dao.searchByFilters(tenantId, userId, null, null).sortedByDescending { it.loginTime }.take(limit)

    @Transactional(readOnly = true)
    override fun countLogins(
        tenantId: String?, userId: String?,
        startTime: LocalDateTime?, endTime: LocalDateTime?,
    ): Long = dao.countByFilters(tenantId, userId, startTime, endTime).toLong()

    @Transactional(readOnly = true)
    override fun countSuccessLogins(
        tenantId: String?, userId: String?,
        startTime: LocalDateTime?, endTime: LocalDateTime?,
    ): Long = dao.countByLoginSuccess(true, tenantId, userId, startTime, endTime).toLong()

    @Transactional(readOnly = true)
    override fun countFailureLogins(
        tenantId: String?, userId: String?,
        startTime: LocalDateTime?, endTime: LocalDateTime?,
    ): Long = dao.countByLoginSuccess(false, tenantId, userId, startTime, endTime).toLong()


}