package io.kudos.ms.sys.api.internal.controller.outline

import io.kudos.ms.sys.common.outline.api.ISysOutLineApi
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.api.SysOutLineApi
import org.springframework.web.bind.annotation.RestController


/**
 * 出网白名单 内部 RPC 控制器。路径继承自 [ISysOutLineApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysOutLineInternalController(
    private val sysOutLineApi: SysOutLineApi,
) : ISysOutLineApi {

    override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> =
        sysOutLineApi.listOutLines(systemCode, tenantId)

}
