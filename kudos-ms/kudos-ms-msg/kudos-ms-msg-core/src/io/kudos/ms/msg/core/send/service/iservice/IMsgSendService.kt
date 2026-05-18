package io.kudos.ms.msg.core.send.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.core.send.model.po.MsgSend


/**
 * 消息发送业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgSendService : IBaseCrudService<String, MsgSend> {

    /**
     * 更新发送记录的 send_status_dict_code 和 update_time。
     *
     * 不动 successCount / failCount —— 那些由 channel listener 在完成发送后写。
     *
     * @return 是否更新成功
     */
    fun updateSendStatus(sendId: String, sendStatusDictCode: String): Boolean

    /**
     * 累加成功/失败计数。channel listener 完成发送后调用。
     * 同时根据传入的状态码更新 sendStatusDictCode（如 SUCCESS / SUCCESS_PARTIAL / FAILED_FINAL）。
     *
     * @return 是否更新成功
     */
    fun finishSend(sendId: String, successDelta: Int, failDelta: Int, finalStatusDictCode: String): Boolean

}
