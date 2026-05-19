package io.kudos.ms.user.core.login.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.ge
import io.kudos.base.query.le
import io.kudos.ms.user.core.login.model.po.UserLogLogin
import io.kudos.ms.user.core.login.model.table.UserLogLogins
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 登录日志数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserLogLoginDao : BaseCrudDao<String, UserLogLogin, UserLogLogins>() {


    /**
     * 按用户ID查询登录日志
     *
     * @param userId 用户ID
     * @return 登录日志列表
     */
    fun searchByUserId(userId: String): List<UserLogLogin> =
        search(Criteria(UserLogLogin::userId eq userId))

    /**
     * 按租户ID查询登录日志
     *
     * @param tenantId 租户ID
     * @return 登录日志列表
     */
    fun searchByTenantId(tenantId: String): List<UserLogLogin> =
        search(Criteria(UserLogLogin::tenantId eq tenantId))

    /**
     * 按可选条件查询登录日志
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 起始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 登录日志列表
     */
    fun searchByFilters(
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): List<UserLogLogin> = search(buildCriteria(tenantId, userId, startTime, endTime))

    /**
     * 按可选条件统计登录日志数量
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 起始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 日志数量
     */
    fun countByFilters(
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Int = count(buildCriteria(tenantId, userId, startTime, endTime))

    /**
     * 按登录结果和可选条件统计日志数量
     *
     * @param loginSuccess 登录是否成功
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 起始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 日志数量
     */
    fun countByLoginSuccess(
        loginSuccess: Boolean,
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Int {
        val criteria = buildCriteria(tenantId, userId, startTime, endTime)
        criteria.addAnd(UserLogLogin::loginSuccess eq loginSuccess)
        return count(criteria)
    }

    private fun buildCriteria(
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Criteria = Criteria().apply {
        tenantId?.let { addAnd(UserLogLogin::tenantId eq it) }
        userId?.let { addAnd(UserLogLogin::userId eq it) }
        startTime?.let { addAnd(UserLogLogin::loginTime ge it) }
        endTime?.let { addAnd(UserLogLogin::loginTime le it) }
    }


}