package io.kudos.ms.user.core.passport.service.iservice

import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
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
     *
     * @param req 登录请求
     * @return 登录结果（含状态枚举 + 可选信息）
     */
    fun login(req: PassportLoginRequest): PassportLoginResult

}
