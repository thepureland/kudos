package io.kudos.ability.comm.sms.aws

import io.kudos.ability.comm.sms.aws.handler.AwsSmsHandler
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.WireMockTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * aws短信发送测试用例，用WireMock模拟aws短信服务
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.-0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
class AwsSmsTest {

    @Resource
    private lateinit var handler: AwsSmsHandler

    @Test
    fun send_sms_ok() {
        stubPublishOK()

        val req = AwsSmsRequest().apply {
            region = "us-east-1"
            accessKeyId = "fake-ak"       // WireMock 不校验签名
            accessKeySecret = "fake-sk"
            phoneNumber = "+15550100"
            message = "hello wiremock"
            messageAttributes = null
        }

        val latch = CountDownLatch(1)
        var statusCode = -1
        var statusText: String? = null

        handler.send(req) { cb ->
            statusCode = cb.statusCode
            statusText = cb.statusText
            latch.countDown()
        }

        latch.await(10, TimeUnit.SECONDS)
        assertEquals(200, statusCode)
        assertEquals("OK", statusText)
    }

    @Test
    fun send_sms_rate_limited() {
        stubPublishRateLimitedFor("+19990000")

        val req = AwsSmsRequest().apply {
            region = "us-east-1"
            accessKeyId = "fake-ak"
            accessKeySecret = "fake-sk"
            phoneNumber = "+19990000"     // 命中限流桩
            message = "trigger 429"
        }

        val latch = CountDownLatch(1)
        var statusCode = -1
        var statusText: String? = null

        handler.send(req) { cb ->
            statusCode = cb.statusCode
            statusText = cb.statusText
            latch.countDown()
        }

        latch.await(10, TimeUnit.SECONDS)
        // 这里我们用 429 模拟业务限流
        assertEquals(429, statusCode)
        assertEquals("Too Many Requests", statusText)
    }

    // ---------------- WireMock 注册桩 ----------------

    /** 成功桩：匹配 Action=Publish，返回 200 + OK */
    private fun stubPublishOK() {
        // 匹配 QueryString
        postJson("$baseUrl/__admin/mappings", """
            {
              "request": {
                "method": "ANY",
                "urlPath": "/",
                "queryParameters": { "Action": { "equalTo": "Publish" } }
              },
              "response": {
                "status": 200,
                "headers": { "Content-Type": "text/xml" },
                "body": "<PublishResponse><MessageId>mid-123</MessageId></PublishResponse>",
                "transformers": []
              },
              "priority": 10
            }
        """.trimIndent())

        // 匹配 x-www-form-urlencoded（SDK 常用）：body 包含 Action=Publish
        postJson("$baseUrl/__admin/mappings", """
            {
              "request": {
                "method": "POST",
                "urlPath": "/",
                "bodyPatterns": [ { "contains": "Action=Publish" } ]
              },
              "response": {
                "status": 200,
                "headers": { "Content-Type": "text/xml" },
                "body": "<PublishResponse><MessageId>mid-123</MessageId></PublishResponse>"
              },
              "priority": 9
            }
        """.trimIndent())
    }

    /** 限流桩：指定手机号触发 429 */
    private fun stubPublishRateLimitedFor(phone: String) {
        val encodedPhone = java.net.URLEncoder.encode(phone, java.nio.charset.StandardCharsets.UTF_8)

        // 429 + AWS Query 错误 XML
        val errorXml = """
      <?xml version="1.0"?>
      <ErrorResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
        <Error>
          <Type>Sender</Type>
          <Code>Throttled</Code>
          <Message>Too Many Requests</Message>
        </Error>
        <RequestId>test-req-429</RequestId>
      </ErrorResponse>
    """.trimIndent()

        // 优先级设低数字（高优先）
        postJson("$baseUrl/__admin/mappings", """
      {
        "request": {
          "method": "POST",
          "urlPath": "/",
          "bodyPatterns": [
            { "contains": "Action=Publish" },
            { "matches": ".*(?:^|&)PhoneNumber=$encodedPhone(?:&|$).*" }
          ]
        },
        "response": {
          "status": 429,
          "headers": {
            "Content-Type": "text/xml",
            "x-amzn-RequestId": "test-req-429"
          },
          "body": ${errorXml.trimIndent().replace("\n","\\n").replace("\"","\\\"").let { "\"$it\"" }}
        },
        "priority": 1
      }
    """.trimIndent())
    }

    private fun postJson(url: String, body: String) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText()
        if (code !in 200..299) error("WireMock admin returned $code: $text, url=$url")
    }

    companion object {
        private lateinit var baseUrl: String

        /** 在 Spring 上下文启动前，注入 Handler 的 endpoint → WireMock */
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            val container = WireMockTestContainer.startIfNeeded(registry)
            baseUrl = "http://${container.ports.first().ip}:${container.ports.first().publicPort}"
            registry.add("kudos.ability.comm.sms.aws.endpoint") { baseUrl }
        }
    }

}
