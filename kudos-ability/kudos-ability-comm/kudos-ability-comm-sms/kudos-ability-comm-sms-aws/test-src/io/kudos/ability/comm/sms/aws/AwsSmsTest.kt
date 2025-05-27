package io.kudos.ability.comm.sms.aws

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.ability.comm.sms.aws.handler.AwsSmsHandler
import org.soul.ability.comm.sms.aws.model.AwsSmsRequest
import org.soul.base.io.FileTool
import org.soul.base.io.IoTool
import org.soul.base.lang.SystemTool
import org.soul.base.lang.collections.CollectionTool
import org.soul.base.lang.collections.MapTool
import org.soul.base.lang.string.StringTool
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.http.HttpStatusFamily
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 亚马逊发送短信测试用例
 *
 * 注意：
 * 本测试用例需要准备收发送邮件的相关账号
 * 配置文件路径：${user.home}\.soul-test\test-aws-sms.properties
 * 配置文件内容示例：
 * region=your_region
 * accessKeyId=your_accessKeyId
 * accessKeySecret=your_accessKeySecret
 * phoneNumber=your_phone_number
 * message=亚马逊短信测试
 *
 * @author pual
 * @author K
 * @since 1.0.0
 */
//TODO 因aws账号获取问题，该测试用例未能真正跑通
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class AwsSmsTest {

    @Autowired
    private lateinit var smsHandler: AwsSmsHandler

    private var smsRequest: AwsSmsRequest? = null

    /**
     * 读取配置文件内容
     */
    private fun readProperties(): MutableMap<String?, String?>? {
        val path = SystemTool.getUserHome().toString() + TEST_SMS_FILE
        val file: File? = FileTool.getFile(path)
        if (file == null || !file.exists()) {
            LOG.warn("测试的配置文件:{0}不存在", path)
            return null
        }
        val data: MutableMap<String?, String?> = HashMap<String?, String?>()
        val inputStream = FileTool.openInputStream(file)
        val body = IoTool.readLines(inputStream)
        if (CollectionTool.isNotEmpty(body)) {
            for (line in body!!) {
                if (StringTool.isNotBlank(line) && !line!!.startsWith("#") && !line.startsWith("//") && line.indexOf(
                        "="
                    ) != -1
                ) {
                    val lineArray: Array<String?> =
                        line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    data.put(lineArray[0], if (lineArray.size > 1) lineArray[1] else "")
                }
            }
        }
        IoTool.closeQuietly(inputStream)
        return data
    }

    @BeforeAll
    fun init() {
        val data = readProperties()
        if (MapTool.isNotEmpty(data)) {
            smsRequest = AwsSmsRequest().apply {
                region = data!!["region"]
                accessKeyId = data["accessKeyId"]
                accessKeySecret = data["accessKeySecret"]
                phoneNumber = data["phoneNumber"]
                message = data["message"]
            }
        }
    }

    @Test
    fun send() {
        if (smsRequest == null) {
            LOG.warn("参数未初始化, 不参与本次测试")
            return
        }
        val code = arrayOfNulls<HttpStatusFamily>(1)
        val latch = CountDownLatch(1)
        smsHandler.send(smsRequest) { callBackParam ->
            try {
                if (callBackParam != null) {
                    code[0] = HttpStatusFamily.of(callBackParam.statusCode)
                }
            } finally {
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.SECONDS)
        Assertions.assertEquals(HttpStatusFamily.SUCCESSFUL, code[0])
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(AwsSmsTest::class.java)

        /**
         * 存放测试账号的文件
         */
        private const val TEST_SMS_FILE = "\\.soul-test\\test-aws-sms.properties"
    }

}
