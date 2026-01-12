package io.kudos.ability.comm.email

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import io.kudos.ability.comm.email.handler.EmailHandler
import io.kudos.ability.comm.email.model.EmailRequest
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.SmtpTestContainer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 测试发送邮件
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
class EmailTest {

    @Autowired
    private lateinit var emailHandler: EmailHandler

    private lateinit var emailRequest: EmailRequest

    @BeforeAll
    fun setup() {
        emailRequest = EmailRequest().apply {
            serverHost = HOST
            serverPort = PORT
            subject = "This is a test email."
            body = "Test Email"
            senderAccount = "sender-email@example.com"
            senderPassword = "secret"
            fromMailAddress = "from-email@example.com"
            receivers = mutableSetOf("recipient-email@example.com")
            ssl = false
        }
    }

    @Test
    fun sendTxt() {
        val emailStatusEnum = mutableListOf<EmailStatusEnum>()
        val latch = CountDownLatch(1)
        emailRequest.htmlFormat = false
        emailHandler.send(emailRequest) { emailCallBackParam ->
            try {
                emailStatusEnum.add(emailCallBackParam.status!!)
            } finally {
                latch.countDown()
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        assertEquals(EmailStatusEnum.SUCCESS, emailStatusEnum.first())
    }

    @Test
    fun sendHtml() {
        val emailStatusEnum = mutableListOf<EmailStatusEnum>()
        val latch = CountDownLatch(1)
        emailRequest.htmlFormat = true
        emailRequest.body = "<h1>${emailRequest.body}</h1>"
        emailHandler.send(emailRequest) { emailCallBackParam ->
            try {
                emailStatusEnum.add(emailCallBackParam.status!!)
            } finally {
                latch.countDown()
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        assertEquals(EmailStatusEnum.SUCCESS, emailStatusEnum.first())
    }

    companion object {
        private var HOST: String? = null
        private var PORT: Int? = null

        @JvmStatic
        @DynamicPropertySource
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            val container = SmtpTestContainer.startIfNeeded(registry)
            HOST = container.ports.first().ip
            PORT = container.ports.first().publicPort
        }
    }

}
