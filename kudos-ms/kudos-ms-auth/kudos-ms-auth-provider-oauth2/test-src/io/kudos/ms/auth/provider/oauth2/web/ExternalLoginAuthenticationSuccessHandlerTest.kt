package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.service.BoundExternalIdentity
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityAuthenticationService
import io.kudos.ms.auth.provider.oauth2.service.ExternalIdentityBindingService
import io.kudos.ms.auth.provider.oauth2.service.ResolvedExternalPrincipal
import io.kudos.ms.auth.provider.oauth2.state.ExternalLoginState
import io.kudos.ms.auth.provider.oauth2.state.InMemoryExternalLoginStateStore
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ExternalLoginAuthenticationSuccessHandlerTest {

    @AfterTest
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun successPersistsOnlyMinimalLocalPrincipalAndConsumesState() {
        val now = Instant.now()
        val completed = AuthenticationTransaction(
            id = "tx-1",
            tenantId = "t-1",
            userId = "u-1",
            username = "alice",
            status = AuthenticationTransactionStatusEnum.COMPLETED,
            method = "external:provider-1",
            amr = setOf("federated", "google"),
            acr = "urn:kudos:acr:federated",
            context = AuthenticationContext(
                "u-1",
                "t-1",
                authTime = now,
                amr = setOf("federated", "google"),
                acr = "urn:kudos:acr:federated",
            ),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )
        val transactions = object : IAuthenticationTransactionService {
            override fun completeExternal(
                id: String,
                providerId: String,
                userId: String,
                tenantId: String,
                username: String,
                providerCode: String,
            ): AuthenticationTransaction = completed

            override fun bindSession(id: String, sessionId: String): AuthenticationTransaction =
                completed.copy(context = completed.context?.copy(sessionId = sessionId))

            override fun create(request: AuthenticationTransactionCreateRequest) = error("not used")
            override fun get(id: String) = completed
            override fun act(id: String, action: AuthenticationActionEnum, request: AuthenticationActionRequest) =
                error("not used")
            override fun cancel(id: String) = error("not used")
            override fun prepareExternal(
                id: String,
                providerId: String,
                tenantId: String,
                externalInvitationId: String?,
            ) = error("not used")
            override fun createExternalLink(userId: String, tenantId: String, providerId: String) = error("not used")
            override fun completeExternalLink(id: String, providerId: String, userId: String) = error("not used")
            override fun failExternal(id: String, providerId: String, errorCode: String) = completed
        }
        val identity = BoundExternalIdentity(
            "u-1",
            "t-1",
            "alice",
            "google",
            ExternalPrincipal("provider-1", ExternalProtocolEnum.OIDC, "https://issuer", "subject"),
        )
        val externalService = object : ExternalIdentityAuthenticationService(
            mock(AuthIdentityProviderDao::class.java),
            mock(AuthProviderTemplateDao::class.java),
            mock(IIdentityProviderClaimMappingService::class.java),
            mock(IExternalIdentityInvitationService::class.java),
            mock(IUserAccountThirdService::class.java),
            mock(IIdentityProviderJitConfigService::class.java),
            mock(IExternalAccountProvisioningService::class.java),
            mock(IUserAccountService::class.java),
            mock(IUserLogLoginService::class.java),
        ) {
            override fun authenticate(
                providerId: String,
                oauth2User: org.springframework.security.oauth2.core.user.OAuth2User,
                loginIp: Long?,
                userAgent: String?,
                invitationId: String?,
            ) = identity
        }
        val states = InMemoryExternalLoginStateStore()
        states.create(ExternalLoginState("state-1", "tx-1", "provider-1", now.plusSeconds(60)))
        val properties = ExternalLoginProperties().apply { successPath = "/login/result" }
        val contextRepository = HttpSessionSecurityContextRepository()
        val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
        val handler = ExternalLoginAuthenticationSuccessHandler(
            states,
            externalService,
            ExternalIdentityBindingService(mock(IUserAccountThirdService::class.java)),
            transactions,
            sessionService,
            properties,
            contextRepository,
        )
        val request = MockHttpServletRequest().apply {
            setParameter("state", "state-1")
            addHeader("User-Agent", "test-agent")
            remoteAddr = "::1"
        }
        val response = MockHttpServletResponse()
        val upstreamPrincipal = mock(OidcUser::class.java)
        val authentication = OAuth2AuthenticationToken(upstreamPrincipal, emptyList(), "provider-1")

        handler.onAuthenticationSuccess(request, response, authentication)

        val session = assertNotNull(request.session)
        val kudosPrincipal = assertIs<SessionUserPrincipal>(
            session.getAttribute(KudosContext.SESSION_KEY_USER)
        )
        assertEquals("u-1", kudosPrincipal.id)
        val logicalSessionId = assertNotNull(
            session.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
        )
        assertTrue(logicalSessionId != session.id)
        assertEquals("u-1", sessionService.get(logicalSessionId)?.userId)
        val securityContext = assertIs<SecurityContext>(
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        )
        val localAuthentication = assertNotNull(securityContext.authentication)
        assertIs<SessionUserPrincipal>(localAuthentication.principal)
        assertEquals(null, states.consume("state-1"))
        assertContains(assertNotNull(response.redirectedUrl), "authenticationTransactionId=tx-1")
    }

    @Test
    fun requiredFederatedSecondFactorRedirectsWithoutIssuingLocalSession() {
        val now = Instant.now()
        val pending = AuthenticationTransaction(
            id = "tx-mfa",
            tenantId = "t-1",
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
            method = "external:provider-1",
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )
        val challenged = pending.copy(
            userId = "u-1",
            username = "alice",
            nextActions = setOf(AuthenticationActionEnum.VERIFY_TOTP),
            amr = setOf("federated", "google"),
            acr = "urn:kudos:acr:federated",
            errorCode = "MFA_REQUIRED",
        )
        val transactions = mock(IAuthenticationTransactionService::class.java)
        `when`(transactions.get("tx-mfa")).thenReturn(pending)
        `when`(
            transactions.completeExternal("tx-mfa", "provider-1", "u-1", "t-1", "alice", "google")
        ).thenReturn(challenged)
        val identity = BoundExternalIdentity(
            "u-1", "t-1", "alice", "google",
            ExternalPrincipal("provider-1", ExternalProtocolEnum.OIDC, "https://issuer", "subject"),
        )
        val externalService = object : ExternalIdentityAuthenticationService(
            mock(AuthIdentityProviderDao::class.java),
            mock(AuthProviderTemplateDao::class.java),
            mock(IIdentityProviderClaimMappingService::class.java),
            mock(IExternalIdentityInvitationService::class.java),
            mock(IUserAccountThirdService::class.java),
            mock(IIdentityProviderJitConfigService::class.java),
            mock(IExternalAccountProvisioningService::class.java),
            mock(IUserAccountService::class.java),
            mock(IUserLogLoginService::class.java),
        ) {
            override fun authenticate(
                providerId: String,
                oauth2User: org.springframework.security.oauth2.core.user.OAuth2User,
                loginIp: Long?,
                userAgent: String?,
                invitationId: String?,
            ) = identity
        }
        val states = InMemoryExternalLoginStateStore()
        states.create(ExternalLoginState("state-mfa", "tx-mfa", "provider-1", now.plusSeconds(60)))
        val properties = ExternalLoginProperties().apply { successPath = "/login/result" }
        val handler = ExternalLoginAuthenticationSuccessHandler(
            states,
            externalService,
            ExternalIdentityBindingService(mock(IUserAccountThirdService::class.java)),
            transactions,
            mock(IAuthenticationSessionService::class.java),
            properties,
            HttpSessionSecurityContextRepository(),
        )
        val request = MockHttpServletRequest().apply {
            setParameter("state", "state-mfa")
        }
        val response = MockHttpServletResponse()
        val oauth = OAuth2AuthenticationToken(mock(OidcUser::class.java), emptyList(), "provider-1")

        handler.onAuthenticationSuccess(request, response, oauth)

        assertContains(assertNotNull(response.redirectedUrl), "authenticationTransactionId=tx-mfa")
        assertContains(assertNotNull(response.redirectedUrl), "authenticationChallenge=MFA_REQUIRED")
        assertEquals(null, request.getSession(false))
    }

    @Test
    fun linkCallbackRequiresPinnedLocalSessionAndRestoresLocalPrincipal() {
        val now = Instant.now()
        val pending = AuthenticationTransaction(
            id = "tx-link",
            tenantId = "t-1",
            purpose = AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY,
            initiatorUserId = "u-1",
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
            method = "external:provider-1",
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(300),
        )
        val completed = pending.copy(
            userId = "u-1",
            status = AuthenticationTransactionStatusEnum.COMPLETED,
            nextActions = emptySet(),
        )
        val transactions = mock(IAuthenticationTransactionService::class.java)
        `when`(transactions.get("tx-link")).thenReturn(pending)
        `when`(transactions.completeExternalLink("tx-link", "provider-1", "u-1")).thenReturn(completed)
        val resolved = ResolvedExternalPrincipal(
            tenantId = "t-1",
            providerId = "provider-1",
            providerCode = "google",
            principal = ExternalPrincipal(
                "provider-1", ExternalProtocolEnum.OIDC, "https://issuer", "subject-1"
            ),
        )
        val externalService = object : ExternalIdentityAuthenticationService(
            mock(AuthIdentityProviderDao::class.java),
            mock(AuthProviderTemplateDao::class.java),
            mock(IIdentityProviderClaimMappingService::class.java),
            mock(IExternalIdentityInvitationService::class.java),
            mock(IUserAccountThirdService::class.java),
            mock(IIdentityProviderJitConfigService::class.java),
            mock(IExternalAccountProvisioningService::class.java),
            mock(IUserAccountService::class.java),
            mock(IUserLogLoginService::class.java),
        ) {
            override fun resolve(
                providerId: String,
                oauth2User: org.springframework.security.oauth2.core.user.OAuth2User,
            ) = resolved
        }
        val binding = UserAccountThird {
            id = "binding-1"
            userId = "u-1"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            subject = "subject-1"
            active = true
        }
        val bindingService = object : ExternalIdentityBindingService(
            mock(IUserAccountThirdService::class.java)
        ) {
            override fun bind(userId: String, tenantId: String, resolved: ResolvedExternalPrincipal) = binding
        }
        val states = InMemoryExternalLoginStateStore()
        states.create(ExternalLoginState("state-link", "tx-link", "provider-1", now.plusSeconds(60)))
        val properties = ExternalLoginProperties().apply { linkSuccessPath = "/account/bindings" }
        val contextRepository = HttpSessionSecurityContextRepository()
        val handler = ExternalLoginAuthenticationSuccessHandler(
            states,
            externalService,
            bindingService,
            transactions,
            AuthenticationSessionService(InMemoryAuthenticationSessionStore()),
            properties,
            contextRepository,
        )
        val request = MockHttpServletRequest().apply {
            getSession(true)!!.setAttribute(
                KudosContext.SESSION_KEY_USER,
                SessionUserPrincipal("u-1", "t-1", "alice"),
            )
            setParameter("state", "state-link")
        }
        val response = MockHttpServletResponse()
        val oauth = OAuth2AuthenticationToken(mock(OidcUser::class.java), emptyList(), "provider-1")

        handler.onAuthenticationSuccess(request, response, oauth)

        assertContains(assertNotNull(response.redirectedUrl), "externalIdentityBindingId=binding-1")
        val session = assertNotNull(request.session)
        val securityContext = assertIs<SecurityContext>(
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        )
        assertIs<SessionUserPrincipal>(assertNotNull(securityContext.authentication).principal)
        assertEquals(null, states.consume("state-link"))
    }
}
