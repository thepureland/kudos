package io.kudos.ms.user.api.public.init

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Pure unit test for [UserApiWebApplication].
 *
 * Only the class declaration is exercised (instantiation). The top-level `main` function is the
 * application entry point: it calls `SpringApplication.run`, which boots the full Spring/DB context,
 * so it is intentionally NOT invoked here and is left for the serial integration phase.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserApiWebApplicationTest {

    @Test
    fun canBeInstantiated() {
        assertNotNull(UserApiWebApplication())
    }
}
