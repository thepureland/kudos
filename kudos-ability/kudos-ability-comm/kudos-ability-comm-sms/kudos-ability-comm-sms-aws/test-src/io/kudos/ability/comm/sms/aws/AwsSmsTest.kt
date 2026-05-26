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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AWS SMS send test case; uses WireMock to mock the AWS SMS service.
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
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
            accessKeyId = "fake-ak"       // WireMock does not verify the signature
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
            phoneNumber = "+19990000"     // hit the rate-limit stub
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
        // Here we use 429 to simulate business rate limiting.
        assertEquals(429, statusCode)
        assertEquals("Too Many Requests", statusText)
    }

    // ---------------- WireMock register stubs ----------------

    /** Success stub: match Action=Publish and return 200 + OK. */
    private fun stubPublishOK() {
        // Match QueryString
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

        // Match x-www-form-urlencoded (common in SDKs): body contains Action=Publish
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

    /** Rate-limit stub: trigger 429 for the specified phone number. */
    private fun stubPublishRateLimitedFor(phone: String) {
        val encodedPhone = URLEncoder.encode(phone, StandardCharsets.UTF_8)

        // 429 + AWS Query error XML
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

        // Set priority to a low number (higher priority)
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

        /** Before the Spring context starts, inject the handler's endpoint -> WireMock. */
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            val container = WireMockTestContainer.startIfNeeded(registry)
            baseUrl = "http://${container.ports.first().ip}:${container.ports.first().publicPort}"
            registry.add("kudos.ability.comm.sms.aws.endpoint") { baseUrl }
        }
    }

}
