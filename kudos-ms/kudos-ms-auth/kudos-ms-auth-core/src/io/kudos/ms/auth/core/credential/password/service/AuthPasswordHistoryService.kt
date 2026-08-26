package io.kudos.ms.auth.core.credential.password.service

import io.kudos.ability.security.common.support.PasswordEncodingKit
import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.auth.core.credential.password.init.PasswordHistoryProperties
import io.kudos.ms.auth.core.credential.password.model.po.AuthPasswordHistory
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Auth-owned retired-password hashes exposed through the User migration SPI. */
@Service
@Transactional
open class AuthPasswordHistoryService(
    private val dao: AuthPasswordHistoryDao,
    private val properties: PasswordHistoryProperties,
    private val passwordEncoder: PasswordEncoder,
) : IPasswordHistory {

    @Transactional(readOnly = true)
    override fun isReused(password: String, context: PasswordPolicyContext): Boolean {
        val scope = scope(context) ?: return false
        val historySize = effectiveHistorySize()
        if (!properties.enabled || historySize == 0) return false
        return dao.findRecent(scope.tenantId, scope.userId, scope.purpose, historySize).any { history ->
            PasswordEncodingKit.matches(passwordEncoder, password, history.passwordHash)
        }
    }

    override fun record(encodedPassword: String, context: PasswordPolicyContext) {
        val scope = requireNotNull(scope(context)) { "Password history requires tenantId and userId" }
        val historySize = effectiveHistorySize()
        if (!properties.enabled || historySize == 0) return
        require(PasswordEncodingKit.looksLikeEncodedPassword(encodedPassword)) {
            "Password history accepts only a supported encoded password"
        }
        dao.insert(
            AuthPasswordHistory {
                id = UUID.randomUUID().toString()
                tenantId = scope.tenantId
                userId = scope.userId
                purpose = scope.purpose
                passwordHash = encodedPassword
                recordedAt = LocalDateTime.now(ZoneOffset.UTC)
            }
        )
        dao.trimToSize(scope.tenantId, scope.userId, scope.purpose, historySize)
    }

    private fun effectiveHistorySize(): Int = properties.historySize.coerceIn(0, MAX_HISTORY_SIZE)

    private fun scope(context: PasswordPolicyContext): Scope? {
        val tenantId = context.tenantId?.takeIf(String::isNotBlank) ?: return null
        val userId = context.userId?.takeIf(String::isNotBlank) ?: return null
        return Scope(tenantId, userId, context.purpose.name)
    }

    private data class Scope(val tenantId: String, val userId: String, val purpose: String)

    private companion object {
        const val MAX_HISTORY_SIZE = 24
    }
}
