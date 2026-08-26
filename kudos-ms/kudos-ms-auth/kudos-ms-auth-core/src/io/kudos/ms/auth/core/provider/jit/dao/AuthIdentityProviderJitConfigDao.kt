package io.kudos.ms.auth.core.provider.jit.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.provider.jit.model.po.AuthIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.table.AuthIdentityProviderJitConfigs
import org.springframework.stereotype.Repository

@Repository
open class AuthIdentityProviderJitConfigDao :
    BaseCrudDao<String, AuthIdentityProviderJitConfig, AuthIdentityProviderJitConfigs>()
