package io.kudos.ms.user.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.api.admin.controller.account.UserAccountProtectionAdminController
import io.kudos.ms.user.api.admin.controller.contact.UserContactWayAdminController
import io.kudos.ms.user.api.admin.controller.login.UserLoginRememberMeAdminController
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke test for the pure-delegation admin controllers that add no behaviour of their own
 * (every endpoint is inherited from [BaseCrudController]). No Spring container is started.
 *
 * Instantiating each subclass covers its (synthetic) constructor and confirms it is wired as a
 * [BaseCrudController] subtype (checked reflectively so the assertion is not a compile-time tautology).
 *
 * @author K
 * @since 1.0.0
 */
internal class PureDelegationControllersTest {

    @Test
    fun accountProtectionController_isBaseCrudControllerSubtype() {
        assertTrue(BaseCrudController::class.java.isInstance(UserAccountProtectionAdminController()))
    }

    @Test
    fun contactWayController_isBaseCrudControllerSubtype() {
        assertTrue(BaseCrudController::class.java.isInstance(UserContactWayAdminController()))
    }

    @Test
    fun rememberMeController_isBaseCrudControllerSubtype() {
        assertTrue(BaseCrudController::class.java.isInstance(UserLoginRememberMeAdminController()))
    }

}
