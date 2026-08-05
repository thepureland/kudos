package io.kudos.ms.auth.core.role.datascope.spi

import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleScopeDao
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * The built-in `org` dimension, resolved against the organization tree in `kudos-ms-user`.
 *
 * This is the one dimension kudos ships, because it is the one whose data kudos owns. It is also
 * the reference implementation: an application adding `region` writes the same three methods
 * against its own tables.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrgScopeDimensionProvider : IScopeDimensionProvider {

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    override fun dimension(): String = AuthRoleScopeDao.DIMENSION_ORG

    /** Orgs nest, so a grant of an org carries its whole subtree. */
    override fun expand(values: Set<String>): Set<String> {
        if (values.isEmpty()) return emptySet()
        return values.flatMapTo(LinkedHashSet()) { orgId ->
            runCatching { userOrgDao.searchOrgAndDescendantIds(orgId) }.getOrDefault(setOf(orgId))
        }
    }

    override fun valueOfUser(userId: String): String? =
        userAccountHashCache.getUsersByIds(setOf(userId))[userId]?.orgId?.takeIf { it.isNotBlank() }
}
