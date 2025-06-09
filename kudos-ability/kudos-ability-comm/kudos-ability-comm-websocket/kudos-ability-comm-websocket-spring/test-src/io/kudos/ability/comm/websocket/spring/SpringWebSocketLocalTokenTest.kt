package io.kudos.ability.comm.websocket.spring

import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.soul.ability.comm.websocket.common.model.WsUser
import org.soul.ability.comm.websocket.common.session.auth.WebSocketAuthErrorCode
import org.soul.base.exception.ServiceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.test.Test


/**
 * spring websocket本地模式token测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@Import(MockWsUser::class, MockWsTokenDecoder::class)
class SpringWebSocketLocalTokenTest {

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var mockWsUser: MockWsUser

    @MockBean
    private lateinit var mockWsTokenDecoder: MockWsTokenDecoder

    /**
     * 场景: 测试 Token 过期
     */
    @Test
    fun token_expired() {
        Mockito.`when`<WsUser?>(mockWsTokenDecoder.decodeToken(ArgumentMatchers.anyString())).thenThrow(
            ServiceException(WebSocketAuthErrorCode.WEBSOCKET_AUTH_TOKEN_EXPIRED)
        )

        val ws = "ws://localhost:$port/ws.do?wsToken=${mockWsUser.token}"
        try {
            val client = MockWebSocketClient(URI(ws))
            client.connectBlocking(10, TimeUnit.SECONDS)
            ThreadKit.sleep(1000)
        } catch (e: Exception) {
            LOG.error(e)
        }
    }

    companion object {
        private val LOG = LogFactory.getLog(SpringWebSocketLocalTokenTest::class)

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.comm.websocket.mode", Supplier { "local" })
        }
    }

}
