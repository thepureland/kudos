package io.kudos.ms.user.common.login.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 登录日志详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLogLoginDetail (

    /** 主键 */
    override val id: String = "",

    /** 用户ID */
    val userId: String? = null,

    /** 用户名 */
    val username: String? = null,

    /** 租户ID */
    val tenantId: String? = null,

    /** 登录时间 */
    val loginTime: LocalDateTime? = null,

    /** 登录IP */
    val loginIp: Long? = null,

    /** 登录地点 */
    val loginLocation: String? = null,

    /** 登录设备 */
    val loginDevice: String? = null,

    /** 浏览器 */
    val loginBrowser: String? = null,

    /** 操作系统 */
    val loginOs: String? = null,

    /** 用户代理字符串 */
    val userAgent: String? = null,

    /** 是否登录成功 */
    val loginSuccess: Boolean? = null,

    /** 失败原因 */
    val failureReason: String? = null,

    /** 会话ID */
    val sessionId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

) : IIdEntity<String>