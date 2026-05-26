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
 * Login log data access object
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserLogLoginDao : BaseCrudDao<String, UserLogLogin, UserLogLogins>() {


    /**
     * Search login logs by user ID
     *
     * @param userId user ID
     * @return list of login logs
     */
    fun searchByUserId(userId: String): List<UserLogLogin> =
        search(Criteria(UserLogLogin::userId eq userId))

    /**
     * Search login logs by tenant ID
     *
     * @param tenantId tenant ID
     * @return list of login logs
     */
    fun searchByTenantId(tenantId: String): List<UserLogLogin> =
        search(Criteria(UserLogLogin::tenantId eq tenantId))

    /**
     * Search login logs by optional filters
     *
     * @param tenantId tenant ID, may be null
     * @param userId user ID, may be null
     * @param startTime start time, may be null
     * @param endTime end time, may be null
     * @return list of login logs
     */
    fun searchByFilters(
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): List<UserLogLogin> = search(buildCriteria(tenantId, userId, startTime, endTime))

    /**
     * Count login logs by optional filters
     *
     * @param tenantId tenant ID, may be null
     * @param userId user ID, may be null
     * @param startTime start time, may be null
     * @param endTime end time, may be null
     * @return log count
     */
    fun countByFilters(
        tenantId: String?,
        userId: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Int = count(buildCriteria(tenantId, userId, startTime, endTime))

    /**
     * Count logs by login result and optional filters
     *
     * @param loginSuccess whether the login succeeded
     * @param tenantId tenant ID, may be null
     * @param userId user ID, may be null
     * @param startTime start time, may be null
     * @param endTime end time, may be null
     * @return log count
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

    /**
     * Build optional filter parameters into a [Criteria]: each non-null field appends an AND condition.
     * Reused by count / search calls to avoid scattering if-not-null boilerplate.
     *
     * @param tenantId tenant id filter; null means no restriction
     * @param userId user id filter; null means no restriction
     * @param startTime loginTime lower bound (inclusive `>=`)
     * @param endTime loginTime upper bound (inclusive `<=`)
     * @return the assembled Criteria
     * @author K
     * @since 1.0.0
     */
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
