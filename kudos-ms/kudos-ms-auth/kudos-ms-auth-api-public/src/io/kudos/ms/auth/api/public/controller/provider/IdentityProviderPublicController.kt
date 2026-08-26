package io.kudos.ms.auth.api.public.controller.provider

import io.kudos.ms.auth.common.provider.vo.IdentityProviderDescriptor
import io.kudos.ms.auth.core.provider.service.iservice.IIdentityProviderCatalogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Secret-free provider discovery used before a user is authenticated. */
@RestController
@RequestMapping("/api/public/auth/providers")
open class IdentityProviderPublicController(
    private val catalogService: IIdentityProviderCatalogService,
) {
    @GetMapping
    open fun getAvailableProviders(
        @RequestParam tenantId: String,
    ): List<IdentityProviderDescriptor> = catalogService.getAvailableProviders(tenantId)
}
