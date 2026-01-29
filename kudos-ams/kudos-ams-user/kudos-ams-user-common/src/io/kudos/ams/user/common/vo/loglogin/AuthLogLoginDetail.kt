package io.kudos.ams.auth.common.vo.loglogin

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 登录日志查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthLogLoginDetail (

    //region your codes 1

    /** 用户ID */
    var userId: String? = null,

    /** 用户名 */
    var username: String? = null,

    /** 租户ID */
    var tenantId: String? = null,

    /** 登录时间 */
    var loginTime: LocalDateTime? = null,

    /** 登录IP */
    var loginIp: Long? = null,

    /** 登录地点 */
    var loginLocation: String? = null,

    /** 登录设备 */
    var loginDevice: String? = null,

    /** 浏览器 */
    var loginBrowser: String? = null,

    /** 操作系统 */
    var loginOs: String? = null,

    /** 用户代理字符串 */
    var userAgent: String? = null,

    /** 是否登录成功 */
    var loginSuccess: Boolean? = null,

    /** 失败原因 */
    var failureReason: String? = null,

    /** 会话ID */
    var sessionId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
