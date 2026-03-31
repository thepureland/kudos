package io.kudos.ms.user.common.vo.user.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.time.LocalDateTime

/**
 * 用户第三方账号表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountThirdFormBase {

    /** 关联用户账号ID */
    val userId: String?

    /** 第三方平台字典码 */
    val accountProviderDictCode: String?

    /** 发行方/平台租户 */
    val accountProviderIssuer: String?

    /** 第三方用户唯一标识 */
    val subject: String?

    /** 跨应用统一标识 */
    val unionId: String?

    /** 第三方展示名 */
    val externalDisplayName: String?

    /** 第三方邮箱 */
    val externalEmail: String?

    /** 头像URL */
    val avatarUrl: String?

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime?

    /** 租户ID */
    val tenantId: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
