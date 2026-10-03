package io.kudos.ms.user.core.org.service.impl

import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reads ownership straight from the owning tables.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
open class OrganizationOwnershipService(
    private val tenantDao: SysTenantDao,
    private val accountDao: UserAccountDao,
    private val orgDao: UserOrgDao,
) : IOrganizationOwnershipService {

    override fun organizationIdForTenant(tenantId: String): String? =
        tenantDao.get(tenantId)?.organizationId?.takeIf(String::isNotBlank)

    override fun organizationIdForAccount(userId: String): String? =
        accountDao.get(userId)?.organizationId?.takeIf(String::isNotBlank)

    override fun findOrganization(organizationId: String): UserOrg? =
        orgDao.get(organizationId)?.takeIf { it.isOrganizationRoot() }

    override fun requireActiveOrganization(organizationId: String): UserOrg {
        val root = requireNotNull(findOrganization(organizationId)) { "ORGANIZATION_NOT_FOUND" }
        require(root.active) { "ORGANIZATION_DISABLED" }
        return root
    }
}

/** Whether this node is a customer organization root. */
fun UserOrg.isOrganizationRoot(): Boolean =
    nodeKind == UserOrgNodeKind.ORGANIZATION && organizationId == id && parentId.isNullOrBlank()

/** Structural node kinds of `user_org` in organization mode; legacy nodes have none. */
object UserOrgNodeKind {
    const val ORGANIZATION = "ORGANIZATION"
    const val DEPARTMENT = "DEPARTMENT"
}
