package io.kudos.ms.user.core.security

import io.kudos.ability.security.common.init.SecurityCommonAutoConfiguration
import io.kudos.ms.user.common.org.vo.request.UserOrgFormUpdate
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.security.DefaultPasswordPolicy
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPolicyProperties
import io.kudos.ms.user.core.account.security.PasswordReusedException
import io.kudos.ms.user.core.account.service.impl.UserAccountService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.service.impl.UserOrgNodeKind
import io.kudos.ms.user.core.org.service.impl.UserOrgService
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Regressions for two defects found while writing the organization-mode tests (both fixed): each test
 * failed against the code before the fix.
 */
internal class OrganizationModeRegressionTest {

    @AfterTest
    fun clear() = signOut()

    /**
     * UserOrgService.update, legacy branch: only tenantId is checked, then the raw form goes to
     * dao.update, so organizationId / nodeKind supplied in a UserOrgFormUpdate are written onto a
     * legacy row without any organization gate. A legacy tenant administrator can graft a node into
     * any organization's directory (or make it a fake ORGANIZATION root).
     */
    @Test
    fun legacyNodeCannotBeConvertedIntoAnOrganizationNodeThroughUpdate() {
        val dao = mock(UserOrgDao::class.java)
        val guard = accessGuard()
        val service = UserOrgService(dao).also { svc ->
            setField(svc, "userOrgUserDao", mock(UserOrgUserDao::class.java))
            setField(svc, "userOrgHashCache", mock(UserOrgHashCache::class.java))
            setField(svc, "userAccountHashCache", mock(UserAccountHashCache::class.java))
            setField(svc, "userIdsByOrgIdCache", mock(UserIdsByOrgIdCache::class.java))
            setField(svc, "eventPublisher", ApplicationEventPublisher { })
            setField(svc, "tenantAccess", guard)
            setField(
                svc, "directoryGate",
                directoryGate(organizationMode(true), guard, mock(IOrganizationMemberPolicy::class.java)),
            )
        }
        whenCalled(dao.get("legacy")).thenReturn(legacyOrg("legacy", "tenant-a"))
        whenCalled(dao.update(ArgumentMatchers.any() ?: Any())).thenReturn(true)
        signIn("u1", "tenant-a")
        val form = UserOrgFormUpdate(
            id = "legacy", name = "x", shortName = null, tenantId = "tenant-a", parentId = null,
            orgTypeDictCode = "1", sortNum = null, remark = null,
            organizationId = "victim-org", nodeKind = UserOrgNodeKind.DEPARTMENT,
        )
        assertFailsWith<IllegalArgumentException> { service.update(form) }
        verify(dao, never()).update(ArgumentMatchers.any() ?: Any())
    }

    /**
     * UserAccountService.isCurrentPassword only consults the credential store when the context tenant
     * is non-blank. Organization accounts have tenantId "" so, with a credential store deployed, the
     * "same as current password" check falls back to the (emptied) column and never fires: an
     * organization account's password can be "reset" to the password already in force.
     */
    @Test
    fun organizationAccountPasswordResetToTheCurrentPasswordIsRejected() {
        val dao = mock(UserAccountDao::class.java)
        val store = mock(IAccountCredentialStore::class.java)
        whenCalled(
            store.verifyPassword(
                ArgumentMatchers.anyString() ?: "",
                ArgumentMatchers.any(PasswordPolicyContext::class.java)
                    ?: PasswordPolicyContext(io.kudos.ms.user.core.account.security.PasswordPurpose.LOGIN),
            )
        ).thenReturn(true)
        val guard = accessGuard()
        val service = UserAccountService(
            dao, ApplicationEventPublisher { }, DefaultPasswordPolicy(PasswordPolicyProperties()),
            listOf(mock(IPasswordHistory::class.java)), SecurityCommonAutoConfiguration().passwordEncoder(), listOf(store),
        ).also { svc ->
            setField(svc, "userOrgHashCache", mock(UserOrgHashCache::class.java))
            setField(svc, "userAccountHashCache", mock(UserAccountHashCache::class.java))
            setField(svc, "orgIdsByUserIdCache", mock(OrgIdsByUserIdCache::class.java))
            setField(svc, "userOrgDao", mock(UserOrgDao::class.java))
            setField(svc, "tenantAccess", guard)
            setField(svc, "directoryGate", directoryGate(organizationMode(true), guard, mock(IOrganizationMemberPolicy::class.java)))
        }
        whenCalled(dao.get("m1")).thenReturn(UserAccount {
            id = "m1"; organizationId = "org-1"; tenantId = ""; username = "m1"; loginPassword = ""; supervisorId = ""
        })
        whenCalled(dao.update(ArgumentMatchers.any() ?: Any())).thenReturn(true)
        assertFailsWith<PasswordReusedException> { service.resetPassword("m1", "correct horse battery staple") }
    }
}
