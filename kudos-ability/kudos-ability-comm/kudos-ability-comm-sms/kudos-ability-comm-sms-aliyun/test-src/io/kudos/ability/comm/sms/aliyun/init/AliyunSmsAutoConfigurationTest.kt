package io.kudos.ability.comm.sms.aliyun.init

import io.kudos.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

/**
 * test for AliyunSmsAutoConfiguration
 *
 * 覆蓋：aliyunSmsHandler Bean 工廠方法返回類型與多次調用獨立實例、組件名常量。
 * （Bean 的容器級裝配行為由 AliyunSmsTest 的 @EnableKudosTest 集成測試覆蓋。）
 *
 * @author K
 * @since 1.0.0
 */
internal class AliyunSmsAutoConfigurationTest {

    @Test
    fun aliyunSmsHandlerBean() {
        val configuration = AliyunSmsAutoConfiguration()

        val handler = configuration.aliyunSmsHandler()
        assertIs<AliyunSmsHandler>(handler)

        // 工廠方法本身無狀態，每次調用產生新實例（單例語義由 Spring 容器保證）
        assertNotSame(handler, configuration.aliyunSmsHandler())
    }

    @Test
    fun componentName() {
        assertEquals("kudos-ability-comm-sms-aliyun", AliyunSmsAutoConfiguration().getComponentName())
    }

}
