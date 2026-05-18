package io.kudos.ms.msg.core.instance.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.model.po.MsgInstance


/**
 * 消息实例业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgInstanceService : IBaseCrudService<String, MsgInstance> {


    /**
     * 根据id获取实例缓存项。
     *
     * @param id 实例主键
     * @return MsgInstanceCacheEntry，找不到返回 null
     */
    fun getInstanceById(id: String): MsgInstanceCacheEntry?


}
