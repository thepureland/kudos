package io.kudos.ms.auth.core.authentication.mfa.policy.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.authentication.mfa.policy.model.po.AuthTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.table.AuthTenantMfaPolicies
import org.springframework.stereotype.Repository

@Repository
open class AuthTenantMfaPolicyDao :
    BaseCrudDao<String, AuthTenantMfaPolicy, AuthTenantMfaPolicies>()
