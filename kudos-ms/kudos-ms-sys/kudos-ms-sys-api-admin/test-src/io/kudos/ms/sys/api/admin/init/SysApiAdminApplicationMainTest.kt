package io.kudos.ms.sys.api.admin.init

import org.mockito.Mockito
import org.springframework.boot.SpringApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Pure unit test for [SysApiAdminApplication] and its [main] entry point.
 *
 * Separate from the [Disabled][org.junit.jupiter.api.Disabled] manual smoke runner
 * `SysApiAdminApplicationTest`: here the Spring container is never started — [SpringApplication.run]
 * is stubbed via a static mock — so the test is fast, deterministic and needs no DB/Redis/container.
 * It only verifies [main] delegates to [SpringApplication.run] with the correct primary source class
 * and forwards the program arguments verbatim, and that the application class is instantiable.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysApiAdminApplicationMainTest {

    @Test
    fun classIsInstantiable() {
        assertNotNull(SysApiAdminApplication())
    }

    @Test
    fun main_delegatesToSpringApplicationRun_withSourceClassAndArgs() {
        val args = arrayOf("--server.port=0", "extra")
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(args)
            mocked.verify {
                SpringApplication.run(SysApiAdminApplication::class.java, "--server.port=0", "extra")
            }
            mocked.verifyNoMoreInteractions()
        }
    }

    @Test
    fun main_forwardsEmptyArgs() {
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(emptyArray())
            mocked.verify {
                SpringApplication.run(SysApiAdminApplication::class.java)
            }
            mocked.verifyNoMoreInteractions()
        }
    }
}
