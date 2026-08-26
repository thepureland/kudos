package io.kudos.ms.auth.core.provider.invitation.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Database-level proof that invitation consume/revoke are one-winner conditional writes. */
@EnabledIfDockerInstalled
class AuthExternalIdentityInvitationDaoTest : RdbTestBase() {

    @Resource
    private lateinit var invitationDao: AuthExternalIdentityInvitationDao

    @Test
    fun oneTimeInvitationCanBeConsumedOnlyOnce() {
        val now = LocalDateTime.now()

        val first = invitationDao.consume(INVITATION_ID, TENANT_ID, PROVIDER_ID, now, SUBJECT_HASH)
        val replay = invitationDao.consume(INVITATION_ID, TENANT_ID, PROVIDER_ID, now, SUBJECT_HASH)

        assertTrue(first)
        assertFalse(replay)
        val stored = assertNotNull(invitationDao.get(INVITATION_ID))
        assertEquals(1, stored.usedCount)
        assertEquals(false, stored.active)
        assertEquals(SUBJECT_HASH, stored.consumedSubjectHash)
        assertNotNull(stored.lastUsedTime)
    }

    @Test
    fun wrongTenantOrProviderCannotConsume() {
        val now = LocalDateTime.now()

        assertFalse(invitationDao.consume(INVITATION_ID, "another-tenant", PROVIDER_ID, now, SUBJECT_HASH))
        assertFalse(invitationDao.consume(INVITATION_ID, TENANT_ID, "another-provider", now, SUBJECT_HASH))

        val stored = assertNotNull(invitationDao.get(INVITATION_ID))
        assertEquals(0, stored.usedCount)
        assertEquals(true, stored.active)
    }

    @Test
    fun revokeAndConsumeCompeteOnTheSameActiveFlag() {
        val revoked = invitationDao.revoke(
            INVITATION_ID,
            TENANT_ID,
            "admin-2",
            "Invitation cancelled",
            LocalDateTime.now(),
        )
        val consumeAfterRevoke = invitationDao.consume(
            INVITATION_ID, TENANT_ID, PROVIDER_ID, LocalDateTime.now(), SUBJECT_HASH
        )

        assertTrue(revoked)
        assertFalse(consumeAfterRevoke)
        val stored = assertNotNull(invitationDao.get(INVITATION_ID))
        assertEquals("admin-2", stored.revokeUserId)
        assertEquals("Invitation cancelled", stored.revokeReason)
    }

    private companion object {
        const val TENANT_ID = "invite-dao-tenant-1"
        const val PROVIDER_ID = "e1000000-0000-0000-0000-000000000001"
        const val INVITATION_ID = "e2000000-0000-0000-0000-000000000001"
        const val SUBJECT_HASH = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }
}
