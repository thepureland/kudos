package io.kudos.ms.user.core.security

import io.kudos.base.bean.BeanKit
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.eq
import io.kudos.base.support.dao.IBaseCrudDao
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.org.service.impl.OrganizationDirectoryGate

/**
 * Authorizes the account owning each record at the CRUD service boundary used by admin controllers.
 * Applies to contacts, security questions, external bindings and remembered logins, including tables
 * without their own tenant column. Filtering happens before pagination and counting.
 */
open class UserOwnedCrudService<PK : Any, E : IIdEntity<PK>, DAO : IBaseCrudDao<PK, E>>(dao: DAO) :
    BaseCrudService<PK, E, DAO>(dao) {

    @Autowired
    protected var tenantAccess = UserTenantAccessGuard()

    @Autowired
    protected lateinit var ownerAccountDao: UserAccountDao

    protected fun assertAccountAccess(userId: String): UserAccount {
        val account = requireNotNull(ownerAccountDao.get(userId)) { "Account not found" }
        tenantAccess.assertCanAccessOwner(account.tenantId, account.organizationId)
        return account
    }

    private fun property(value: Any, name: String): String? =
        runCatching { BeanKit.getProperty(value, name) as? String }.getOrNull()

    private fun assertOwned(value: Any) {
        if (!tenantAccess.hasPrincipal()) return
        val owner = assertAccountAccess(requireNotNull(property(value, "userId")) { "Account owner is required" })
        property(value, "tenantId")?.let { assertRecordTenant(it, owner) }
    }

    @Autowired(required = false)
    private var directoryGate: OrganizationDirectoryGate? = null

    /**
     * Writing someone else's account records (contacts, recovery data, remembered logins) of an
     * organization account is member management: it goes through the organization member policy, which
     * also refuses targets that outrank the caller. One's own records stay self-service.
     */
    private fun assertCanWrite(value: Any) {
        assertOwned(value)
        if (!tenantAccess.hasPrincipal()) return
        val owner = ownerAccountDao.get(property(value, "userId") ?: return) ?: return
        val organizationId = owner.organizationId?.takeIf(String::isNotBlank) ?: return
        if (CurrentUserKit.currentUserIdOrNull() == owner.id) return
        requireNotNull(directoryGate) { "ORGANIZATION_MODE_UNAVAILABLE" }.assertCanManageMember(organizationId, owner.id)
    }

    @Autowired(required = false)
    private var ownership: IOrganizationOwnershipService? = null

    /**
     * A legacy account's records live in its own tenant. An organization account serves all tenants of
     * its organization, so a record (an external identity binding, say) may name any of them, or none.
     */
    private fun assertRecordTenant(
        tenantId: String,
        owner: UserAccount,
        legacyMessage: String = "Record and account must belong to the same tenant",
    ) {
        val organizationId = owner.organizationId
        if (organizationId.isNullOrBlank()) {
            require(tenantId == owner.tenantId) { legacyMessage }
        } else {
            require(tenantId.isBlank() || ownership?.organizationIdForTenant(tenantId) == organizationId) {
                "Record and account must belong to the same organization"
            }
        }
    }

    override fun get(id: PK): E? = dao.get(id)?.also(::assertOwned)

    override fun <R : Any> get(id: PK, returnType: KClass<R>): R? {
        get(id) ?: return null
        return dao.get(id, returnType)
    }

    override fun insert(any: Any): PK {
        assertCanWrite(any)
        return super.insert(any)
    }

    override fun update(any: Any): Boolean {
        @Suppress("UNCHECKED_CAST")
        val id = BeanKit.getProperty(any, "id") as PK
        val stored = get(id) ?: return false
        assertCanWrite(stored)
        property(any, "userId")?.let {
            require(it == property(stored, "userId")) { "Record ownership cannot be changed" }
        }
        property(any, "tenantId")?.let {
            assertRecordTenant(it, assertAccountAccess(requireNotNull(property(stored, "userId"))), "Record tenant cannot be changed")
        }
        return super.update(any)
    }

    override fun deleteById(id: PK): Boolean {
        assertCanWrite(get(id) ?: return false)
        return super.deleteById(id)
    }

    override fun batchDelete(ids: Collection<PK>): Int {
        dao.getByIds(ids).forEach(::assertCanWrite)
        return super.batchDelete(ids)
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val ownerCriteria = when (val scope = tenantAccess.restrictedScope()) {
            UserAccessScope.Unrestricted -> return super.pagingSearch(listSearchPayload)
            is UserAccessScope.Tenant -> Criteria(UserAccount::tenantId eq scope.tenantId)
            is UserAccessScope.Organization -> Criteria(UserAccount::organizationId eq scope.organizationId)
        }
        // All user-admin query DTOs use conjunctions. Refuse a new unsupported query shape rather
        // than accidentally OR-ing the mandatory owner restriction with user-selected conditions.
        require(listSearchPayload.getAndOr() == AndOrEnum.AND) { "Tenant queries must use conjunctions" }
        val requestedOwner = property(listSearchPayload, "userId")?.takeIf(String::isNotBlank)
        requestedOwner?.let(::assertAccountAccess)
        val owners = requestedOwner?.let(::listOf) ?: ownerAccountDao.searchProperty(
            ownerCriteria, UserAccount::id,
        ).filterNotNull()
        if (owners.isEmpty()) return PagingSearchResult(emptyList<Any>(), 0)
        val operators = listSearchPayload.getOperators().orEmpty().mapKeys { it.key.name }
        val conditions = listSearchPayload.getCriterions() ?: listSearchPayload::class.declaredMemberProperties
            .mapNotNull { p ->
                val value = p.getter.call(listSearchPayload)
                val operator = operators[p.name] ?: OperatorEnum.EQ
                if (value == null && p.name !in listSearchPayload.getNullProperties().orEmpty()) null
                else Criterion(p.name, if (value == null) OperatorEnum.IS_NULL else operator, value)
            }
        val scoped = MutableListSearchPayload().apply {
            pageNo = listSearchPayload.pageNo
            pageSize = listSearchPayload.pageSize?.coerceAtMost(listSearchPayload.getMaxPageSize())
            orders = listSearchPayload.orders
            setReturnEntityClass(listSearchPayload.getReturnEntityClass())
            setReturnProperties(listSearchPayload.getReturnProperties())
            setCriterions(conditions + Criterion("userId", OperatorEnum.IN, owners, alias = "authorizedOwner"))
        }
        return super.pagingSearch(scoped)
    }
}
