package io.kudos.ms.msg.core.instance.api

import io.kudos.ms.msg.common.instance.api.IMsgInstanceApi
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * 消息实例API本地实现。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class MsgInstanceApi : IMsgInstanceApi {

    @Resource
    private lateinit var msgInstanceService: IMsgInstanceService

    override fun getInstanceById(id: String): MsgInstanceCacheEntry? =
        msgInstanceService.getInstanceById(id)

}
