package io.kudos.ms.sys.api.internal.controller.outline

import io.kudos.ms.sys.common.outline.api.ISysOutLineApi
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.api.SysOutLineApi
import org.springframework.web.bind.annotation.RestController


/**
 * Outbound whitelist internal RPC controller. Paths are inherited from method-level annotations on [ISysOutLineApi].
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
