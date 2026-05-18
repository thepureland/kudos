package io.kudos.ms.user.common.passport.enums

/**
 * 登录尝试的结果状态。
 *
 * 与 HTTP 状态码解耦：所有失败也走 HTTP 200，由本枚举区分原因，便于前端做差异化提示。
 *
 * @author K
 * @since 1.0.0
 */
enum class PassportLoginStatusEnum {

    /** 登录成功 */
    SUCCESS,

    /** 用户名/租户不存在或被删除 */
    USER_NOT_FOUND,

    /** 密码错误（[PassportLoginResult.loginErrorTimes] 携带累计错误次数） */
    WRONG_PASSWORD,

    /** 账号已禁用（active=false） */
    INACTIVE,

    /** 账号已被锁定（错误次数超限）—— 当前实现下尚未细分，保留枚举位 */
    LOCKED,
}
