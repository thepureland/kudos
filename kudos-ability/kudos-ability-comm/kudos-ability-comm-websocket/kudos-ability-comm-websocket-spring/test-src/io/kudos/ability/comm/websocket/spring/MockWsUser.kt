package io.kudos.ability.comm.websocket.spring

import jakarta.annotation.PostConstruct
import org.soul.ability.comm.websocket.common.model.WsUser
import org.soul.base.security.DigestTool
import org.springframework.stereotype.Component

/**
 * 模拟websocket的用户
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@Component
class MockWsUser {

    private lateinit var user: WsUser

    @PostConstruct
    fun init() {
        user = WsUser().apply {
            id = "123"
            username = "test"
            tenantId = "-99"
            subSysCode = "console"
        }
    }

    val tenantId: String?
        get() = user.tenantId

    val userId: String?
        get() = user.id

    val token: String
        get() = DigestTool.getMD5(user.tenantId + user.id, user.username)

    fun getUserByToken(token: String) = user

}
