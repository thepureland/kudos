package io.kudos.ability.distributed.notify.rdb.support

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.soul.ability.distributed.notify.common.model.NotifyMessageVo
import org.soul.ability.distributed.notify.common.support.NotifyListenerItem
import org.soul.base.data.json.JsonTool
import java.io.IOException

/**
 * @author Fei
 * @date 2022/12/21 15:25
 * @since 5.0.0
 */
class AppNotifyServlet : HttpServlet() {
    @Throws(IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        this.doPost(req, resp)
    }

    @Throws(IOException::class)
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val msg = req.getHeader("msgBody")

        val messageVo = JsonTool.fromJson<NotifyMessageVo<*>>(msg, NotifyMessageVo::class.java)

        val listener = NotifyListenerItem.get(messageVo.getNotifyType())
        if (listener != null) {
            listener.notifyProcess(messageVo)
            resp.getWriter().print("notify successful!")
        } else resp.getWriter().print("Could not found the listener!")
    }
}
