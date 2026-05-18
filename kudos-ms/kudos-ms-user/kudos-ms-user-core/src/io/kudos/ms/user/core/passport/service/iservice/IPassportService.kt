package io.kudos.ms.user.core.passport.service.iservice

import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult


/**
 * 登录通行证业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface IPassportService {

    /**
     * 校验登录凭据，处理登录副作用（错误计数 / 最后登录信息）。
     */
    fun login(req: PassportLoginRequest): PassportLoginResult

    /**
     * 登出：写入最后登出时间。
     *
     * 不做会话/JWT 撤销——上层会话清理由调用方在本方法之外完成。
     */
    fun logout(userId: String): Boolean

    /**
     * 校验当前用户的登录密码（不消耗错误次数，不更新登录时间）。
     * 用于敏感操作前的二次身份确认。
     */
    fun verifyPassword(req: VerifyPasswordRequest): Boolean

    /**
     * 校验当前用户的安全密码（不消耗错误次数）。
     */
    fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean

    /**
     * 用户本人修改登录密码：先校验旧密码，正确才覆盖新密码。
     */
    fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum

    /**
     * 用户本人修改安全密码。
     */
    fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum

}
