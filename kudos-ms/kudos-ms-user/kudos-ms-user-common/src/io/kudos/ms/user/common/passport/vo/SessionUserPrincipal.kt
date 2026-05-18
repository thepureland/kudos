package io.kudos.ms.user.common.passport.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 登录会话中存放的"当前用户"快照。
 *
 * 落地后两个用途：
 *   1) 序列化到 `HttpSession[KudosContext.SESSION_KEY_USER]`（servlet session 默认 java 序列化）
 *   2) [io.kudos.ms.user.api.public.filter.UserContextWebFilter] 读出后填到
 *      [io.kudos.context.core.KudosContext.user]，给 `KudosContextHolder.get().user?.id`
 *      这类既有调用者用
 *
 * 实现 [IIdEntity] 是因为 `KudosContext.user` 字段是 `IIdEntity<String>?` 类型。
 *
 * @author K
 * @since 1.0.0
 */
data class SessionUserPrincipal(

    override val id: String,

    val tenantId: String,

    val username: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
