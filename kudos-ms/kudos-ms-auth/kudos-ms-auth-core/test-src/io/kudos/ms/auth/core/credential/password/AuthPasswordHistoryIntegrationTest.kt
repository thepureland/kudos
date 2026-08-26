package io.kudos.ms.auth.core.credential.password

import io.kudos.ability.security.common.support.PasswordEncodingKit
import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.security.PasswordReusedException
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Verifies the Auth history bean is automatically composed into the User password-write boundary. */
@EnabledIfDockerInstalled
internal class AuthPasswordHistoryIntegrationTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userAccountService: IUserAccountService

    @Resource
    private lateinit var historyDao: AuthPasswordHistoryDao

    @Resource
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun passwordChangeArchivesOldHashAndRejectsReuse() {
        val suffix = UUID.randomUUID().toString()
        val tenantId = "t-${suffix.take(20)}"
        val userId = suffix
        val original = "original strong passphrase"
        val replacement = "replacement strong passphrase"
        userAccountService.insert(
            UserAccount {
                id = userId
                username = "u-${suffix.take(20)}"
                this.tenantId = tenantId
                loginPassword = original
                supervisorId = "00000000-0000-0000-0000-000000000000"
                active = true
                builtIn = false
            }
        )

        assertTrue(userAccountService.resetPassword(userId, replacement))

        val history = historyDao.findRecent(tenantId, userId, "LOGIN", 10)
        assertEquals(1, history.size)
        assertTrue(PasswordEncodingKit.matches(passwordEncoder, original, history.single().passwordHash))
        assertFailsWith<PasswordReusedException> {
            userAccountService.resetPassword(userId, original)
        }
    }
}
