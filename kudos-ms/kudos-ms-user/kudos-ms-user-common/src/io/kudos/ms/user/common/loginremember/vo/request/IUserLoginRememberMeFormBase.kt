package io.kudos.ms.user.common.loginremember.vo.request
import java.time.LocalDateTime

/**
 * 记住我登录表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserLoginRememberMeFormBase {

    /** 用户名 */
    val username: String?

    /** 令牌 */
    val token: String?

    /** 最后使用时间 */
    val lastUsed: LocalDateTime?
}
