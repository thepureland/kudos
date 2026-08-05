package io.kudos.ability.comm.email.init

import io.kudos.ability.comm.email.handler.EmailHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * test for EmailAutoConfiguration
 *
 * 覆蓋：emailHandler Bean 工廠方法（每次調用返回新實例，交由 Spring 管理單例）、組件名。
 *
 * @author K
 * @since 1.0.0
 */
internal class EmailAutoConfigurationTest {

    private val configuration = EmailAutoConfiguration()

    @Test
    fun emailHandler() {
        val handler = configuration.emailHandler()
        assertNotNull(handler)
        assertEquals(EmailHandler::class, handler::class)
        // 工廠方法本身無狀態，每次調用產生新實例（單例語義由 Spring 容器保證）
        assertNotSame(handler, configuration.emailHandler())
    }

    @Test
    fun getComponentName() {
        assertEquals("kudos-ability-comm-email", configuration.getComponentName())
    }

}
