package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantLocaleApi
import io.kudos.ms.sys.core.service.iservice.ISysTenantLocaleService
import org.springframework.stereotype.Service


/**
 * 租户-语言关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysTenantLocaleApi(
    private val sysTenantLocaleService: ISysTenantLocaleService,
) : ISysTenantLocaleApi {

    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> =
        sysTenantLocaleService.getLocaleCodesByTenantId(tenantId)

    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> =
        sysTenantLocaleService.getTenantIdsByLocaleCode(localeCode)

    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int =
        sysTenantLocaleService.batchBind(tenantId, localeCodes)

    override fun unbind(tenantId: String, localeCode: String): Boolean =
        sysTenantLocaleService.unbind(tenantId, localeCode)

    override fun exists(tenantId: String, localeCode: String): Boolean =
        sysTenantLocaleService.exists(tenantId, localeCode)
}
