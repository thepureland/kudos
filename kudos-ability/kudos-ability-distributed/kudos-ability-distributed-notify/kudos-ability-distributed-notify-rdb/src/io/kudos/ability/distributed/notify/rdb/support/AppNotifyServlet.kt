package io.kudos.ability.distributed.notify.rdb.support

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.soul.base.data.json.JsonTool

/**
 * @author Fei
 * @date 2022/12/21 15:25
 * @since 5.0.0
 */
class AppNotifyServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        this.doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val msg = req.getHeader("msgBody")

        val messageVo = JsonTool.fromJson(msg, NotifyMessageVo::class.java)

        val listener = NotifyListenerItem.get(messageVo.notifyType)
        if (listener != null) {
            listener.notifyProcess(messageVo)
            resp.writer.print("notify successful!")
        } else resp.writer.print("Could not found the listener!")
    }

}
