package io.kudos.ms.user.common.passport

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal


/**
 * "当前登录用户"读取工具。
 *
 * 数据源是 [io.kudos.context.core.KudosContext.user]，由
 * [io.kudos.ms.user.api.public.filter.UserContextWebFilter] 从 HttpSession 填进来。
 * 因此只在以下场景能读到非空值：
 *   1) 请求经过 web 过滤器链
 *   2) 用户已通过 `POST /api/public/user/passport/login` 登录（写入了 session）
 *
 * RPC / 后台任务 / 测试里上下文是空的，调用 [currentUserIdOrNull] 会返回 null。
 *
 * @author K
 * @since 1.0.0
 */
object CurrentUserKit {

    /** 当前线程绑定的登录用户；未登录 / 无上下文返回 null。 */
    fun currentPrincipalOrNull(): SessionUserPrincipal? =
        KudosContextHolder.getOrNull()?.user as? SessionUserPrincipal

    /** 当前线程绑定的登录用户 id；未登录 / 无上下文返回 null。 */
    fun currentUserIdOrNull(): String? = currentPrincipalOrNull()?.id

    /** 当前线程绑定的登录用户 id；未登录抛 [IllegalStateException]。用于明确"必须登录"的代码路径。 */
    fun currentUserId(): String =
        currentUserIdOrNull() ?: error("当前线程未绑定登录用户：未登录或未经过 UserContextWebFilter")

    /** 当前线程绑定的租户 id；未登录返回 null。 */
    fun currentTenantIdOrNull(): String? = currentPrincipalOrNull()?.tenantId

}
