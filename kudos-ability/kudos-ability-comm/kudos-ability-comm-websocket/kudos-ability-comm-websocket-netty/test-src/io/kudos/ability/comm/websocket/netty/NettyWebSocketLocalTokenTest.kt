package io.kudos.ability.comm.websocket.netty

import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.soul.ability.comm.websocket.common.session.auth.WebSocketAuthErrorCode
import org.soul.ability.comm.websocket.netty.starter.properties.NettyWebsocketProperties
import org.soul.base.exception.ServiceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * netty websocket本地模式token测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@Import(MockWsUser::class, MockWsTokenDecoder::class)
class NettyWebSocketLocalTokenTest {

    @Autowired
    private lateinit var mockWsUser: MockWsUser

    @Autowired
    private lateinit var nettyWebsocketProperties: NettyWebsocketProperties

    @MockBean
    var mockWsTokenDecoder: MockWsTokenDecoder? = null

    /**
     * 场景: 测试 Token 过期
     */
    @Test
    fun token_expired() {
        Mockito.`when`(mockWsTokenDecoder!!.decodeToken(ArgumentMatchers.anyString())).thenThrow(
            ServiceException(WebSocketAuthErrorCode.WEBSOCKET_AUTH_TOKEN_EXPIRED)
        )

        val ws = "ws://localhost:${nettyWebsocketProperties.port}/ws.do?wsToken=${mockWsUser.token}"
        try {
            val client = MockWebSocketClient(URI(ws))
            client.connectBlocking(10, TimeUnit.SECONDS)
            //client.onClose will get error code: WebSocketAuthErrorCode.WEBSOCKET_AUTH_TOKEN_EXPIRED
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val LOG = LogFactory.getLog(this)

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.comm.websocket.mode",  { "local" })
            registry.add("kudos.ability.comm.websocket.netty.port", { randomPort() })
        }

        private fun randomPort(): kotlin.Int {
            val minPort = 10000
            val maxPort = 65535
            val port = Random().nextInt(maxPort - minPort + 1) + minPort
            return port
        }
    }

}
