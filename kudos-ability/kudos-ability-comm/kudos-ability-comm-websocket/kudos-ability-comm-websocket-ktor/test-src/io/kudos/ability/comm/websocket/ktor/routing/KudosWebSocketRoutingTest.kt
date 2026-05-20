package io.kudos.ability.comm.websocket.ktor.routing

import io.kudos.ability.comm.websocket.ktor.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.ktor.session.KudosWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [kudosWebSocket] 路由扩展函数的端到端测试。
 *
 * 用 `testApplication { ... }`（[io.ktor.server.testing.testApplication]）在内存中起一份
 * Ktor 应用，挂上路由 + 用同 application 上下文的 ktor client 反向连接。
 *
 * 覆盖：
 *  - **连接生命周期**：register 在 onConnect 之前；unregister 在 onDisconnect 之后
 *  - **消息收发**：客户端发 text → handler.onText 收到
 *  - **服务端主动 send**：handler 内调 `session.sendText` → 客户端 incoming 收到
 *  - **sessionFactory 接入业务元数据**：userId / tenantId 透传到注册中心
 *  - **优雅关闭**：连接关闭后注册中心计数回零
 */
internal class KudosWebSocketRoutingTest {

    @Test
    fun roundtrip_clientSendsTextAndReceivesEcho() = testApplication {
        val registry = KudosWebSocketRegistry()
        val received = Channel<String>(capacity = 16)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSession, text: String) {
                received.send(text)
                session.sendText("echo:$text")
            }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket("/ws", registry, handler)
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Text("hello"))
            val response = incoming.receive() as Frame.Text
            assertEquals("echo:hello", response.readText())
        }

        // 客户端连接已断开，等服务端跑完 finally 把 session 从注册中心剔除
        waitFor { registry.size == 0 }
        assertEquals(0, registry.size, "正常关闭后注册中心计数应回零")
        assertEquals("hello", received.tryReceive().getOrNull())
    }

    @Test
    fun sessionFactory_carriesUserAndTenantInfo() = testApplication {
        val registry = KudosWebSocketRegistry()
        val captured = Channel<Triple<String, String?, String?>>(capacity = 1)
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSession) {
                captured.send(Triple(session.sessionId, session.userId, session.tenantId))
                session.close()
            }
        }

        application {
            install(WebSockets)
            routing {
                kudosWebSocket("/ws", registry, handler) { raw ->
                    KudosWebSocketSession(
                        raw = raw,
                        userId = raw.call.request.headers["X-User-Id"],
                        tenantId = raw.call.request.headers["X-Tenant-Id"],
                    )
                }
            }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket(
            urlString = "/ws",
            request = {
                headers.append("X-User-Id", "alice")
                headers.append("X-Tenant-Id", "tenant-1")
            },
        ) {
            // 服务端 onConnect 完会主动 close，无需 send 内容；等连接关闭即可
            try { incoming.receive() } catch (_: Throwable) { /* 期望关闭 */ }
        }

        val info = captured.tryReceive().getOrNull()
        requireNotNull(info) { "onConnect 应记录一次 session" }
        assertTrue(info.first.isNotBlank())
        assertEquals("alice", info.second)
        assertEquals("tenant-1", info.third)

        waitFor { registry.size == 0 }
    }

    @Test
    fun onConnect_thenOnText_thenOnDisconnect_lifecycleOrder() = testApplication {
        val registry = KudosWebSocketRegistry()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val handler = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSession) { order.add("connect:${session.sessionId}") }
            override suspend fun onText(session: KudosWebSocketSession, text: String) { order.add("text:$text") }
            override suspend fun onDisconnect(session: KudosWebSocketSession, cause: Throwable?) {
                // 此时 registry 仍持有 session（finally 顺序：先 onDisconnect 后 unregister）
                order.add("disconnect:sizeBeforeUnregister=${registry.size}")
            }
        }

        application {
            install(WebSockets)
            routing { kudosWebSocket("/ws", registry, handler) }
        }

        val client = createClient { install(ClientWebSockets) { contentConverter = null } }
        client.webSocket("/ws") {
            send(Frame.Text("hi"))
            // 给服务端一点时间收到 frame 并处理
            delay(100)
        }

        waitFor { registry.size == 0 }
        val snapshot = order.toList()
        // connect 必须在 text 之前；text 必须在 disconnect 之前
        val connectIdx = snapshot.indexOfFirst { it.startsWith("connect:") }
        val textIdx = snapshot.indexOf("text:hi")
        val disconnectIdx = snapshot.indexOfFirst { it.startsWith("disconnect:") }
        assertTrue(connectIdx >= 0 && textIdx > connectIdx && disconnectIdx > textIdx,
            "钩子顺序应为 connect → text → disconnect，实际：$snapshot")
        // onDisconnect 时 registry 应当还看得到这次会话（unregister 在 finally 顺序里靠后）
        assertEquals("disconnect:sizeBeforeUnregister=1",
            snapshot.first { it.startsWith("disconnect:") })
    }



    /** 简易轮询等待——服务端 finally 里的 unregister 是异步发生的（coroutine 让出）。 */
    private suspend fun waitFor(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            delay(20)
        }
        // 超时不抛——具体断言由调用方完成，本函数只尽力等
    }
}
