package io.kudos.ability.data.vdb.milvus

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.MilvusTestContainer
import io.kudos.test.container.containers.ollama.OllamaEmbeddingModelEnum
import io.kudos.test.container.containers.ollama.OllamaMiniTestContainer
import io.kudos.test.container.containers.ollama.OllamaOfficialTestContainer
import jakarta.annotation.Resource
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.ai.vectorstore.milvus.MilvusSearchRequest
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Milvus VectorStore（基于 Spring AI VectorStore 抽象）的完整用法测试集合：
 * - schema/collection 初始化（通过配置 initialize-schema=true）
 * - 写入（add）
 * - 相似度检索（similaritySearch）
 * - 元数据过滤（字符串表达式 & DSL）
 * - 删除（按 id 列表、按过滤表达式）
 * - “更新”（同 id 覆盖：delete + add）
 * - Milvus 特有增强：MilvusSearchRequest（nativeExpression、searchParamsJson）
 * - 获取原生客户端句柄（getNativeClient）
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerAvailable
class MilvusVectorStoreTest {

    @Resource
    private lateinit var vectorStore: VectorStore

    @BeforeTest
    fun cleanupBeforeEach() {
        // 每个用例开始前，尽量清理上一次写入（避免跨用例相互影响）
        // delete(String) 是 VectorStore 的便捷方法，会内部转换为 Filter.Expression
        runCatching { vectorStore.delete("tenant in ['t0','t1','t2']") }
        runCatching { vectorStore.delete("category in ['basic','tech','batch','native']") }
    }

    @Test
    fun shouldAutoInitializeSchemaAndAddDocuments() {
        // 验证 initialize-schema 能让 collection 自动可用，并完成最基本写入与检索
        val docs = listOf(
            doc(id = "doc-1", text = "Tokyo is a big city.", metadata = mapOf("tenant" to "t1", "category" to "basic", "city" to "tokyo")),
            doc(id = "doc-2", text = "Osaka has great food.", metadata = mapOf("tenant" to "t1", "category" to "basic", "city" to "osaka")),
            doc(id = "doc-3", text = "Spring Boot 4 with Kotlin is productive.", metadata = mapOf("tenant" to "t2", "category" to "basic", "topic" to "spring"))
        )

        vectorStore.add(docs)

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Tokyo")
                .topK(5)
                .similarityThresholdAll()
                // Milvus 默认 IVF_FLAT + nprobe=1 可能召回很差，测试里显式提高 nprobe
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.any { it.id == "doc-1" })
        assertTrue(results.first().text.isNotBlank())
    }

    @Test
    fun shouldSimilaritySearchWithPlainSearchRequest() {
        // 使用通用 SearchRequest（非 MilvusSearchRequest）也能完成检索
        vectorStore.add(
            listOf(
                doc("doc-10", "Vector databases are great for similarity search.", mapOf("tenant" to "t1", "category" to "tech")),
                doc("doc-11", "Milvus is a vector database.", mapOf("tenant" to "t1", "category" to "tech"))
            )
        )

        val request = SearchRequest.builder()
            .query("similarity search")
            .topK(5)
            // 通用请求里不一定有 nprobe 之类参数；为了稳定，数据量小的用例通常也能命中
            .build()

        val results = awaitNonEmptySearch(request)
        assertTrue(results.any { it.id == "doc-10" || it.id == "doc-11" })
    }

    @Test
    fun shouldFilterWithStringExpression() {
        // 用字符串过滤表达式进行“元数据过滤 + 相似检索”
        vectorStore.add(
            listOf(
                doc("doc-20", "Tokyo travel guide.", mapOf("tenant" to "t1", "category" to "tech", "author" to "john")),
                doc("doc-21", "Tokyo ramen recommendation.", mapOf("tenant" to "t1", "category" to "food", "author" to "john")),
                doc("doc-22", "Kotlin tips.", mapOf("tenant" to "t1", "category" to "tech", "author" to "jill"))
            )
        )

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Tokyo")
                .topK(10)
                .similarityThresholdAll()
                .filterExpression("tenant == 't1' && category == 'tech'")
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.all { it.metadata["tenant"] == "t1" && it.metadata["category"] == "tech" })
        assertTrue(results.any { it.id == "doc-20" })
    }

    @Test
    fun shouldFilterWithDslExpression() {
        // 用 FilterExpressionBuilder（DSL）进行过滤
        vectorStore.add(
            listOf(
                doc("doc-30", "Spring AI provides VectorStore abstraction.", mapOf("tenant" to "t1", "category" to "tech", "lang" to "java")),
                doc("doc-31", "Spring Framework 6 and Boot 4.", mapOf("tenant" to "t1", "category" to "tech", "lang" to "kotlin")),
                doc("doc-32", "Gardening in winter.", mapOf("tenant" to "t1", "category" to "life", "lang" to "zh"))
            )
        )

        val b = FilterExpressionBuilder()
        val filter: Filter.Expression = b.and(
            b.and(
                b.eq("tenant", "t1"),
                b.eq("category", "tech"),
            ),
            b.`in`("lang", "java", "kotlin")
        ).build()

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Spring Boot")
                .topK(10)
                .similarityThresholdAll()
                .filterExpression(filter)
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.metadata["tenant"] == "t1" && it.metadata["category"] == "tech" })
    }

    @Test
    fun shouldDeleteByIds() {
        // 按 id 列表删除
        vectorStore.add(
            listOf(
                doc("doc-40", "Delete me please.", mapOf("tenant" to "t1", "category" to "basic")),
                doc("doc-41", "Keep me.", mapOf("tenant" to "t1", "category" to "basic"))
            )
        )

        vectorStore.delete(listOf("doc-40"))

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Delete me")
                .topK(10)
                .similarityThresholdAll()
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.none { it.id == "doc-40" })
    }

    @Test
    fun shouldDeleteByFilterExpressionString() {
        // 按“字符串过滤表达式”删除
        vectorStore.add(
            listOf(
                doc("doc-50", "Batch delete - tech A", mapOf("tenant" to "t2", "category" to "tech")),
                doc("doc-51", "Batch delete - tech B", mapOf("tenant" to "t2", "category" to "tech")),
                doc("doc-52", "Should remain", mapOf("tenant" to "t2", "category" to "basic"))
            )
        )

        vectorStore.delete("tenant == 't2' && category == 'tech'")

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Batch delete")
                .topK(20)
                .similarityThresholdAll()
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.none { it.id == "doc-50" || it.id == "doc-51" })
    }

    @Test
    fun shouldUpdateByReplacingSameId() {
        // Milvus/Spring AI VectorStore 一般没有“原地 update”抽象；常见做法是 delete + add（同 id 覆盖）
        val id = "doc-60"
        vectorStore.add(listOf(doc(id, "Old content about Tokyo.", mapOf("tenant" to "t1", "category" to "basic"))))

        vectorStore.delete(listOf(id))
        vectorStore.add(listOf(doc(id, "New content about Tokyo tower.", mapOf("tenant" to "t1", "category" to "basic"))))

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Tokyo tower")
                .topK(5)
                .similarityThresholdAll()
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.any { it.id == id && it.text.contains("Tokyo tower") })
    }

    @Test
    fun shouldUseMilvusNativeExpressionOverrideFilterExpression() {
        // MilvusSearchRequest 支持 nativeExpression（Milvus 原生过滤表达式），并会覆盖 filterExpression
        vectorStore.add(
            listOf(
                doc("doc-70", "Native filter demo A", mapOf("tenant" to "t1", "category" to "native", "age" to 20)),
                doc("doc-71", "Native filter demo B", mapOf("tenant" to "t1", "category" to "native", "age" to 35))
            )
        )

        val results = awaitNonEmptySearch(
            MilvusSearchRequest.milvusBuilder()
                .query("Native filter demo")
                .topK(10)
                .similarityThresholdAll()
                // 这里故意写一个“会排除所有”的通用过滤
                .filterExpression("age < 0")
                // 用 Milvus 原生表达式覆盖：只要 age > 30
                .nativeExpression("""metadata["age"] > 30""")
                .searchParamsJson("""{"nprobe":64}""")
                .build()
        )

        assertTrue(results.any { it.id == "doc-71" })
        assertTrue(results.none { it.id == "doc-70" })
    }

    @Test
    fun shouldAccessNativeMilvusClientHandle() {
        // 通过 MilvusVectorStore.getNativeClient() 拿到底层 MilvusServiceClient（用于 Spring AI 抽象未覆盖的 Milvus 特性）
        val milvusStore = vectorStore as? MilvusVectorStore
        assertNotNull(milvusStore)

        val native = milvusStore.getNativeClient<Any>()
        assertTrue(native.isPresent)
    }

    // -------------------------
    // helpers
    // -------------------------

    private fun doc(id: String, text: String, metadata: Map<String, Any> = emptyMap()): Document {
        // 尽量用构造器（而不是 Document.builder），避免某些 store 的兼容性差异
        return Document(id, text, metadata)
    }

    private fun awaitNonEmptySearch(request: SearchRequest, retries: Int = 20, sleepMs: Long = 200): List<Document> {
        var last: List<Document> = emptyList()
        repeat(retries) {
            last = vectorStore.similaritySearch(request)
            if (last.isNotEmpty()) return last
            Thread.sleep(sleepMs)
        }
        return last
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            val model = OllamaEmbeddingModelEnum.ALL_MINILM
            registry.add("spring.ai.milvus.embedding-dimension") { model.dimension }
            OllamaMiniTestContainer.startIfNeeded(registry, model)
            MilvusTestContainer.startIfNeeded(registry)
        }
    }

}
