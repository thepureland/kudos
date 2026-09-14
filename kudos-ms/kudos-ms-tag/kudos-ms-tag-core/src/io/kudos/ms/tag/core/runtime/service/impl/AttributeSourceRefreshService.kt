package io.kudos.ms.tag.core.runtime.service.impl

import io.kudos.ms.tag.core.fact.model.AttributeFactStatus
import io.kudos.ms.tag.core.fact.service.iservice.ITagAttributeFactService
import io.kudos.ms.tag.core.runtime.port.AttributeSourceBatch
import io.kudos.ms.tag.core.runtime.port.AttributeSourceConfig
import io.kudos.ms.tag.core.runtime.port.AttributeSourceProvider
import io.kudos.ms.tag.core.runtime.port.SourceCursor
import org.springframework.stereotype.Service

@Service
open class AttributeSourceRefreshService(
    providers: List<AttributeSourceProvider>,
    private val factService: ITagAttributeFactService,
) {
    private val providersByCode = providers.associateBy { provider ->
        require(provider.providerCode.isNotBlank()) { "Attribute source provider code must not be blank." }
        provider.providerCode
    }.also { indexed ->
        require(indexed.size == providers.size) { "Attribute source provider codes must be unique." }
    }

    open fun refresh(
        providerCode: String,
        config: AttributeSourceConfig,
        cursor: SourceCursor?,
        limit: Int,
    ): AttributeSourceBatch {
        require(limit in 1..1000) { "Attribute source fetch limit must be between 1 and 1000." }
        val provider = providersByCode[providerCode]
            ?: throw IllegalArgumentException("Attribute source provider [$providerCode] is not registered.")
        val validation = provider.validate(config)
        require(validation.isEmpty()) {
            validation.joinToString("; ") { "${it.code}: ${it.message}" }
        }
        val batch = provider.fetch(config, cursor, limit)
        require(batch.facts.all { fact ->
            fact.subjectKey.tenantId == config.tenantId &&
                fact.subjectKey.subjectType == config.subjectType &&
                fact.sourceCode == providerCode
        }) { "Attribute source provider returned a fact outside its configured scope." }
        val results = factService.submitFacts(batch.facts)
        require(results.all { it.status == AttributeFactStatus.APPLIED || it.status == AttributeFactStatus.DUPLICATE }) {
            "Attribute source provider facts were rejected during ingestion."
        }
        return batch
    }
}
