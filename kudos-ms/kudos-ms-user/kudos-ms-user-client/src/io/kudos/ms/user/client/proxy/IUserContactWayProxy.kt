package io.kudos.ms.user.client.proxy

import io.kudos.ms.user.client.fallback.UserContactWayFallback
import io.kudos.ms.user.common.api.IUserContactWayApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户联系方式客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "user-contact-way", fallback = UserContactWayFallback::class)
interface IUserContactWayProxy : IUserContactWayApi {



}
