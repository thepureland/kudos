package io.kudos.ms.user.core.login.service

import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import io.kudos.ms.user.core.login.service.iservice.IUserLoginRememberMeService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for UserLoginRememberMeService — the service adds no behaviour over [BaseCrudService],
 * so this verifies the inherited CRUD pipeline (insert / get / update / delete) is correctly wired
 * for the [UserLoginRememberMe] entity through the [IUserLoginRememberMeService] interface.
 *
 * Test data source: `UserLoginRememberMeServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserLoginRememberMeServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userLoginRememberMeService: IUserLoginRememberMeService

    @Test
    fun get_seededRecord() {
        val po = assertNotNull(userLoginRememberMeService.get("33333333-0000-0000-0000-000000000001"))
        assertEquals("remember-user-1", po.username)
        assertEquals("token-1", po.token)
    }

    @Test
    fun crudRoundTrip() {
        val id = UUID.randomUUID().toString()
        val po = UserLoginRememberMe {
            this.id = id
            // user_id has a FK to user_account(id); reuse the seeded account.
            this.userId = "33330000-0000-0000-0000-000000000001"
            this.tenantId = "svc-rt-tenant"
            this.username = "svc-rt-username"
            this.token = "svc-rt-token"
            this.lastUsed = LocalDateTime.now().withNano(0)
        }
        userLoginRememberMeService.insert(po)

        val loaded = assertNotNull(userLoginRememberMeService.get(id))
        assertEquals("svc-rt-token", loaded.token)
        assertEquals("svc-rt-username", loaded.username)

        loaded.token = "svc-rt-token-updated"
        assertTrue(userLoginRememberMeService.update(loaded))
        assertEquals("svc-rt-token-updated", userLoginRememberMeService.get(id)?.token)

        assertTrue(userLoginRememberMeService.deleteById(id))
        assertEquals(null, userLoginRememberMeService.get(id))
    }
}
