package io.kudos.ability.comm.sms.aws.handler

import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import io.kudos.ability.comm.sms.aws.model.AwsSmsCallBackParam
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkServiceException
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.ProxyConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.net.URI

/**
 * AWS SMS send handler (supports a configurable endpoint, convenient for offline testing with
 * WireMock / Testcontainers).
 *
 * @author paul
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
class AwsSmsHandler {

    /** Proxy configuration, injected by Spring; when enabled, affects the process-shared [HTTP_CLIENT]. */
    @Resource
    private lateinit var proxyProperties: SmsAwsProxyProperties

    /** Optional: override the SNS endpoint. Leave empty in production; tests inject http://host:port. */
    @Value($$"${kudos.ability.comm.sms.aws.endpoint}")
    private lateinit var endpointOverride: String

    /**
     * AWS send SMS (without callback). Equivalent to `send(smsRequest, null)`, used in
     * "fire-and-forget" scenarios where the result is not needed.
     *
     * @param smsRequest the SMS request (containing region / credentials / phone number / body)
     * @author K
     * @since 1.0.0
     */
    fun send(smsRequest: AwsSmsRequest) {
        send(smsRequest, null)
    }

    /**
     * AWS send SMS (asynchronous, virtual thread); invokes the callback once complete.
     *
     * Uses `Thread.ofVirtual()` rather than `CoroutineScope.launch` because downstream consumers of
     * this module may not yet pull in coroutines. Virtual threads also have near-zero overhead on
     * blocking IO, which is sufficient for SMS - a "call once and done" scenario.
     *
     * @param smsRequest the SMS request
     * @param callback completion callback; invoked exactly once regardless of success or failure
     *   (including the fallback `599 client error`)
     * @author K
     * @since 1.0.0
     */
    fun send(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)? = null) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    /**
     * Actual send logic (executed on a virtual thread).
     *
     * Flow: build a [SnsClient] with proxy / overridable endpoint -> assemble a [PublishRequest]
     *      (filtering null kv from messageAttributes) -> call publish and wrap the SDK response into
     *      an [AwsSmsCallBackParam] -> close the client in finally and ensure the callback fires.
     *
     * Exceptions are grouped into three categories, each mapped to a status code:
     * - [AwsServiceException]: AWS business exception (e.g. auth failure, rate limit); takes
     *   `statusCode`/`errorMessage`
     * - [SdkServiceException]: lower-level SDK service exception (non-2xx); same as above
     * - Others: local errors (DNS resolution, serialization) - no statusCode, log only
     *
     * Any code path guarantees the callback is invoked at least once to prevent upstream hangs.
     *
     * @param smsRequest the SMS request
     * @param callback optional callback
     * @author K
     * @since 1.0.0
     */
    private fun doSend(
        smsRequest: AwsSmsRequest,
        callback: ((AwsSmsCallBackParam) -> Unit)? = null
    ) {
        var snsClient: SnsClient? = null
        var cb: AwsSmsCallBackParam? = null

        try {
            // 1) Build the SnsClient (supports proxy + endpoint override)
            val credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(smsRequest.accessKeyId, smsRequest.accessKeySecret)
            )

            val builder = SnsClient.builder()
                .region(Region.of(smsRequest.region))
                .credentialsProvider(credentialsProvider)

            if (HTTP_CLIENT != null) builder.httpClient(HTTP_CLIENT)
            if (endpointOverride.isNotBlank()) {
                // Injected in tests, e.g. "http://localhost:1080"
                builder.endpointOverride(URI.create(endpointOverride.trim()))
            }

            snsClient = builder.build()

            // 2) Build the PublishRequest
            val reqBuilder = PublishRequest.builder()
                .phoneNumber(smsRequest.phoneNumber)
                .message(smsRequest.message)

            smsRequest.messageAttributes
                ?.mapNotNull { (k, v) -> if (k != null && v != null) k to v else null }
                ?.toMap()
                ?.takeIf { it.isNotEmpty() }
                ?.let { reqBuilder.messageAttributes(it) }

            val request = reqBuilder.build()

            // 3) Send and parse the response
            LOG.info("[aws] Starting to send SMS...")
            val result = snsClient.publish(request)
            val http = result.sdkHttpResponse()

            cb = AwsSmsCallBackParam().apply {
                messageId = result.messageId()
                sequenceNumber = result.sequenceNumber()
                statusCode = http.statusCode()
                statusText = http.statusText().orElse("OK")
            }
            LOG.info("[aws] SMS sent successfully, result: {0}", cb)
        } catch (e: AwsServiceException) {
            cb = AwsSmsCallBackParam().apply {
                statusCode = e.statusCode()
                statusText = e.awsErrorDetails()?.errorMessage()
                    ?: e.awsErrorDetails()?.errorCode()
                    ?: "AwsServiceException"
            }
            LOG.error(e, "[aws] Failed to send SMS (AWS service exception)")
        } catch (e: SdkServiceException) {
            cb = AwsSmsCallBackParam().apply {
                statusCode = e.statusCode()
                statusText = e.message ?: "ServiceException"
            }
            LOG.error(e, "[aws] Failed to send SMS (non-2xx response)")
        } catch (e: Exception) {
            // Other local errors (connection failure, serialization errors, etc.)
            LOG.error(e, "[aws] Failed to send SMS (local exception)")
        } finally {
            try {
                snsClient?.close()
            } catch (_: Exception) {
            }

            // Safe callback: regardless of which branch above ran, ensure an object is always passed to the callback.
            val safe = cb ?: AwsSmsCallBackParam().apply {
                statusCode = 599
                statusText = "client error"
            }
            callback?.invoke(safe)
        }
    }

    /**
     * `@PostConstruct` initialization: if the proxy is enabled, build a process-level
     * [ApacheHttpClient] for reuse. When the proxy is not enabled, no client is constructed -
     * leave the SDK with its default client to avoid introducing an extra connection pool needlessly.
     *
     * @author K
     * @since 1.0.0
     */
    @PostConstruct
    private fun initApacheHttpClient() {
        if (proxyProperties.enable) {
            LOG.info("AWS SMS send: HTTP proxy enabled")
            HTTP_CLIENT = ApacheHttpClient.builder()
                .proxyConfiguration(
                    ProxyConfiguration.builder()
                        .endpoint(URI.create(requireNotNull(proxyProperties.url) { "proxy url is null" }))
                        .username(proxyProperties.username)
                        .password(proxyProperties.password)
                        .build()
                )
                .build()
        }
    }

    /**
     * Process-wide shared `SdkHttpClient` (assigned by `initApacheHttpClient` when the proxy is enabled).
     * Changing the proxy configuration requires a process restart; if multi-tenant scenarios need
     * different proxies, a separate client factory should be designed.
     */
    companion object {
        /** Logger. */
        private val LOG = LogFactory.getLog(this::class)
        /** Process-shared SDK HTTP client; assigned only when the proxy is enabled by [initApacheHttpClient]. */
        private var HTTP_CLIENT: SdkHttpClient? = null
    }
}
