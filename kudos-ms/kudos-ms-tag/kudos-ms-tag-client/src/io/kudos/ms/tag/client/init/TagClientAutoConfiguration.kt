package io.kudos.ms.tag.client.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.tag.client.fallback.TagAssignmentFallback
import io.kudos.ms.tag.client.fallback.TagAttributeFactFallback
import io.kudos.ms.tag.client.fallback.TagCatalogFallback
import io.kudos.ms.tag.client.fallback.TagQueryFallback
import io.kudos.ms.tag.client.proxy.ITagAssignmentProxy
import io.kudos.ms.tag.client.proxy.ITagAttributeFactProxy
import io.kudos.ms.tag.client.proxy.ITagCatalogProxy
import io.kudos.ms.tag.client.proxy.ITagQueryProxy
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(
    group = TagClientAutoConfiguration.GROUP,
    types = [
        ITagAttributeFactProxy::class,
        ITagAssignmentProxy::class,
        ITagQueryProxy::class,
        ITagCatalogProxy::class,
    ],
)
@HttpServiceFallback(value = TagAttributeFactFallback::class, service = [ITagAttributeFactProxy::class], group = TagClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = TagAssignmentFallback::class, service = [ITagAssignmentProxy::class], group = TagClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = TagQueryFallback::class, service = [ITagQueryProxy::class], group = TagClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = TagCatalogFallback::class, service = [ITagCatalogProxy::class], group = TagClientAutoConfiguration.GROUP)
open class TagClientAutoConfiguration : IComponentInitializer {
    override fun getComponentName() = "kudos-ms-tag-client"

    companion object {
        const val GROUP = "tag"
    }
}
