package io.kudos.ms.tag.api.internal.controller

import io.kudos.ms.tag.common.query.api.ITagQueryApi
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RestController

@RestController
open class TagQueryInternalController(
    @Qualifier("tagQueryApi") private val delegate: ITagQueryApi,
    private val tenantAccessGuard: TagTenantAccessGuard,
) : ITagQueryApi {
    override fun findSubjects(request: TagQueryRequest): TagSubjectPage {
        tenantAccessGuard.requireTenant(request.tenantId)
        return delegate.findSubjects(request)
    }
}
