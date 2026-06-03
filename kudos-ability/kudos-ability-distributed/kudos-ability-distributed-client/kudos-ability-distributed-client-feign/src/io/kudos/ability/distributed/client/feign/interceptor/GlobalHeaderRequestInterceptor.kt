package io.kudos.ability.distributed.client.feign.interceptor

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.init.properties.OpenFeignProperties
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Base64
import io.kudos.base.lang.string.RandomStringKit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Global Feign request interceptor.
 *
 * Automatically adds KudosContext data (tenant id, subsystem code, trace key, etc.) to Feign request headers.
 *
 * Core features:
 * 1. Context propagation: forwards the current thread's KudosContext through Feign headers for cross-service propagation.
 * 2. Trace key generation: if the context has no trace key, a UUID is generated and used.
 * 3. Extensibility: header handling can be extended through the [IFeignRequestContextProcess] SPI —
 *    designed for data that "must cross services but lives outside KudosContext". One production implementation today:
 *    `SeataFeignXidProcessor` (in the `kudos-ability-distributed-tx-seata` module) writes Seata's
 *    `RootContext.getXID()` into the `TX_XID` header so the callee can bind back to the same global
 *    transaction context. With both ends cooperating, `@GlobalTransactional` distributed transactions
 *    can register branches and roll back correctly.
 *
 * Headers added:
 * - TENANT_ID: tenant id
 * - SUB_SYS_CODE: subsystem code
 * - TRACE_KEY: trace key for distributed tracing
 * - DATASOURCE_ID: data source id (if present)
 * - LOCAL: locale, defaults to zh_CN
 * - FEIGN_REQUEST: marker for Feign requests, value "true"
 *
 * Workflow:
 * - Read the current context from KudosContextHolder.
 * - Extract context data.
 * - Generate a UUID trace key if none is present.
 * - Write the data to request headers.
 * - Invoke extension processors for additional header handling.
 *
 * Notes:
 * - This interceptor applies to all Feign requests.
 * - A trace key is auto-generated when empty so every request carries a trace identifier.
 * - The locale defaults to zh_CN when not set.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class GlobalHeaderRequestInterceptor(
    private val properties: OpenFeignProperties = OpenFeignProperties(),
    private val clock: Clock = Clock.systemUTC(),
    private val nonceSupplier: () -> String = { RandomStringKit.uuid() }
) : RequestInterceptor {

    /**
     * Apply the interceptor: add context information to Feign request headers.
     *
     * Extracts context from KudosContext and writes it to the Feign headers to propagate context across services.
     *
     * Workflow:
     * 1. Read the current thread's KudosContext.
     * 2. Extract context fields: tenant id, subsystem code, trace key, data source id, locale.
     * 3. Trace key handling: if empty, generate a UUID as the trace key.
     * 4. Header injection: write all context fields into request headers.
     * 5. Extension hook: invoke every IFeignRequestContextProcess implementation for extra header handling.
     *
     * Headers added:
     * - TENANT_ID: tenant id (string form)
     * - SUB_SYS_CODE: subsystem code
     * - TRACE_KEY: trace key for distributed tracing (UUID is generated when empty)
     * - DATASOURCE_ID: data source id (when present)
     * - LOCAL: locale in "language_country" form (defaults to zh_CN when missing)
     * - FEIGN_REQUEST: Feign-request marker, value "true"
     *
     * Extension mechanism:
     * - Header handling can be extended via the [IFeignRequestContextProcess] SPI.
     * - Every implementation is invoked and may add extra headers.
     * - Primary use case: state that must cross services but lives outside [io.kudos.context.core.KudosContext]
     *   in another thread-local (e.g. Seata's `RootContext` holds the global transaction XID).
     *   Existing implementation: `SeataFeignXidProcessor` (`kudos-ability-distributed-tx-seata` module)
     *   injects the `TX_XID` header; the server-side `SeataXidServletFilter` binds it back into RootContext.
     *
     * Notes:
     * - This interceptor applies to all Feign requests.
     * - A trace key is auto-generated when empty so every request carries a trace identifier.
     * - The locale defaults to zh_CN when not set.
     * - The data source id is optional and is only added when present.
     *
     * @param requestTemplate the Feign request template used to add headers
     */
    override fun apply(requestTemplate: RequestTemplate) {
        // Read tenantId and subSysCode from the current context.
        val context = KudosContextHolder.get()
        val tenantId = context.tenantId
        val subSysCode = context.subSystemCode
        // When traceKey is missing, generate one and **write it back to context** — every subsequent
        // outbound Feign call within the same logical request shares the same traceKey so downstream
        // tracing can stitch them into one trace. The previous implementation only generated without
        // writing back, producing a new UUID per outbound call and fragmenting the trace.
        val traceKey = context.traceKey?.takeIf { it.isNotBlank() }
            ?: RandomStringKit.uuid().also { context.traceKey = it }
        val dataSourceId = context.dataSourceId
        requestTemplate.header(Consts.RequestHeader.TENANT_ID, tenantId.toString())
        requestTemplate.header(Consts.RequestHeader.SUB_SYS_CODE, subSysCode)
        requestTemplate.header(Consts.RequestHeader.TRACE_KEY, traceKey)
        val localeStr = context.clientInfo?.locale?.toString() ?: "zh_CN"
        requestTemplate.header(Consts.RequestHeader.LOCAL, localeStr)
        if (dataSourceId != null) {
            requestTemplate.header(Consts.RequestHeader.DATASOURCE_ID, dataSourceId)
        }
        requestTemplate.header(Consts.RequestHeader.FEIGN_REQUEST, "true")
        signContextHeadersIfNecessary(requestTemplate)
        // The previous implementation called `SpringKit.getBeansOfType` per request — overhead is small
        // when there are few beans, but Feign repeatedly hits the hot path, so reflection + map building
        // still shows up. Spring beans do not change after startup; cache once into a lazy field.
        // `@Volatile` is the default thread-safety guarantee of Kotlin lazy; SpringKit must already be
        // ready by the first access (same constraint as LockTool).
        processors.forEach { it.processContext(requestTemplate, context) }
    }

    private fun signContextHeadersIfNecessary(requestTemplate: RequestTemplate) {
        val secret = properties.contextSignatureSecret?.takeIf { it.isNotBlank() } ?: return
        val timestamp = clock.millis().toString()
        val nonce = nonceSupplier()
        requestTemplate.header(FeignContextSignature.TIMESTAMP_HEADER, timestamp)
        requestTemplate.header(FeignContextSignature.NONCE_HEADER, nonce)
        requestTemplate.header(
            FeignContextSignature.SIGNATURE_HEADER,
            hmacSha256(secret, signaturePayload(requestTemplate, timestamp, nonce))
        )
    }

    private fun signaturePayload(requestTemplate: RequestTemplate, timestamp: String, nonce: String): String =
        listOf(
            requestTemplate.method().orEmpty(),
            requestTemplate.url().orEmpty(),
            firstHeader(requestTemplate, Consts.RequestHeader.TENANT_ID),
            firstHeader(requestTemplate, Consts.RequestHeader.SUB_SYS_CODE),
            firstHeader(requestTemplate, Consts.RequestHeader.TRACE_KEY),
            firstHeader(requestTemplate, Consts.RequestHeader.DATASOURCE_ID),
            firstHeader(requestTemplate, Consts.RequestHeader.LOCAL),
            timestamp,
            nonce
        ).joinToString("\n")

    private fun firstHeader(requestTemplate: RequestTemplate, name: String): String =
        requestTemplate.headers()[name]?.firstOrNull().orEmpty()

    private fun hmacSha256(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    /**
     * Processor list resolved once at startup — `by lazy` ensures Spring is ready on first access.
     *
     * Uses Spring's standard ordering rules to honour `Ordered` / `@Order`, so that when multiple
     * processors write to the same header the behaviour does not depend on container return order.
     */
    private val processors: List<IFeignRequestContextProcess> by lazy {
        SpringKit.getBeansOfType<IFeignRequestContextProcess>().values
            .sortedWith(AnnotationAwareOrderComparator.INSTANCE)
    }

}
