package io.kudos.ms.user.common.passport.enums

/**
 * 修改密码（登录密码 / 安全密码）的结果。
 *
 * 与 [PassportLoginStatusEnum] 一样，HTTP 层一律 200，由本枚举区分原因。
 *
 * @author K
 * @since 1.0.0
 */
enum class ChangePasswordResultEnum {

    /** 修改成功 */
    SUCCESS,

    /** 用户不存在 */
    USER_NOT_FOUND,

    /** 旧密码不正确 */
    OLD_PASSWORD_WRONG,
}
