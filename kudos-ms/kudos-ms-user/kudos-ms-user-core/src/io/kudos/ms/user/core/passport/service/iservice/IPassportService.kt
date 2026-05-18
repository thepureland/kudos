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

    /**
     * 登出：写入最后登出时间。
     *
     * 当前实现不包含会话/JWT 撤销——上层会话清理（cookie 清除、Redis 会话 evict 等）
     * 应由调用方在调用本方法**之外**完成。这里只负责把 `last_logout_time` 落库以便审计。
     *
     * @param userId 用户主键
     * @return true 写库成功；false 用户不存在或写库失败
     */
    fun logout(userId: String): Boolean

}
