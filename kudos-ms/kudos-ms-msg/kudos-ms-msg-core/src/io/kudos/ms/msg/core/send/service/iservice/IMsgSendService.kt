package io.kudos.ms.msg.core.send.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.core.send.model.po.MsgSend


/**
 * Message send business service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgSendService : IBaseCrudService<String, MsgSend> {

    /**
     * Updates the send_status_dict_code and update_time of the send record.
     *
     * Does not touch successCount / failCount — those are written by the channel listener after send completes.
     *
     * @return whether the update succeeded
     */
    fun updateSendStatus(sendId: String, sendStatusDictCode: String): Boolean

    /**
     * Increments success/fail counts. Called by the channel listener after send completes.
     * Also updates sendStatusDictCode based on the provided status code (e.g. SUCCESS / SUCCESS_PARTIAL / FAILED_FINAL).
     *
     * @return whether the update succeeded
     */
    fun finishSend(sendId: String, successDelta: Int, failDelta: Int, finalStatusDictCode: String): Boolean

}
