package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantLocaleApi
import io.kudos.ms.sys.core.service.iservice.ISysTenantLocaleService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 租户-语言关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysTenantLocaleApi : ISysTenantLocaleApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysTenantLocaleService: ISysTenantLocaleService

    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> {
        return sysTenantLocaleService.getLocaleCodesByTenantId(tenantId)
    }

    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> {
        return sysTenantLocaleService.getTenantIdsByLocaleCode(localeCode)
    }

    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
        return sysTenantLocaleService.batchBind(tenantId, localeCodes)
    }

    override fun unbind(tenantId: String, localeCode: String): Boolean {
        return sysTenantLocaleService.unbind(tenantId, localeCode)
    }

    override fun exists(tenantId: String, localeCode: String): Boolean {
        return sysTenantLocaleService.exists(tenantId, localeCode)
    }

    //endregion your codes 2

}