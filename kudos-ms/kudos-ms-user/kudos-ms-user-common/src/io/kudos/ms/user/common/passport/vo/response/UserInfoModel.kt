package io.kudos.ms.user.common.passport.vo.response

import java.io.Serializable
import java.time.LocalDateTime


/**
 * 登录成功后返回给调用方的用户信息快照。
 *
 * 与 [io.kudos.ms.user.common.account.vo.UserAccountCacheEntry] 的区别：
 * 后者是缓存层的内部类型（含完整字段），本类仅暴露登录后可对外公开的字段。
 *
 * @author K
 * @since 1.0.0
 */
data class UserInfoModel(

    /** 用户主键 */
    val id: String,

    /** 用户名 */
    val username: String,

    /** 租户id */
    val tenantId: String,

    /** 用户所在机构id，可为 null */
    val orgId: String?,

    /** 账号类型字典码 */
    val accountTypeDictCode: String?,

    /** 默认语言 */
    val defaultLocale: String?,

    /** 默认时区 */
    val defaultTimezone: String?,

    /** 默认币种 */
    val defaultCurrency: String?,

    /** 本次登录时间（服务端时间） */
    val loginTime: LocalDateTime,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
