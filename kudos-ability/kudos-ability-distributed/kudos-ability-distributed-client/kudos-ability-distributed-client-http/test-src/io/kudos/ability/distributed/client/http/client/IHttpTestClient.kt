package io.kudos.ability.distributed.client.http.client

import io.kudos.ability.distributed.client.http.PostParam
import io.kudos.ability.distributed.client.http.PostResult
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

/**
 * Interface client under test — the `@HttpExchange` equivalent of the client-feign module's
 * `IFeignClient`.
 *
 * Note what is *absent*: no service id on the interface. The target service is bound by the group's
 * `spring.http.serviceclient.testservice.base-url = lb://test-http-service`, so the interface stays a
 * pure contract and the deployment topology lives in configuration.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@HttpExchange("/testHttpService")
interface IHttpTestClient {

    @GetExchange("/get")
    fun get(): Boolean

    @PostExchange("/post")
    fun post(@RequestBody data: PostParam): PostResult

    @GetExchange("/exception")
    fun exception()

    /** Provider sleeps 3s; used to observe which timeout actually fires. */
    @GetExchange("/slow")
    fun slow(): Boolean

    /** Returns the HMAC signature headers as the provider actually received them. */
    @GetExchange("/echoSignature")
    fun echoSignature(): Map<String, String?>

    /** Returns the context headers as the provider actually received them. */
    @GetExchange("/echoHeaders")
    fun echoHeaders(): Map<String, String?>

}
