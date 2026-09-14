package io.kudos.ms.tag.core.security

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/** Restricts fact producers to subject types registered to their atomic service identity. */
@Component
open class TagSubjectWriteGuard {
    @Resource
    private lateinit var tenantAccessGuard: TagTenantAccessGuard

    @Resource
    private lateinit var subjectTypeDao: TagSubjectTypeDao

    open fun requireWrite(key: TagSubjectKey) {
        tenantAccessGuard.requireTenant(key.tenantId)
        if (tenantAccessGuard.hasPlatformManagementAuthority()) return
        val context = KudosContextHolder.getOrNull()
            ?: throw TagAccessDeniedException("A Kudos security context is required for tag writes.")
        val serviceCode = context.atomicServiceCode?.takeIf { it.isNotBlank() }
            ?: throw TagAccessDeniedException("An atomic service identity is required for tag writes.")
        val subjectType = subjectTypeDao.findByCode(key.subjectType)
            ?.takeIf { it.active }
            ?: throw TagAccessDeniedException("The tag subject type is not registered or active.")
        if (subjectType.ownerServiceCode != serviceCode) {
            throw TagAccessDeniedException("The calling service does not own this tag subject type.")
        }
    }
}
