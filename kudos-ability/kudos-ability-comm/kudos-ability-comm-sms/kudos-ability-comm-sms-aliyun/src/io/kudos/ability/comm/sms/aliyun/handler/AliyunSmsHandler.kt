package io.kudos.ability.comm.sms.aliyun.handler

import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponseBody
import darabonba.core.client.ClientOverrideConfiguration
import io.kudos.ability.comm.sms.aliyun.model.AliyunSmsRequest
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Value
import java.net.URI

/**
 * Aliyun SMS send handler (supports a configurable endpoint, convenient for injecting WireMock in tests).
 *
 * Configuration (prefers `kudos.ability.comm.sms.aliyun.endpoint`, falls back to legacy key
 * `kudos.ability.comm.sms.aliyun`):
 * - Production may leave it empty (the SDK uses the official domain based on region)
 * - Tests should set a full URI such as `http://<wiremock-host>:<port>` (do not omit the scheme;
 *   local Mocks require an explicit `http`)
 *
 * @author K
 * @since 1.0.0
 */
class AliyunSmsHandler {

    /**
     * Custom SDK endpoint, only for use in test / special network environments.
     * Configuration syntax: `${kudos.ability.comm.sms.aliyun.endpoint}` takes precedence; when
     * missing, it falls back to the legacy key `${kudos.ability.comm.sms.aliyun}` for historical
     * compatibility. If neither is set, the value is an empty string and the SDK uses the official
     * domain based on the region.
     */
    @Value("\${kudos.ability.comm.sms.aliyun.endpoint:\${kudos.ability.comm.sms.aliyun:}}")
    private lateinit var endpointOverrideStr: String

    /**
     * Asynchronously send an SMS.
     *
     * Sends the SMS asynchronously on a virtual thread and returns the result via a callback when done.
     *
     * Workflow:
     * 1. Create a virtual thread: use Thread.ofVirtual() to create a lightweight virtual thread.
     * 2. Asynchronous execution: run doSend inside the virtual thread.
     * 3. Non-blocking: the calling thread returns immediately and does not wait for the send to complete.
     *
     * Advantages of virtual threads:
     * - Lightweight: virtual threads consume fewer resources than traditional threads.
     * - High concurrency: a large number of virtual threads can be created to handle concurrent requests.
     * - Suitable for IO-bound tasks: SMS sending is a network IO operation, ideal for virtual threads.
     *
     * Notes:
     * - The send result is returned via the callback; it does not block the calling thread.
     * - Even if the send fails, the error information is returned via the callback.
     *
     * @param smsRequest the SMS send request object
     * @param callback callback invoked after the send completes; receives the SendSmsResponseBody result
     */
    fun send(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    /**
     * Execute the SMS send.
     *
     * Builds the Aliyun client, sends the SMS request, and ensures resources are properly released.
     *
     * Workflow:
     * 1. Build the client: construct an AsyncClient based on the request parameters.
     * 2. Build the request: create a SendSmsRequest object and set the phone number, sign, template, etc.
     * 3. Send the request: call client.sendSms to send the SMS.
     * 4. Get the result: await the response and obtain the response body.
     * 5. Release resources: close the client in a finally block.
     * 6. Callback notification: regardless of success or failure, return the result via the callback.
     *
     * Exception handling:
     * - Catch all exceptions and log the error.
     * - Even when an exception occurs, build a safe response body and invoke the callback.
     * - Ensure client resources are properly released in the finally block.
     *
     * Safe response body:
     * - On success, return the actual response body.
     * - On failure, build a response body that contains the error information.
     * - The response body's code is "EXCEPTION" and the message is the exception information.
     * - Ensure the callback always receives a valid response body.
     *
     * Resource management:
     * - Use try-finally to ensure the client is always closed.
     * - Exceptions during client close are ignored and do not affect the main flow.
     *
     * Notes:
     * - response.get() is a blocking call that waits for the async request to complete.
     * - The client close operation is executed in the finally block to ensure resource release.
     * - The callback is always invoked, even when an exception occurs.
     *
     * @param smsRequest the SMS send request object
     * @param callback callback invoked after the send completes
     */
    private fun doSend(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        var client: AsyncClient? = null
        var result: SendSmsResponseBody? = null
        var lastError: Exception? = null

        try {
            client = buildClient(
                region = smsRequest.region,
                accessKeyId = smsRequest.accessKeyId,
                accessKeySecret = smsRequest.accessKeySecret
            )

            val request = SendSmsRequest.builder()
                .phoneNumbers(smsRequest.phoneNumbers)
                .signName(smsRequest.signName)
                .templateCode(smsRequest.templateCode)
                .templateParam(smsRequest.templateParam)
                .build()

            LOG.info("[aliyun] Starting to send SMS...")
            val response = client.sendSms(request)
            result = response.get()?.body
            LOG.debug("[aliyun] SMS sent successfully, code={0}, requestId={1}", result?.code, result?.requestId)
        } catch (e: Exception) {
            lastError = e
            LOG.error(e, "[aliyun] Failed to send SMS")
        } finally {
            try { client?.close() } catch (_: Exception) { /* ignore */ }

            // Always invoke the callback: the exception branch returns a safe response body to avoid an uninitialized lateinit.
            val safe = result ?: SendSmsResponseBody.builder()
                .code("EXCEPTION")
                .message(lastError?.message ?: "unknown error")
                .requestId("local-test")
                .build()

            callback.invoke(safe)
        }
    }

    /**
     * Build the Aliyun SMS async client.
     *
     * Builds an AsyncClient based on configuration, supporting a custom endpoint (for test
     * environments such as WireMock).
     *
     * Workflow:
     * 1. Create the credentials provider: build a StaticCredentialProvider from the AccessKey ID and Secret.
     * 2. Build the client builder: set the region and the credentials provider.
     * 3. Process the custom endpoint (if configured):
     *    - Parse endpointOverrideStr (supports multiple formats)
     *    - Extract the protocol (http/https)
     *    - Extract the host and port
     *    - Create a ClientOverrideConfiguration and apply it
     * 4. Build and return the client.
     *
     * Supported endpoint formats:
     * - Full URI: "http://localhost:8080" or "https://dysmsapi.aliyuncs.com"
     * - Host:port: "localhost:8080"
     * - Pure domain: "dysmsapi.aliyuncs.com"
     *
     * Protocol handling:
     * - If the URI contains a scheme, use that scheme (uppercased).
     * - If there is no scheme (only host[:port]), default to **https**; local WireMock and similar
     *   should use a full `http://...`.
     *
     * Port handling:
     * - If the URI contains a port, use that port.
     * - If the URI does not contain a port (port=-1), use only the host name.
     * - If the URI cannot be parsed, use the original string (trimmed).
     *
     * Usage scenarios:
     * - Production: use the Aliyun default endpoint.
     * - Test: use Mock services such as WireMock.
     * - Development: can use a local proxy.
     *
     * Notes:
     * - When endpointOverrideStr is empty, the Aliyun default endpoint is used.
     * - When there is no scheme, defaults to https; tests should use a full URI with `http://`.
     * - On URI parse failure, falls back to the original string.
     *
     * @param region region code (such as "cn-hangzhou")
     * @param accessKeyId Aliyun AccessKey ID
     * @param accessKeySecret Aliyun AccessKey Secret
     * @return a configured AsyncClient instance
     */
    private fun buildClient(
        region: String?,
        accessKeyId: String?,
        accessKeySecret: String?
    ): AsyncClient {
        val provider = StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build()
        )

        val builder = AsyncClient.builder()
            .region(region)
            .credentialsProvider(provider)

        // If a custom endpoint is configured (e.g. WireMock), override it.
        if (endpointOverrideStr.isNotBlank()) {
            // Supports both "http://host:port" and "host:port" / a bare domain.
            val uri = runCatching { URI(endpointOverrideStr) }.getOrNull()
            val protocol = (uri?.scheme ?: "https").uppercase()
            val hostPort = when {
                uri == null -> endpointOverrideStr.trim()
                uri.port == -1 -> uri.host
                else -> "${uri.host}:${uri.port}"
            }

            val override = ClientOverrideConfiguration.create()
                .setEndpointOverride(hostPort) // e.g. "localhost:8080" or "dysmsapi.aliyuncs.com"
                .setProtocol(protocol)         // "HTTP" or "HTTPS"

            builder.overrideConfiguration(override)
        }

        return builder.build()
    }

    /** Logger. */
    private val LOG = LogFactory.getLog(this::class)
}
