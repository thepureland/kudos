package io.kudos.ms.sys.core.outline.api

import io.kudos.ms.sys.common.outline.api.ISysOutLineApi
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * Local implementation of the outbound allowlist API
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Component
open class SysOutLineApi(
    private val sysOutLineService: ISysOutLineService,
) : ISysOutLineApi {

    override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> =
        sysOutLineService.listActiveOutLines(systemCode, tenantId)
}
