package io.kudos.ability.comm.sms.aliyun

import io.kudos.base.io.FileKit
import io.kudos.base.io.IoKit
import io.kudos.base.lang.SystemKit
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.soul.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import org.soul.ability.comm.sms.aliyun.model.AliyunSmsRequest
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 阿里云发送短信测试用例
 *
 * 注意：
 * 本测试用例需要准备收发送邮件的相关账号
 * 配置文件路径：${user.home}\.soul-test\test-aliyun-sms.properties
 * 配置文件内容示例：
 * region=your_region
 * accessKeyId=your_accessKeyId
 * accessKeySecret=your_accessKeySecret
 * phoneNumbers=your_phone_number
 * signName=阿里云短信测试
 * templateCode=SMS_154950909
 * templateParam={"code":"6666"}
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
//TODO 因aliyun账号获取问题，该测试用例未能真正跑通
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class AliyunSmsTest {

    @Autowired
    private lateinit var smsHandler: AliyunSmsHandler

    private var smsRequest: AliyunSmsRequest? = null

    /**
     * 读取配置文件内容
     */
    private fun readProperties(): MutableMap<String?, String?>? {
        val path = SystemKit.getUserHome().toString() + TEST_SMS_FILE
        val file = FileKit.getFile(path)
        if (!file.exists()) {
            LOG.warn("测试的配置文件:{0}不存在", path)
            return null
        }
        val data: MutableMap<String?, String?> = HashMap<String?, String?>()
        val inputStream = FileKit.openInputStream(file)
        val body = IoKit.readLines(inputStream)
        if (body.isNotEmpty()) {
            for (line in body) {
                if (line.isNotBlank() && !line.startsWith("#") && !line.startsWith("//") && line.indexOf(
                        "="
                    ) != -1
                ) {
                    val lineArray: Array<String?> =
                        line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    data.put(lineArray[0], if (lineArray.size > 1) lineArray[1] else "")
                }
            }
        }
        inputStream.close()
        return data
    }

    @BeforeAll
    fun init() {
        val data = readProperties()
        if (!data.isNullOrEmpty()) {
            smsRequest = AliyunSmsRequest().apply {
                region = data["region"]
                accessKeyId = data["accessKeyId"]
                accessKeySecret = data["accessKeySecret"]
                phoneNumbers = data["phoneNumbers"]
                signName = data["signName"]
                templateCode = data["templateCode"]
                templateParam = data["templateParam"]
            }
        }
    }

    @Test
    fun send() {
        if (smsRequest == null) {
            LOG.warn("参数未初始化, 不参与本次测试")
            return
        }
        val code = arrayOfNulls<String>(1)
        val latch = CountDownLatch(1)
        smsHandler.send(smsRequest) { sendSmsResponseBody ->
            try {
                if (sendSmsResponseBody != null) {
                    code[0] = sendSmsResponseBody.getCode()
                }
            } finally {
                latch.countDown()
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        assertEquals("OK", code[0])
    }

    companion object {
        private val LOG = LogFactory.getLog(this)

        /**
         * 存放测试账号的文件
         */
        private const val TEST_SMS_FILE = "\\.kudos-test\\test-aliyun-sms.properties"
    }
}
