package io.kudos.ms.user.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.user.core.model.po.UserLogLogin
import java.time.LocalDateTime

/**
 * 登录日志业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserLogLoginService : IBaseCrudService<String, UserLogLogin> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据用户ID查询登录日志
     *
     * @param userId 用户ID
     * @param limit 限制条数，默认100
     * @return 登录日志列表（按时间倒序）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByUserId(userId: String, limit: Int = 100): List<UserLogLogin>

    /**
     * 根据租户ID查询登录日志
     *
     * @param tenantId 租户ID
     * @param limit 限制条数，默认100
     * @return 登录日志列表（按时间倒序）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByTenantId(tenantId: String, limit: Int = 100): List<UserLogLogin>

    /**
     * 根据时间范围查询登录日志
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录日志列表（按时间倒序）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getLoginsByTimeRange(tenantId: String?, userId: String?, startTime: LocalDateTime, endTime: LocalDateTime): List<UserLogLogin>

    /**
     * 查询最近登录记录
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param limit 限制条数，默认10
     * @return 登录日志列表（按时间倒序）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRecentLogins(tenantId: String?, userId: String?, limit: Int = 10): List<UserLogLogin>

    /**
     * 统计登录次数
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 登录次数
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long

    /**
     * 统计成功登录次数
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 成功登录次数
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countSuccessLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long

    /**
     * 统计失败登录次数
     *
     * @param tenantId 租户ID，可为null
     * @param userId 用户ID，可为null
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @return 失败登录次数
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun countFailureLogins(tenantId: String?, userId: String?, startTime: LocalDateTime?, endTime: LocalDateTime?): Long

    //endregion your codes 2

}