package io.kudos.ms.msg.core.instance.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.model.po.MsgInstance


/**
 * Message instance business service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgInstanceService : IBaseCrudService<String, MsgInstance> {


    /**
     * Gets the instance cache entry by id.
     *
     * @param id instance primary key
     * @return MsgInstanceCacheEntry, or null if not found
     */
    fun getInstanceById(id: String): MsgInstanceCacheEntry?


}
