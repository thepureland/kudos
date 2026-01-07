package io.kudos.test.container.containers.ollama

/**
 * ollama官方测试容器
 *
 * @author K
 * @since 1.0.0
 */
object OllamaOfficialTestContainer : AbstractOllamaTestContainer(
    imageName = "ollama/ollama:0.13.5",
    port = 11435,
    label = "Ollama-Official"
) {

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null, OllamaEmbeddingModelEnum.ALL_MINILM)
        println("Ollama ${container.host} port: $port")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}