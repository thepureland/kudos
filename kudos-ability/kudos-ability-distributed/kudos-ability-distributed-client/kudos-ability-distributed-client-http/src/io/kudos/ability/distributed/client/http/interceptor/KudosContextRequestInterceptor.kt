package io.kudos.ability.distributed.client.http.interceptor

import io.kudos.ability.distributed.client.http.init.properties.HttpClientProperties
import io.kudos.ability.distributed.client.http.support.IHttpRequestContextProcess
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Global context-propagation interceptor for Spring interface clients.
 *
 * The direct port of `GlobalHeaderRequestInterceptor` from the client-feign module: Feign's
 * `RequestInterceptor` / `RequestTemplate` becomes Spring's [ClientHttpRequestInterceptor] /
 * [HttpRequest]. Header names, the trace-key write-back rule and the HMAC payload layout are all
 * **byte-for-byte identical** to the Feign version, so the provider side
 * (`InternalRpcContextWebFilter` + `InternalRpcContextSignatureVerifier` in the discovery-nacos module) accepts
 * requests from either client without any change.
 *
 * Headers added:
 * - TENANT_ID: tenant id
 * - SUB_SYS_CODE: subsystem code
 * - TRACE_KEY: trace key for distributed tracing (a UUID is generated **and written back into the
 *   context** when missing, so every outbound call in the same logical request shares one trace key)
 * - DATASOURCE_ID: data source id (only when present)
 * - LOCAL: locale in "language_country" form (defaults to zh_CN)
 * - RPC_REQUEST: internal-call marker, value "true" — the gate every provider-side context filter
 *   checks before honouring the other headers
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class KudosContextRequestInterceptor(
    private val properties: HttpClientProperties = HttpClientProperties(),
    private val clock: Clock = Clock.systemUTC(),
    private val nonceSupplier: () -> String = { RandomStringKit.uuid() }
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val context = KudosContextHolder.get()
        val headers = request.headers
        // Mirrors the Feign version: generating a trace key without writing it back would produce a
        // fresh UUID per outbound call and fragment the trace.
        val traceKey = context.traceKey?.takeIf { it.isNotBlank() }
            ?: RandomStringKit.uuid().also { context.traceKey = it }
        headers.set(Consts.RequestHeader.TENANT_ID, context.tenantId.toString())
        // Skip rather than write an empty header: `HttpHeaders.set(name, null)` still puts the header
        // on the wire with an empty value, which the provider then reads as "" instead of absent.
        // Feign's RequestTemplate drops null-valued headers, so skipping is also what keeps the two
        // transports observationally identical.
        context.subSystemCode?.let { headers.set(Consts.RequestHeader.SUB_SYS_CODE, it) }
        headers.set(Consts.RequestHeader.TRACE_KEY, traceKey)
        headers.set(Consts.RequestHeader.LOCAL, context.clientInfo?.locale?.toString() ?: "zh_CN")
        context.dataSourceId?.let { headers.set(Consts.RequestHeader.DATASOURCE_ID, it) }
        headers.set(Consts.RequestHeader.RPC_REQUEST, "true")
        signContextHeadersIfNecessary(request, headers)
        processors.forEach { it.processContext(request, context) }
        return execution.execute(request, body)
    }

    private fun signContextHeadersIfNecessary(request: HttpRequest, headers: HttpHeaders) {
        val secret = properties.contextSignatureSecret?.takeIf { it.isNotBlank() } ?: return
        val timestamp = clock.millis().toString()
        val nonce = nonceSupplier()
        headers.set(HttpContextSignature.TIMESTAMP_HEADER, timestamp)
        headers.set(HttpContextSignature.NONCE_HEADER, nonce)
        headers.set(
            HttpContextSignature.SIGNATURE_HEADER,
            hmacSha256(secret, signaturePayload(request, headers, timestamp, nonce))
        )
    }

    /**
     * Must stay in lock-step with `InternalRpcContextSignatureVerifier.signaturePayload` on the provider side:
     *
     * `method \n url \n TENANT_ID \n SUB_SYS_CODE \n TRACE_KEY \n DATASOURCE_ID \n LOCAL \n timestamp \n nonce`
     */
    private fun signaturePayload(
        request: HttpRequest,
        headers: HttpHeaders,
        timestamp: String,
        nonce: String
    ): String = listOf(
        request.method.name(),
        signedUrl(request.uri),
        headers.getFirst(Consts.RequestHeader.TENANT_ID).orEmpty(),
        headers.getFirst(Consts.RequestHeader.SUB_SYS_CODE).orEmpty(),
        headers.getFirst(Consts.RequestHeader.TRACE_KEY).orEmpty(),
        headers.getFirst(Consts.RequestHeader.DATASOURCE_ID).orEmpty(),
        headers.getFirst(Consts.RequestHeader.LOCAL).orEmpty(),
        timestamp,
        nonce
    ).joinToString("\n")

    /**
     * The signed URL is the **path (plus raw query)**, never the absolute URI.
     *
     * The provider rebuilds candidates from `HttpServletRequest.requestURI` (+ `queryString`), which is
     * always path-only; and Feign signed `RequestTemplate.url()`, which is likewise relative to the
     * service root. Path-only also makes the signature independent of interceptor ordering: the
     * LoadBalancer interceptor rewrites scheme and authority (`lb://svc` -> `http://10.0.0.1:8080`)
     * but never the path, so it does not matter whether it runs before or after this interceptor.
     */
    private fun signedUrl(uri: URI): String {
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery
        return if (query.isNullOrEmpty()) path else "$path?$query"
    }

    private fun hmacSha256(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    /**
     * Processor list resolved once on first use — Spring beans do not change after startup, and the
     * interceptor sits on the hot path of every outbound call.
     */
    private val processors: List<IHttpRequestContextProcess> by lazy {
        SpringKit.getBeansOfType<IHttpRequestContextProcess>().values
            .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
    }

}
