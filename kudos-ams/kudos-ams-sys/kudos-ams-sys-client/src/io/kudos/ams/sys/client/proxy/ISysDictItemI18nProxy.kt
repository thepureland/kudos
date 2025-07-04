package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysDictItemI18nApi
import io.kudos.ams.sys.client.fallback.SysDictItemI18nFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 字典项国际化客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-dictitemi18n", fallback = SysDictItemI18nFallback::class)
interface ISysDictItemI18nProxy : ISysDictItemI18nApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}