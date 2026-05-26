package io.kudos.ms.sys.core.outline.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.model.po.SysOutLine


/**
 * Outbound allowlist service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineService : IBaseCrudService<String, SysOutLine> {

    /**
     * List all enabled outbound allowlist entries under the given system/tenant. `tenantId == null` means platform-level rules.
     *
     * @param systemCode system code, non-blank
     * @param tenantId tenant id; when `null`, queries platform-level rules
     * @return list of active outbound allowlist cache entries
     */
    fun listActiveOutLines(systemCode: String, tenantId: String? = null): List<SysOutLineCacheEntry>

    /**
     * Update the enabled state.
     *
     * @param id outbound allowlist id
     * @param active whether enabled
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
