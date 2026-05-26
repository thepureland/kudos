package io.kudos.ms.user.core.login.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.core.login.model.po.UserLogLogin
import java.time.LocalDateTime

/**
 * Login log service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserLogLoginService : IBaseCrudService<String, UserLogLogin> {


    /**
     * Query login logs by user id.
     *
     * @param userId user id
     * @param limit row cap, default 100
     * @return login log list (descending by time)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByUserId(userId: String, limit: Int = 100): List<UserLogLogin>

    /**
     * Query login logs by tenant id.
     *
     * @param tenantId tenant id
     * @param limit row cap, default 100
     * @return login log list (descending by time)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByTenantId(tenantId: String, limit: Int = 100): List<UserLogLogin>

    /**
     * Query login logs in a time range.
     *
     * @param tenantId tenant id, nullable
     * @param userId user id, nullable
     * @param startTime start time
     * @param endTime end time
     * @return login log list (descending by time)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByTimeRange(tenantId: String?, userId: String?, startTime: LocalDateTime, endTime: LocalDateTime): List<UserLogLogin>

    /**
     * Query the most recent login records.
     *
     * @param tenantId tenant id, nullable
     * @param userId user id, nullable
     * @param limit row cap, default 10
     * @return login log list (descending by time)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRecentLogins(tenantId: String?, userId: String?, limit: Int = 10): List<UserLogLogin>

    /**
     * Count login attempts.
     *
     * @param tenantId tenant id, nullable
     * @param userId user id, nullable
     * @param startTime start time, nullable
     * @param endTime end time, nullable
     * @return login count
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long

    /**
     * Count successful logins.
     *
     * @param tenantId tenant id, nullable
     * @param userId user id, nullable
     * @param startTime start time, nullable
     * @param endTime end time, nullable
     * @return successful login count
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countSuccessLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long

    /**
     * Count failed logins.
     *
     * @param tenantId tenant id, nullable
     * @param userId user id, nullable
     * @param startTime start time, nullable
     * @param endTime end time, nullable
     * @return failed login count
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countFailureLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long


}
