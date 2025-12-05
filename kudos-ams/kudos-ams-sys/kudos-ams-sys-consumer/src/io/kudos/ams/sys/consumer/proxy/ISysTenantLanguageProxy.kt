package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysTenantLanguageApi
import io.kudos.ams.sys.consumer.fallback.SysTenantLanguageFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-语言关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-tenantlanguage", fallback = SysTenantLanguageFallback::class)
interface ISysTenantLanguageProxy : ISysTenantLanguageApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}