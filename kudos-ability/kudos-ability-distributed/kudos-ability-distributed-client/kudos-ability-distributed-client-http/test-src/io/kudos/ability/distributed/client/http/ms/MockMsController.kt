package io.kudos.ability.distributed.client.http.ms

import io.kudos.ability.distributed.client.http.PostParam
import io.kudos.ability.distributed.client.http.PostResult
import io.kudos.context.support.Consts
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Microservice controller invoked through the interface client.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/testHttpService")
class MockMsController {

    @GetMapping("/get")
    fun get(): Boolean {
        return true
    }

    @PostMapping("/post")
    fun post(@RequestBody data: PostParam?): PostResult {
        return PostResult(1, true)
    }

    @GetMapping("/exception")
    fun exception() {
        throw RuntimeException("Exception thrown for testing purposes; can be ignored.")
    }

    /** Sleeps longer than any configured timeout, to test what actually cuts a slow call off. */
    @GetMapping("/slow")
    fun slow(): Boolean {
        Thread.sleep(3000)
        return true
    }

    /**
     * Echoes back the HMAC signature headers, so the test can assert the client actually signed.
     */
    @GetMapping("/echoSignature")
    fun echoSignature(
        @RequestHeader("X-Kudos-Context-Timestamp", required = false) timestamp: String?,
        @RequestHeader("X-Kudos-Context-Nonce", required = false) nonce: String?,
        @RequestHeader("X-Kudos-Context-Signature", required = false) signature: String?
    ): Map<String, String?> = mapOf(
        "timestamp" to timestamp,
        "nonce" to nonce,
        "signature" to signature,
    )

    /**
     * Echoes back the propagated context headers so the end-to-end test can assert that
     * `KudosContextRequestInterceptor` really put them on the wire (rather than only asserting them
     * in a unit test against a mocked request).
     */
    @GetMapping("/echoHeaders")
    fun echoHeaders(
        @RequestHeader(Consts.RequestHeader.TENANT_ID, required = false) tenantId: String?,
        @RequestHeader(Consts.RequestHeader.SUB_SYS_CODE, required = false) subSysCode: String?,
        @RequestHeader(Consts.RequestHeader.TRACE_KEY, required = false) traceKey: String?,
        @RequestHeader(Consts.RequestHeader.LOCAL, required = false) local: String?,
        @RequestHeader(Consts.RequestHeader.RPC_REQUEST, required = false) internalMarker: String?
    ): Map<String, String?> = mapOf(
        "tenantId" to tenantId,
        "subSysCode" to subSysCode,
        "traceKey" to traceKey,
        "local" to local,
        "internalMarker" to internalMarker,
    )

}
