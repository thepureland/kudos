package io.kudos.ability.comm.websocket.spring

import org.soul.ability.comm.websocket.common.handler.IWsTokenDecoder
import org.soul.ability.comm.websocket.common.model.WsUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 模拟websocket token的解码器
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@Component
class MockWsTokenDecoder : IWsTokenDecoder {

    @Autowired
    private lateinit var mockWsUser: MockWsUser

    override fun decodeToken(token: String): WsUser {
        return mockWsUser.getUserByToken(token)
    }

}
