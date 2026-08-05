package io.kudos.ms.auth.core.principal.spi

import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * The built-in `USER` directory, reading kudos' own account table.
 *
 * This is the one principal kind kudos ships, because it is the one whose data kudos owns. It is
 * also the reference implementation: an application adding service accounts writes the same two
 * methods against its own table.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class UserAccountPrincipalDirectory : IPrincipalDirectory {

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    override fun principalType(): String = PRINCIPAL_TYPE_USER

    override fun find(principalId: String): PrincipalFacts? {
        val user = userAccountHashCache.getUserById(principalId) ?: return null
        return PrincipalFacts(
            principalId = principalId,
            principalType = PRINCIPAL_TYPE_USER,
            tenantId = user.tenantId,
            active = user.active != false,
            displayName = user.username,
        )
    }

    companion object {
        const val PRINCIPAL_TYPE_USER = "USER"
    }
}
