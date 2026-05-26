package io.kudos.ms.user.client.passport.proxy

import io.kudos.ms.user.client.passport.fallback.PassportFallback
import io.kudos.ms.user.common.passport.api.IPassportApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Login passport client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "user-passport", fallback = PassportFallback::class)
interface IPassportProxy : IPassportApi
