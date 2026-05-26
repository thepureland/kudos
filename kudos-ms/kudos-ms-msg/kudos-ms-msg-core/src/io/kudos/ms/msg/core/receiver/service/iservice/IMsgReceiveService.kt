package io.kudos.ms.msg.core.receiver.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive


/**
 * Message receive business service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiveService : IBaseCrudService<String, MsgReceive> {


    /**
     * Fetches all receive records (inbox) for a user, in descending order by create time.
     *
     * Not paginated — callers that need pagination should go through the admin
     * BaseCrudController.search directly; this is intended for small "recent inbox" use cases.
     *
     * @param receiverId receiver user id
     * @return list of receive records (ordered by createTime DESC)
     */
    fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry>

    /**
     * Counts the unread receive records for a user.
     * Unread = one of [io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum.UNREAD_CODES]
     * (includes RECEIVED + UNREAD, but excludes READ / DELETED).
     *
     * @param receiverId receiver user id
     * @return unread count; returns 0 if the user has never received any messages
     */
    fun getUnreadCountByUserId(receiverId: String): Int

    /**
     * Marks a single receive record as read.
     * If the current status is already READ / DELETED, no change is made and false is returned
     * (to avoid repeatedly triggering downstream audit side effects).
     *
     * @param id receive record primary key
     * @return true if marked successfully; false if the record does not exist or the current status disallows the change
     */
    fun markRead(id: String): Boolean

    /**
     * Batch marks all unread receive records of a user as read.
     * Only takes effect on unread status; read / deleted records are skipped.
     *
     * @param receiverId receiver user id
     * @return number of records actually updated
     */
    fun markAllReadByUserId(receiverId: String): Int


}
