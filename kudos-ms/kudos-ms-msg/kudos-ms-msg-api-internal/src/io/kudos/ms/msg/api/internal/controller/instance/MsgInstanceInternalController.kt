package io.kudos.ms.msg.api.internal.controller.instance

import io.kudos.ms.msg.common.instance.api.IMsgInstanceApi
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.api.MsgInstanceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 消息实例 内部 RPC 控制器。路径继承自 [IMsgInstanceApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class MsgInstanceInternalController(
    private val msgInstanceApi: MsgInstanceApi,
) : IMsgInstanceApi {

    override fun getInstanceById(id: String): MsgInstanceCacheEntry? =
        msgInstanceApi.getInstanceById(id)

}
