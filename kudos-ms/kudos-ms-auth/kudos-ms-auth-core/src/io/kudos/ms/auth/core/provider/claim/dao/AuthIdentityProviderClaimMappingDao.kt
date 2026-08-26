package io.kudos.ms.auth.core.provider.claim.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.provider.claim.model.po.AuthIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.model.table.AuthIdentityProviderClaimMappings
import org.springframework.stereotype.Repository

@Repository
open class AuthIdentityProviderClaimMappingDao :
    BaseCrudDao<String, AuthIdentityProviderClaimMapping, AuthIdentityProviderClaimMappings>()
