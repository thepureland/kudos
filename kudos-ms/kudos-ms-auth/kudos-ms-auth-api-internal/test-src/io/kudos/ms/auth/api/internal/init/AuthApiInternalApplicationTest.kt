package io.kudos.ms.auth.api.internal.init

import org.mockito.Mockito
import org.springframework.boot.SpringApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Pure unit test for [AuthApiInternalApplication] and its [main] entry point.
 *
 * The Spring container is never actually started: [SpringApplication.run] is stubbed via a static
 * mock, so the test stays fast, deterministic and free of any DB/Nacos/container dependency. It only
 * verifies that [main] delegates to [SpringApplication.run] with the correct primary source class and
 * forwards the given program arguments verbatim.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthApiInternalApplicationTest {

    @Test
    fun canBeInstantiated() {
        assertNotNull(AuthApiInternalApplication())
    }

    @Test
    fun main_delegatesToSpringApplicationRun_withSourceClassAndArgs() {
        val args = arrayOf("--server.port=0", "extra")
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(args)
            mocked.verify {
                SpringApplication.run(AuthApiInternalApplication::class.java, "--server.port=0", "extra")
            }
            mocked.verifyNoMoreInteractions()
        }
    }

    @Test
    fun main_forwardsEmptyArgs() {
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(emptyArray())
            mocked.verify {
                SpringApplication.run(AuthApiInternalApplication::class.java)
            }
            mocked.verifyNoMoreInteractions()
        }
    }
}
