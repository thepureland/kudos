package io.kudos.ability.comm.websocket.spring

import io.kudos.base.logger.LogFactory
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

/**
 * 模拟websocket的客户端
 *
 * @author unknown
 * @author K
 * @since 1.0.0
 */
class MockWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {

    private val LOG = LogFactory.getLog(this)

    override fun onOpen(serverHandshake: ServerHandshake?) {
        LOG.info("ws-client连接成功！")
    }

    override fun onMessage(s: String?) {
        LOG.info("ws-client收到消息：{0}", s)
    }

    override fun onClose(i: Int, s: String?, b: Boolean) {
        LOG.info("ws-client连接已断开！")
    }

    //出现错误时调用该方法
    override fun onError(e: Exception?) {
        LOG.info("ws-client连接出现错误！")
    }

}

