package io.kudos.test.container.containers.ollama


/**
 * Ollama支持的嵌入模型枚舉
 *
 * @author K
 * @since 1.0.0
 */
enum class OllamaEmbeddingModelEnum(
    override val modelName: String,
    val dimension: Int
) : OllamaModel {

    ALL_MINILM("all-minilm:l6-v2", 384), // 文本向量嵌入模型 384維 小、快 適合測試/開發

    NOMIC_EMBED_TEXT("nomic-embed-text", 768), // 文本向量嵌入模型 768維 高质量、通用 適合生产/RAG

}