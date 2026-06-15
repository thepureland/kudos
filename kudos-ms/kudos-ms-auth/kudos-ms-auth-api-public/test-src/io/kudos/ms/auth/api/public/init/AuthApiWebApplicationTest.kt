package io.kudos.ms.auth.api.public.init

import org.springframework.boot.SpringApplication
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.mockito.Mockito

/**
 * Pure unit test for [AuthApiWebApplication] and its [main] entry point.
 *
 * The Spring container is never actually started: [SpringApplication.run] is stubbed via a static
 * mock, so the test stays fast, deterministic and free of any DB/container dependency. It only
 * verifies that [main] delegates to [SpringApplication.run] with the correct primary source class
 * and forwards the given program arguments verbatim.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthApiWebApplicationTest {

    @Test
    fun classIsInstantiable() {
        assertNotNull(AuthApiWebApplication())
    }

    @Test
    fun main_delegatesToSpringApplicationRun_withSourceClassAndArgs() {
        val args = arrayOf("--server.port=0", "extra")

        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(args)

            // exact source class + each forwarded program arg are verified verbatim
            mocked.verify {
                SpringApplication.run(AuthApiWebApplication::class.java, "--server.port=0", "extra")
            }
            mocked.verifyNoMoreInteractions()
        }
    }

    @Test
    fun main_forwardsEmptyArgs() {
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(emptyArray())

            mocked.verify {
                SpringApplication.run(AuthApiWebApplication::class.java)
            }
            mocked.verifyNoMoreInteractions()
        }
    }

    @Test
    fun main_forwardsUnicodeArgVerbatim() {
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(arrayOf("--app.name=认证-公共"))

            mocked.verify {
                SpringApplication.run(AuthApiWebApplication::class.java, "--app.name=认证-公共")
            }
            mocked.verifyNoMoreInteractions()
        }
    }
}
