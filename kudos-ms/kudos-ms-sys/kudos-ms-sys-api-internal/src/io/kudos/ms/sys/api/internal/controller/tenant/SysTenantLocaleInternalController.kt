package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.common.tenant.api.ISysTenantLocaleApi
import io.kudos.ms.sys.core.tenant.api.SysTenantLocaleApi
import org.springframework.web.bind.annotation.RestController


/**
 * 租户-语言 关联 内部 RPC 控制器。路径继承自 [ISysTenantLocaleApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysTenantLocaleInternalController(
    private val sysTenantLocaleApi: SysTenantLocaleApi,
) : ISysTenantLocaleApi {

    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> =
        sysTenantLocaleApi.getLocaleCodesByTenantId(tenantId)

    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> =
        sysTenantLocaleApi.getTenantIdsByLocaleCode(localeCode)

    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int =
        sysTenantLocaleApi.batchBind(tenantId, localeCodes)

    override fun unbind(tenantId: String, localeCode: String): Boolean =
        sysTenantLocaleApi.unbind(tenantId, localeCode)

    override fun exists(tenantId: String, localeCode: String): Boolean =
        sysTenantLocaleApi.exists(tenantId, localeCode)

}
