package io.kudos.ms.user.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 登录日志数据库实体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface UserLogLogin : IDbEntity<String, UserLogLogin> {
//endregion your codes 1

    companion object : DbEntityFactory<UserLogLogin>()

    /** 用户ID */
    var userId: String?

    /** 用户名 */
    var username: String

    /** 租户ID */
    var tenantId: String

    /** 登录时间 */
    var loginTime: LocalDateTime

    /** 登录IP */
    var loginIp: Long?

    /** 登录地点 */
    var loginLocation: String?

    /** 登录设备 */
    var loginDevice: String?

    /** 浏览器 */
    var loginBrowser: String?

    /** 操作系统 */
    var loginOs: String?

    /** 用户代理字符串 */
    var userAgent: String?

    /** 是否登录成功 */
    var loginSuccess: Boolean

    /** 失败原因 */
    var failureReason: String?

    /** 会话ID */
    var sessionId: String?

    /** 备注 */
    var remark: String?

    /** 创建时间 */
    var createTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}