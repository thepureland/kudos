package io.kudos.ability.comm.sms.aws.init

import io.kudos.ability.comm.sms.aws.handler.AwsSmsHandler
import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * test for AwsSmsAutoConfiguration
 *
 * 覆蓋：awsSmsHandler / smsAwsProxyProperties Bean 工廠方法返回類型與多次調用獨立實例、組件名常量。
 * （Bean 的容器級裝配行為由 AwsSmsTest 的 @EnableKudosTest 集成測試覆蓋。）
 *
 * @author K
 * @since 1.0.0
 */
internal class AwsSmsAutoConfigurationTest {

    @Test
    fun awsSmsHandlerBean() {
        val configuration = AwsSmsAutoConfiguration()

        val handler = configuration.awsSmsHandler()
        assertIs<AwsSmsHandler>(handler)

        // 工廠方法本身無狀態，每次調用產生新實例（單例語義由 Spring 容器保證）
        assertNotSame(handler, configuration.awsSmsHandler())
    }

    @Test
    fun smsAwsProxyPropertiesBean() {
        val configuration = AwsSmsAutoConfiguration()

        val properties = configuration.smsAwsProxyProperties()
        assertIs<SmsAwsProxyProperties>(properties)
        // 默認值：代理關閉，其餘為 null（實際值由 @ConfigurationProperties 綁定）
        assertFalse(properties.enable)
        assertNull(properties.url)

        assertNotSame(properties, configuration.smsAwsProxyProperties())
    }

    @Test
    fun componentName() {
        assertEquals("kudos-ability-comm-sms-aws", AwsSmsAutoConfiguration().getComponentName())
    }

}
