package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.dao.UserAccountThirdAuditDao
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditEvent
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditSnapshot
import io.kudos.ms.user.core.account.model.po.UserAccountThirdAudit
import io.kudos.ms.user.core.account.service.impl.UserAccountThirdAuditService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class UserAccountThirdAuditServiceTest {

    @Test
    fun recordHashesSubjectInsteadOfPersistingRawIdentifier() {
        val dao = mock(UserAccountThirdAuditDao::class.java)
        `when`(dao.insert(org.mockito.ArgumentMatchers.any(Any::class.java) ?: Any())).thenReturn("audit-1")
        val service = UserAccountThirdAuditService(dao)

        assertEquals(
            "audit-1",
            service.record(
                UserAccountThirdAuditEvent(
                    userId = "u-1",
                    tenantId = "t-1",
                    identityProviderId = "provider-1",
                    providerCode = "google",
                    subject = "raw-sensitive-subject",
                    action = "BIND",
                    success = true,
                    actorUserId = "u-1",
                    operationReason = "onboarding ticket",
                    beforeSnapshot = UserAccountThirdAuditSnapshot(
                        identityProviderId = "provider-1",
                        providerCode = "google",
                        issuer = "https://accounts.google.com",
                        subject = "raw-sensitive-subject",
                        active = false,
                    ),
                )
            )
        )

        val captor = ArgumentCaptor.forClass(UserAccountThirdAudit::class.java)
        verify(dao).insert(captor.capture() ?: UserAccountThirdAudit {})
        assertEquals(64, captor.value.subjectHash.length)
        assertFalse(captor.value.subjectHash.contains("raw-sensitive-subject"))
        assertEquals("onboarding ticket", captor.value.operationReason)
        assertFalse(captor.value.beforeSnapshot!!.contains("raw-sensitive-subject"))
        assertEquals(64, captor.value.beforeSnapshot!!.substringAfter("subjectHash=").substringBefore(';').length)
    }
}
