package io.kudos.ability.comm.sms.aliyun.handler

import io.kudos.ability.comm.sms.aliyun.model.AliyunSmsRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * test for AliyunSmsHandler（純單元測試，不依賴 Spring 容器與真實網絡服務）
 *
 * 覆蓋：
 * - buildClient 的全部 endpoint 解析分支：空/空白（不覆蓋 endpoint）、完整 http URI（帶端口）、
 *   https URI（無端口，port=-1）、無 scheme 的 host:port（opaque URI 回退原始串）、
 *   裸域名（相對 URI 回退原始串）、非法 URI（解析異常回退 trim 後原始串）
 * - send/doSend 的異常路徑：endpoint 指向不可達端口時回調收到 code="EXCEPTION" 的安全響應體
 *   （含 client 已創建需 close、以及 buildClient 即拋異常 client 為 null 兩種情形）
 *
 * 成功發送路徑（WireMock 模擬阿里雲服務）由 AliyunSmsTest 集成測試覆蓋。
 *
 * endpointOverrideStr 為 @Value 注入的私有字段，測試中通過反射注入以模擬各種配置值。
 *
 * @author K
 * @since 1.0.0
 */
internal class AliyunSmsHandlerTest {

    private fun handlerWithEndpoint(endpoint: String): AliyunSmsHandler {
        val handler = AliyunSmsHandler()
        val field = AliyunSmsHandler::class.java.getDeclaredField("endpointOverrideStr")
        field.isAccessible = true
        field.set(handler, endpoint)
        return handler
    }

    private fun assertBuildClientOk(endpoint: String) {
        val client = handlerWithEndpoint(endpoint).buildClient("cn-hangzhou", "ak", "sk")
        try {
            assertNotNull(client)
        } finally {
            client.close()
        }
    }

    // ---------------- buildClient: endpoint 解析分支 ----------------

    @Test
    fun buildClientWithEmptyEndpointUsesSdkDefault() {
        // 空串：不走 override 分支，使用 SDK 按 region 的默認域名
        assertBuildClientOk("")
    }

    @Test
    fun buildClientWithBlankEndpointUsesSdkDefault() {
        // 純空白：isNotBlank() 為 false，同樣不走 override 分支
        assertBuildClientOk("   ")
    }

    @Test
    fun buildClientWithFullHttpUriWithPort() {
        // 完整 URI 帶端口：scheme=http、port=8080 → protocol=HTTP、hostPort="localhost:8080"
        assertBuildClientOk("http://localhost:8080")
    }

    @Test
    fun buildClientWithHttpsUriWithoutPort() {
        // 完整 URI 無端口：port=-1 分支 → hostPort 僅為 host
        assertBuildClientOk("https://dysmsapi.aliyuncs.com")
    }

    @Test
    fun buildClientWithHostPortWithoutScheme() {
        // "host:port" 會被解析為 opaque URI（scheme="localhost"、host=null）→ 回退原始串，協議默認 HTTPS
        assertBuildClientOk("localhost:8080")
    }

    @Test
    fun buildClientWithBareDomain() {
        // 裸域名解析為相對 URI（host=null）→ 回退原始串
        assertBuildClientOk("dysmsapi.aliyuncs.com")
    }

    @Test
    fun buildClientWithUnparseableUriFallsBackToTrimmedRaw() {
        // 含空格的非法 URI：URI() 拋 URISyntaxException → uri=null → 回退 trim 後原始串
        assertBuildClientOk("  http://bad uri  ")
    }

    @Test
    fun buildClientWithNullRegionAndCredentials() {
        // region/AK 為 null 時構建 client 本身不應失敗（校驗發生在實際調用時）
        val client = handlerWithEndpoint("").buildClient(null, null, null)
        try {
            assertNotNull(client)
        } finally {
            client.close()
        }
    }

    // ---------------- send/doSend: 異常路徑 ----------------

    @Test
    fun sendToUnreachableEndpointInvokesCallbackWithExceptionBody() {
        // endpoint 指向本機保留端口（無監聽），請求必然失敗 → 回調收到 code="EXCEPTION" 的安全響應體
        val handler = handlerWithEndpoint("http://127.0.0.1:1")
        val request = AliyunSmsRequest().apply {
            region = "cn-hangzhou"
            accessKeyId = "fake-ak"
            accessKeySecret = "fake-sk"
            phoneNumbers = "13800000000"
            signName = "SIGN"
            templateCode = "SMS_123456"
            templateParam = """{"code":"1234"}"""
        }

        val bodyRef = AtomicReference<com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponseBody>()
        val latch = CountDownLatch(1)

        handler.send(request) { body ->
            bodyRef.set(body)
            latch.countDown()
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "callback was not invoked in time")
        val body = bodyRef.get()
        assertNotNull(body)
        assertEquals("EXCEPTION", body.code)
        assertNotNull(body.message)
        assertEquals("local-test", body.requestId)
    }

    @Test
    fun sendWithUninitializedEndpointFieldStillInvokesCallback() {
        // 未注入 endpointOverrideStr（lateinit 未初始化）：buildClient 即拋異常、client 為 null，
        // finally 塊仍須回調安全響應體
        val handler = AliyunSmsHandler()
        val request = AliyunSmsRequest().apply {
            region = "cn-hangzhou"
            accessKeyId = "fake-ak"
            accessKeySecret = "fake-sk"
            phoneNumbers = "13800000000"
        }

        val codeRef = AtomicReference<String>()
        val latch = CountDownLatch(1)

        handler.send(request) { body ->
            codeRef.set(body.code)
            latch.countDown()
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "callback was not invoked in time")
        assertEquals("EXCEPTION", codeRef.get())
    }

}
