package io.kudos.ms.msg.api.public.init

import org.mockito.Mockito
import org.springframework.boot.SpringApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Pure unit test for [MsgApiWebApplication] and its [main] entry point.
 *
 * The Spring container is never actually started: [SpringApplication.run] is stubbed via a
 * static mock, so the test stays fast, deterministic and free of any DB/container dependency.
 * It only verifies that [main] delegates to [SpringApplication.run] with the correct primary
 * source class and forwards the given program arguments verbatim.
 *
 * Note: the stubbed static [SpringApplication.run] returns `null` (Mockito default) and [main]
 * ignores the return value, so no explicit `thenReturn` stub is needed — this also keeps the
 * test free of Kotlin-vararg / argument-matcher pitfalls.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgApiWebApplicationTest {

    @Test
    fun classIsInstantiable() {
        assertNotNull(MsgApiWebApplication())
    }

    @Test
    fun main_delegatesToSpringApplicationRun_withSourceClassAndArgs() {
        val args = arrayOf("--server.port=0", "extra")

        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(args)

            // exact source class + each forwarded program arg are verified verbatim
            mocked.verify {
                SpringApplication.run(MsgApiWebApplication::class.java, "--server.port=0", "extra")
            }
            mocked.verifyNoMoreInteractions()
        }
    }

    @Test
    fun main_forwardsEmptyArgs() {
        Mockito.mockStatic(SpringApplication::class.java).use { mocked ->
            main(emptyArray())

            mocked.verify {
                SpringApplication.run(MsgApiWebApplication::class.java)
            }
            mocked.verifyNoMoreInteractions()
        }
    }
}
