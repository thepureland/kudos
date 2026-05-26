package io.kudos.ability.comm.sms.aliyun

import io.kudos.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import io.kudos.ability.comm.sms.aliyun.model.AliyunSmsRequest
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.WireMockTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Aliyun SMS send test case; uses WireMock to mock the Aliyun SMS service.
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
class AliyunSmsTest {

    @Autowired
    private lateinit var smsHandler: AliyunSmsHandler

    @Test
    fun send_sms_ok() {
        stubSendSmsOK()

        val req = AliyunSmsRequest().apply {
            region = "cn-hangzhou"
            accessKeyId = "fake-ak"
            accessKeySecret = "fake-sk"
            phoneNumbers = "13800000000"
            signName = "SIGN"
            templateCode = "SMS_123456"
            templateParam = """{"code":"1234"}"""
        }

        val codeBox = arrayOfNulls<String>(1)
        val latch = CountDownLatch(1)

        smsHandler.send(req) { body ->
            try { codeBox[0] = body.code } finally { latch.countDown() }
        }

        latch.await(10, TimeUnit.SECONDS)
        assertEquals("OK", codeBox[0])
    }

    @Test
    fun send_sms_rate_limited() {
        stubSendSmsRateLimitedFor("13000000000")

        val req = AliyunSmsRequest().apply {
            region = "cn-hangzhou"
            accessKeyId = "fake-ak"
            accessKeySecret = "fake-sk"
            phoneNumbers = "13000000000" // hit the rate-limit stub
            signName = "SIGN"
            templateCode = "SMS_123456"
            templateParam = """{"code":"9999"}"""
        }

        val codeBox = arrayOfNulls<String>(1)
        val latch = CountDownLatch(1)

        smsHandler.send(req) { body ->
            try { codeBox[0] = body.code } finally { latch.countDown() }
        }

        latch.await(10, TimeUnit.SECONDS)
        assertEquals("isv.BUSINESS_LIMIT_CONTROL", codeBox[0])
    }

    // ---------------- WireMock Admin API: register stubs ----------------

    private fun stubSendSmsOK() {
        // GET/POST + QueryString form
        postJson("$baseUrl/__admin/mappings", """
          {
            "request": {
              "method": "ANY",
              "urlPath": "/",
              "queryParameters": { "Action": { "equalTo": "SendSms" } }
            },
            "response": {
              "status": 200,
              "headers": { "Content-Type": "application/json" },
              "jsonBody": { "Code": "OK", "Message": "OK", "RequestId": "test", "BizId": "biz" }
            },
            "priority": 10
          }
        """.trimIndent())

        // x-www-form-urlencoded (the SDK may use a form): match body contains
        postJson("$baseUrl/__admin/mappings", """
          {
            "request": {
              "method": "ANY",
              "urlPath": "/",
              "bodyPatterns": [ { "contains": "Action=SendSms" } ]
            },
            "response": {
              "status": 200,
              "headers": { "Content-Type": "application/json" },
              "jsonBody": { "Code": "OK", "Message": "OK", "RequestId": "test", "BizId": "biz" }
            },
            "priority": 9
          }
        """.trimIndent())
    }

    private fun stubSendSmsRateLimitedFor(phone: String) {
        // QueryString form: specific phone number hits rate limit
        postJson("$baseUrl/__admin/mappings", """
          {
            "request": {
              "method": "ANY",
              "urlPath": "/",
              "queryParameters": {
                "Action": { "equalTo": "SendSms" },
                "PhoneNumbers": { "equalTo": "$phone" }
              }
            },
            "response": {
              "status": 200,
              "headers": { "Content-Type": "application/json" },
              "jsonBody": {
                "Code": "isv.BUSINESS_LIMIT_CONTROL",
                "Message": "Rate limited for testing",
                "RequestId": "test-429"
              }
            },
            "priority": 8
          }
        """.trimIndent())

        // x-www-form-urlencoded form
        postJson("$baseUrl/__admin/mappings", """
          {
            "request": {
              "method": "ANY",
              "urlPath": "/",
              "bodyPatterns": [
                { "contains": "Action=SendSms" },
                { "contains": "PhoneNumbers=$phone" }
              ]
            },
            "response": {
              "status": 200,
              "headers": { "Content-Type": "application/json" },
              "jsonBody": {
                "Code": "isv.BUSINESS_LIMIT_CONTROL",
                "Message": "Rate limited for testing",
                "RequestId": "test-429-body"
              }
            },
            "priority": 7
          }
        """.trimIndent())
    }

    private fun postJson(url: String, body: String) {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText()
        if (code !in 200..299) {
            error("WireMock admin returned HTTP $code: $text\nurl=$url")
        }
    }


    companion object {
        private lateinit var baseUrl: String

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            val container = WireMockTestContainer.startIfNeeded(registry)
            baseUrl = "http://${container.ports.first().ip}:${container.ports.first().publicPort}"
            registry.add("kudos.ability.comm.sms.aliyun.endpoint") { baseUrl }
        }
    }

}
