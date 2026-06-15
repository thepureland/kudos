package io.kudos.ability.comm.sms.aws.handler

import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import io.kudos.ability.comm.sms.aws.model.AwsSmsCallBackParam
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkServiceException
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.SnsClientBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AwsSmsHandler（純單元測試，不依賴 Spring 容器、Docker 與真實 AWS 服務）
 *
 * 覆蓋：
 * - doSend 成功路徑（Mockito mockStatic 攔截 SnsClient.builder）：statusText 缺失回退 "OK"、
 *   messageAttributes 為 null / 含 null 鍵值被過濾 / 全部過濾後為空時不設置屬性
 * - doSend 異常映射：AwsServiceException（errorMessage / errorCode / 全空回退 "AwsServiceException"）、
 *   SdkServiceException（message 非空 / null 回退 "ServiceException"）、
 *   本地異常（cb 為 null 時兜底 599 + "client error"）、close 拋異常被吞
 * - send 兩個公開入口（虛擬線程異步回調；endpoint 指向本機保留端口必然連接失敗 → 599 安全回調）
 * - initApacheHttpClient 的代理分支：未啟用不構建；啟用構建共享 HTTP_CLIENT（隨後 doSend 走
 *   httpClient(HTTP_CLIENT) 分支與 endpoint 空白不覆蓋分支）；啟用但 url 為 null 拋 IllegalArgumentException
 *
 * 成功發送的容器級鏈路（WireMock 模擬 AWS SNS）由 AwsSmsTest 集成測試覆蓋。
 *
 * proxyProperties / endpointOverride 為 @Resource / @Value 注入的私有字段，測試中通過反射注入；
 * 進程級靜態 HTTP_CLIENT 每個測試結束後重置為 null，避免污染其他測試（含 WireMock 集成測試）。
 *
 * @author K
 * @since 1.0.0
 */
internal class AwsSmsHandlerTest {

    // ---------------- 反射輔助 ----------------

    private fun newHandler(
        endpoint: String = "",
        proxy: SmsAwsProxyProperties = SmsAwsProxyProperties()
    ): AwsSmsHandler {
        val handler = AwsSmsHandler()
        setField(handler, "endpointOverride", endpoint)
        setField(handler, "proxyProperties", proxy)
        return handler
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val field = AwsSmsHandler::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun staticHttpClient(): SdkHttpClient? {
        val field = AwsSmsHandler::class.java.getDeclaredField("HTTP_CLIENT")
        field.isAccessible = true
        return field.get(null) as SdkHttpClient?
    }

    private fun resetStaticHttpClient() {
        val field = AwsSmsHandler::class.java.getDeclaredField("HTTP_CLIENT")
        field.isAccessible = true
        try {
            (field.get(null) as? SdkHttpClient)?.close()
        } catch (_: Exception) {
        }
        field.set(null, null)
    }

    /** 防止任何測試遺留進程級共享 HTTP_CLIENT，污染後續（集成）測試。 */
    @AfterTest
    fun tearDown() = resetStaticHttpClient()

    private fun validRequest() = AwsSmsRequest().apply {
        region = "us-east-1"
        accessKeyId = "fake-ak"
        accessKeySecret = "fake-sk"
        phoneNumber = "+15550100"
        message = "hello unit test"
    }

    /** 同步調用 doSend 並取回唯一一次回調參數。 */
    private fun doSendAndGetCallback(handler: AwsSmsHandler, request: AwsSmsRequest): AwsSmsCallBackParam {
        val callbacks = mutableListOf<AwsSmsCallBackParam>()
        handler.doSend(request) { callbacks.add(it) }
        assertEquals(1, callbacks.size, "callback 應且僅應被調用一次")
        return callbacks.first()
    }

    /**
     * 在 mockStatic(SnsClient) 環境下執行 [block]：builder 鏈返回自身，build() 返回 mock 的 client。
     * 注意 mockStatic 僅對當前線程生效，因此測試直接同步調用 internal 的 doSend。
     */
    private fun withMockedSnsClient(block: (SnsClient) -> Unit) {
        Mockito.mockStatic(SnsClient::class.java).use { statics ->
            val builder = Mockito.mock(SnsClientBuilder::class.java, Mockito.RETURNS_SELF)
            val client = Mockito.mock(SnsClient::class.java)
            Mockito.`when`(builder.build()).thenReturn(client)
            statics.`when`<SnsClientBuilder> { SnsClient.builder() }.thenReturn(builder)
            block(client)
        }
    }

    private fun publishResponse(
        messageId: String,
        sequenceNumber: String,
        statusCode: Int,
        statusText: String? = null
    ): PublishResponse {
        val httpBuilder = SdkHttpResponse.builder().statusCode(statusCode)
        if (statusText != null) httpBuilder.statusText(statusText)
        return PublishResponse.builder()
            .messageId(messageId)
            .sequenceNumber(sequenceNumber)
            .sdkHttpResponse(httpBuilder.build())
            .build() as PublishResponse
    }

    // ---------------- doSend：成功路徑（mock SnsClient） ----------------

    @Test
    fun doSendSuccessWithoutAttributesFallsBackToOkStatusText() {
        withMockedSnsClient { client ->
            // statusText 缺失：orElse("OK") 分支
            Mockito.`when`(client.publish(any(PublishRequest::class.java)))
                .thenReturn(publishResponse("mid-1", "seq-1", 200))

            val request = validRequest().apply { messageAttributes = null }
            val callback = doSendAndGetCallback(newHandler(), request)

            assertEquals(200, callback.statusCode)
            assertEquals("OK", callback.statusText)
            assertEquals("mid-1", callback.messageId)
            assertEquals("seq-1", callback.sequenceNumber)

            // messageAttributes 為 null 時不應設置屬性
            val captor = ArgumentCaptor.forClass(PublishRequest::class.java)
            Mockito.verify(client).publish(captor.capture())
            assertFalse(captor.value.hasMessageAttributes())
            assertEquals("+15550100", captor.value.phoneNumber())
            assertEquals("hello unit test", captor.value.message())
            Mockito.verify(client).close()
        }
    }

    @Test
    fun doSendSuccessFiltersNullKeyValueAttributes() {
        withMockedSnsClient { client ->
            Mockito.`when`(client.publish(any(PublishRequest::class.java)))
                .thenReturn(publishResponse("mid-2", "seq-2", 200, "Accepted"))

            val valid = MessageAttributeValue.builder().dataType("String").stringValue("KUDOS").build()
            val request = validRequest().apply {
                messageAttributes = mutableMapOf(
                    null to valid,                      // null 鍵被過濾
                    "AWS.SNS.SMS.SMSType" to null,      // null 值被過濾
                    "AWS.SNS.SMS.SenderID" to valid     // 保留
                )
            }
            val callback = doSendAndGetCallback(newHandler(), request)

            assertEquals(200, callback.statusCode)
            assertEquals("Accepted", callback.statusText)

            val captor = ArgumentCaptor.forClass(PublishRequest::class.java)
            Mockito.verify(client).publish(captor.capture())
            assertTrue(captor.value.hasMessageAttributes())
            assertEquals(mapOf("AWS.SNS.SMS.SenderID" to valid), captor.value.messageAttributes())
        }
    }

    @Test
    fun doSendSkipsAttributesWhenAllEntriesFilteredOut() {
        withMockedSnsClient { client ->
            Mockito.`when`(client.publish(any(PublishRequest::class.java)))
                .thenReturn(publishResponse("mid-3", "seq-3", 200))

            val valid = MessageAttributeValue.builder().dataType("String").stringValue("X").build()
            val request = validRequest().apply {
                // 全部鍵值對都含 null：過濾後為空 map，takeIf 不滿足 → 不設置屬性
                messageAttributes = mutableMapOf(null to valid, "k" to null)
            }
            doSendAndGetCallback(newHandler(), request)

            val captor = ArgumentCaptor.forClass(PublishRequest::class.java)
            Mockito.verify(client).publish(captor.capture())
            assertFalse(captor.value.hasMessageAttributes())
        }
    }

    @Test
    fun doSendWithoutCallbackCompletesSilently() {
        withMockedSnsClient { client ->
            // 省略 callback 實參：走 Kotlin 默認參數（callback = null），回調安全跳過
            Mockito.`when`(client.publish(any(PublishRequest::class.java)))
                .thenReturn(publishResponse("mid-4", "seq-4", 200))

            newHandler().doSend(validRequest())

            Mockito.verify(client).publish(any(PublishRequest::class.java))
            Mockito.verify(client).close()
        }
    }

    // ---------------- doSend：異常映射分支 ----------------

    @Test
    fun doSendMapsAwsServiceExceptionWithErrorMessage() {
        withMockedSnsClient { client ->
            val exception = AwsServiceException.builder()
                .statusCode(429)
                .awsErrorDetails(
                    AwsErrorDetails.builder().errorCode("Throttled").errorMessage("Too Many Requests").build()
                )
                .build()
            Mockito.`when`(client.publish(any(PublishRequest::class.java))).thenThrow(exception)

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(429, callback.statusCode)
            assertEquals("Too Many Requests", callback.statusText)
            assertNull(callback.messageId)
        }
    }

    @Test
    fun doSendMapsAwsServiceExceptionWithErrorCodeOnly() {
        withMockedSnsClient { client ->
            // errorMessage 為 null：回退 errorCode
            val exception = AwsServiceException.builder()
                .statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
                .build()
            Mockito.`when`(client.publish(any(PublishRequest::class.java))).thenThrow(exception)

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(403, callback.statusCode)
            assertEquals("AccessDenied", callback.statusText)
        }
    }

    @Test
    fun doSendMapsAwsServiceExceptionWithoutErrorDetails() {
        withMockedSnsClient { client ->
            // awsErrorDetails 整體為 null：回退固定文案
            val exception = AwsServiceException.builder().statusCode(500).build()
            Mockito.`when`(client.publish(any(PublishRequest::class.java))).thenThrow(exception)

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(500, callback.statusCode)
            assertEquals("AwsServiceException", callback.statusText)
        }
    }

    @Test
    fun doSendMapsSdkServiceExceptionWithMessage() {
        withMockedSnsClient { client ->
            val exception = SdkServiceException.builder()
                .statusCode(503)
                .message("Service Unavailable")
                .build()
            Mockito.`when`(client.publish(any(PublishRequest::class.java))).thenThrow(exception)

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(503, callback.statusCode)
            assertEquals("Service Unavailable", callback.statusText)
        }
    }

    @Test
    fun doSendMapsSdkServiceExceptionWithoutMessage() {
        withMockedSnsClient { client ->
            // message 為 null：回退 "ServiceException"
            val exception = SdkServiceException.builder().statusCode(502).build()
            Mockito.`when`(client.publish(any(PublishRequest::class.java))).thenThrow(exception)

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(502, callback.statusCode)
            assertEquals("ServiceException", callback.statusText)
        }
    }

    @Test
    fun doSendFallsBackTo599OnLocalExceptionAndSwallowsCloseFailure() {
        withMockedSnsClient { client ->
            // 本地異常（非 SDK 服務異常）：cb 保持 null → finally 兜底 599 + "client error"
            Mockito.`when`(client.publish(any(PublishRequest::class.java)))
                .thenThrow(RuntimeException("local boom"))
            // close 拋異常必須被吞掉，不影響安全回調
            Mockito.doThrow(RuntimeException("close boom")).`when`(client).close()

            val callback = doSendAndGetCallback(newHandler(), validRequest())

            assertEquals(599, callback.statusCode)
            assertEquals("client error", callback.statusText)
            assertNull(callback.messageId)
            assertNull(callback.sequenceNumber)
        }
    }

    // ---------------- send：異步公開入口（真實 SnsClient，端點不可達） ----------------

    @Test
    fun sendToUnreachableEndpointInvokesCallbackWith599() {
        // endpoint 指向本機保留端口（無監聽），連接必然失敗 → 走本地異常分支 → 599 安全回調；
        // 同時覆蓋 endpointOverride.isNotBlank() 為 true 的 override 分支（含 trim）
        val handler = newHandler(endpoint = "  http://127.0.0.1:1  ")
        val callbackRef = AtomicReference<AwsSmsCallBackParam>()
        val latch = CountDownLatch(1)

        handler.send(validRequest()) { cb ->
            callbackRef.set(cb)
            latch.countDown()
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "callback 應在 60 秒內被執行")
        val callback = callbackRef.get()
        assertNotNull(callback)
        assertEquals(599, callback.statusCode)
        assertEquals("client error", callback.statusText)
    }

    @Test
    fun sendSingleArgDelegatesWithoutCallback() {
        // 單參重載等價於 send(request, null)：無回調可觀察，僅驗證調用不拋異常（fire-and-forget）
        val handler = newHandler(endpoint = "http://127.0.0.1:1")
        handler.send(validRequest())
    }

    @Test
    fun sendTwoArgDefaultCallbackParameterIsReachableViaSynthetic() {
        // send(smsRequest, callback = null) 的默認值從源碼無法觸發（單參重載在決議時優先），
        // 通過 JVM 合成方法 send$default 調用以驗證默認參數行為（fire-and-forget，不拋異常）
        val handler = newHandler(endpoint = "http://127.0.0.1:1")
        val method = AwsSmsHandler::class.java.getDeclaredMethod(
            "send\$default",
            AwsSmsHandler::class.java,
            AwsSmsRequest::class.java,
            Function1::class.java,
            Int::class.javaPrimitiveType,
            Any::class.java
        )
        // mask=2：第 2 個參數（callback）使用默認值 null
        method.invoke(null, handler, validRequest(), null, 2, null)
    }

    // ---------------- initApacheHttpClient：代理分支 ----------------

    @Test
    fun initApacheHttpClientDoesNothingWhenProxyDisabled() {
        val handler = newHandler(proxy = SmsAwsProxyProperties()) // enable 默認 false

        handler.initApacheHttpClient()

        assertNull(staticHttpClient(), "代理未啟用時不應構建共享 HTTP_CLIENT")
    }

    @Test
    fun initApacheHttpClientBuildsSharedClientWhenProxyEnabled() {
        val proxy = SmsAwsProxyProperties().apply {
            enable = true
            url = "http://127.0.0.1:18888"
            username = "user"
            password = "p@ss"
        }
        val handler = newHandler(proxy = proxy)

        try {
            handler.initApacheHttpClient()
            assertNotNull(staticHttpClient(), "代理啟用時應構建進程級共享 HTTP_CLIENT")
        } finally {
            resetStaticHttpClient()
        }
    }

    @Test
    fun initApacheHttpClientFailsWhenProxyUrlMissing() {
        val proxy = SmsAwsProxyProperties().apply {
            enable = true
            url = null
        }
        val handler = newHandler(proxy = proxy)

        val exception = assertFailsWith<IllegalArgumentException> { handler.initApacheHttpClient() }
        assertEquals("proxy url is null", exception.message)
        assertNull(staticHttpClient())
    }

    @Test
    fun sendUsesSharedHttpClientWhenProxyEnabled() {
        // 代理指向本機保留端口：doSend 走 httpClient(HTTP_CLIENT) 分支與 endpoint 空白不覆蓋分支，
        // 經代理連接必然失敗（無真實外網訪問）→ 599 安全回調
        val proxy = SmsAwsProxyProperties().apply {
            enable = true
            url = "http://127.0.0.1:1"
        }
        val handler = newHandler(endpoint = "", proxy = proxy)

        try {
            handler.initApacheHttpClient()
            assertNotNull(staticHttpClient())

            val callbackRef = AtomicReference<AwsSmsCallBackParam>()
            val latch = CountDownLatch(1)
            handler.send(validRequest()) { cb ->
                callbackRef.set(cb)
                latch.countDown()
            }

            assertTrue(latch.await(60, TimeUnit.SECONDS), "callback 應在 60 秒內被執行")
            assertEquals(599, callbackRef.get().statusCode)
            assertEquals("client error", callbackRef.get().statusText)
        } finally {
            resetStaticHttpClient()
        }
    }

}
