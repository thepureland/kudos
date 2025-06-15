package io.kudos.ability.comm.websocket.spring

import io.kudos.base.lang.ThreadKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.RedisTestContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.soul.ability.comm.websocket.common.session.IWebSocketManager
import org.soul.base.support.model.websocket.WebsocketMsgRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * spring websocket分布式模式测试用例
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerAvailable
@Import(MockWsUser::class, MockWsTokenDecoder::class)
class SpringWebSocketDistributedTest {

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var mockWsUser: MockWsUser

    @Autowired
    private lateinit var webSocketManager: IWebSocketManager

    private lateinit var client: MockWebSocketClient

    @BeforeAll
    fun init() {
        LOG.info("分布式模式建立spring-ws连接...")
        val ws = "ws://localhost:$port/ws.do?wsToken=${mockWsUser.token}"
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
        fun property(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.comm.websocket.mode", Supplier { "distributed" })
            val container = RedisTestContainer.startIfNeeded(registry)
            registry.add("soul.ability.data.redis.redis-map.data.port", Supplier { container.firstMappedPort })
        }
    }

}
