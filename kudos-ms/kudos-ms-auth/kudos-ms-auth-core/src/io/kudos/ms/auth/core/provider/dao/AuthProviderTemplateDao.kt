package io.kudos.ms.auth.core.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.auth.core.provider.model.table.AuthProviderTemplates
import org.springframework.stereotype.Repository

@Repository
open class AuthProviderTemplateDao :
    BaseCrudDao<String, AuthProviderTemplate, AuthProviderTemplates>() {

    open fun findByCode(code: String): AuthProviderTemplate? =
        search(Criteria(AuthProviderTemplate::code eq code.trim().uppercase())).firstOrNull()
}
