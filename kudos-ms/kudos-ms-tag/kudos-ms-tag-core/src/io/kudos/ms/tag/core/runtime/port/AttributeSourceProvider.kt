package io.kudos.ms.tag.core.runtime.port

interface AttributeSourceProvider {
    val providerCode: String
    fun validate(config: AttributeSourceConfig): List<ValidationError>
    fun fetch(config: AttributeSourceConfig, cursor: SourceCursor?, limit: Int): AttributeSourceBatch
}
