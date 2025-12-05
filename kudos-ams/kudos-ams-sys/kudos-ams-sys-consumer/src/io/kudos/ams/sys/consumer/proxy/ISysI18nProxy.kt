package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysI18nApi
import io.kudos.ams.sys.consumer.fallback.SysI18nFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 国际化客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-i18n", fallback = SysI18nFallback::class)
interface ISysI18nProxy : ISysI18nApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}