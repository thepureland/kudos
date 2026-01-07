package io.kudos.test.container.containers.ollama

/**
 * ollama迷你测试容器
 *
 * @author K
 * @since 1.0.0
 */
object OllamaMiniTestContainer : AbstractOllamaTestContainer(
    imageName = "alpine/ollama:0.12.10",
    port = 11434,
    label = "Ollama-mini"
) {

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null, OllamaEmbeddingModelEnum.ALL_MINILM)
        println("Ollama ${container.host} port: $port")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}