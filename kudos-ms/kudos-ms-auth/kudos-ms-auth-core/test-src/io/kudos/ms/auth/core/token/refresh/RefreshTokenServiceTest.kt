package io.kudos.ms.auth.core.token.refresh

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.dao.AuthRefreshTokenDao
import io.kudos.ms.auth.core.token.refresh.model.RefreshTokenException
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class RefreshTokenServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var refreshTokenService: IRefreshTokenService

    @Resource
    private lateinit var sessionService: IAuthenticationSessionService

    @Resource
    private lateinit var dao: AuthRefreshTokenDao

    @Resource
    private lateinit var permissionVersionApi: IPermissionVersionApi

    private fun sourceSessionId(): String = sessionService.issue(
        AuthenticationSessionIssueCommand(
            context = AuthenticationContext(
                userId = "u-token-1",
                tenantId = "t-token-1",
                authTime = Instant.now(),
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
            ),
            username = "alice",
        )
    ).id

    @Test
    fun issue_persistsOnlyHashAndCreatesIndependentApiSession() {
        val sourceId = sourceSessionId()

        val issued = refreshTokenService.issue(sourceId, "mobile-app", "phone-1")
        val stored = assertNotNull(dao.findByTokenHash(sha256(issued.token)))

        assertTrue(issued.token.startsWith("krt_"))
        assertEquals(47, issued.token.length)
        assertNotEquals(issued.token, stored.tokenHash)
        assertEquals(64, stored.tokenHash.length)
        assertEquals(0, stored.tokenEpoch)
        assertNotEquals(sourceId, issued.session.id)
        assertEquals("mobile-app", issued.session.clientId)
        assertEquals("phone-1", issued.session.deviceId)
        assertNull(stored.parentId)
        assertNull(stored.consumedAt)
    }

    @Test
    fun rotate_consumesParentAndPreservesFamilyAbsoluteExpiry() {
        val first = refreshTokenService.issue(sourceSessionId())

        val second = refreshTokenService.rotate(first.token)
        val parent = assertNotNull(dao.findByTokenHash(sha256(first.token)))
        val child = assertNotNull(dao.findByTokenHash(sha256(second.token)))

        assertNotEquals(first.token, second.token)
        assertNotNull(parent.consumedAt)
        assertEquals(child.id, parent.replacedById)
        assertEquals(parent.id, child.parentId)
        assertEquals(parent.familyId, child.familyId)
        assertEquals(parent.expiresAt, child.expiresAt)
        assertTrue(second.session.isActive())
    }

    @Test
    fun replay_revokesWholeFamilyAndLogicalApiSession() {
        val first = refreshTokenService.issue(sourceSessionId())
        val second = refreshTokenService.rotate(first.token)

        val error = assertFailsWith<RefreshTokenException> {
            refreshTokenService.rotate(first.token)
        }

        assertTrue(error.reuseDetected)
        assertNotNull(dao.findByTokenHash(sha256(first.token))?.revokedAt)
        assertNotNull(dao.findByTokenHash(sha256(second.token))?.revokedAt)
        assertFalse(assertNotNull(sessionService.get(second.session.id)).isActive())
        assertEquals("REFRESH_TOKEN_REUSE", sessionService.get(second.session.id)?.revokeReason)
    }

    @Test
    fun administrativeTokenEpochBump_preventsRefreshFromMintingFreshAccess() {
        val issued = refreshTokenService.issue(sourceSessionId())
        permissionVersionApi.revokeAllTokens("u-token-1", "credential leaked")

        val error = assertFailsWith<RefreshTokenException> {
            refreshTokenService.rotate(issued.token)
        }

        assertFalse(error.reuseDetected)
        assertNotNull(dao.findByTokenHash(sha256(issued.token))?.revokedAt)
        assertFalse(assertNotNull(sessionService.get(issued.session.id)).isActive())
        assertEquals("TOKEN_EPOCH_STALE", sessionService.get(issued.session.id)?.revokeReason)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
