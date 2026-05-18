package io.kudos.ms.user.client.passport.proxy

import io.kudos.ms.user.client.passport.fallback.PassportFallback
import io.kudos.ms.user.common.passport.api.IPassportApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 登录通行证客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "user-passport", fallback = PassportFallback::class)
interface IPassportProxy : IPassportApi
