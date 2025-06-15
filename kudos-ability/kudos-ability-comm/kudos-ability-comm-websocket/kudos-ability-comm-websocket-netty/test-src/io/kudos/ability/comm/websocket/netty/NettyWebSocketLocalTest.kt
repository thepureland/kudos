package io.kudos.ability.comm.websocket.netty

import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.soul.ability.comm.websocket.common.session.IWebSocketManager
import org.soul.ability.comm.websocket.netty.starter.properties.NettyWebsocketProperties
import org.soul.base.support.model.websocket.WebsocketMsgRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * netty websocket本地模式测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@Import(MockWsUser::class, MockWsTokenDecoder::class)
class NettyWebSocketLocalTest {

    @Autowired
    private lateinit var mockWsUser: MockWsUser

    @Autowired
    private lateinit var webSocketManager: IWebSocketManager

    @Autowired
    private lateinit var nettyWebsocketProperties: NettyWebsocketProperties

    private lateinit var client: MockWebSocketClient

    @BeforeAll
    fun init() {
        LOG.info("单机模式建立netty-ws连接...")
        val ws = "ws://localhost:${nettyWebsocketProperties.port}/ws.do?wsToken=${mockWsUser.token}"
        client = MockWebSocketClient(URI(ws))
        client.connectBlocking(10, TimeUnit.SECONDS)
        ThreadKit.sleep(1000)
    }

    @Test
    fun sendMessage() {
        client.send("hello")
    }

    @Test
    fun pushMessageToUser() {
        val websocketMsg = WebsocketMsgRequest<Any?>()
        websocketMsg.content = "hello"
        val success = webSocketManager.sendMessageToUser(mockWsUser.tenantId, mockWsUser.userId, websocketMsg)
        assertTrue(success)
    }

    @AfterAll
    fun destroy() {
        LOG.info("关闭ws连接...")
        ThreadKit.sleep(1000)
        client.close()
        ThreadKit.sleep(1000)
    }

    companion object {
        private val LOG = LogFactory.getLog(this)

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.comm.websocket.mode", { "local" })
            registry.add("kudos.ability.comm.websocket.netty.port", Supplier { randomPort() })
        }

        private fun randomPort(): Int {
            val minPort = 10000
            val maxPort = 65535
            val port = Random().nextInt(maxPort - minPort + 1) + minPort
            return port
        }
    }

}
