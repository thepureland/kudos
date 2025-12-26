package io.kudos.ability.data.vectordb.milvus

import io.kudos.test.container.containers.MilvusTestContainer
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.ai.vectorstore.milvus.MilvusSearchRequest
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
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
 * 依赖要求（仅说明，代码不做依赖声明）：
 * - org.springframework.ai:spring-ai-starter-vector-store-milvus
 * - 你的 TestContainers 封装：io.kudos.test.container.containers.MilvusTestContainer
 */
@SpringBootTest(
    classes = [
        MilvusVectorStoreTest.TestApp::class,
        MilvusVectorStoreTest.TestConfig::class
    ]
)
@TestPropertySource(properties = [
    // 让向量库在测试时自动建 collection/schema（Spring AI 里这是 opt-in）
    "spring.ai.vectorstore.milvus.initialize-schema=true",
    // 避免“自动 id”行为不一致；测试里我们显式指定 Document.id
    "spring.ai.vectorstore.milvus.auto-id=false"
])
class MilvusVectorStoreTest {

    @jakarta.annotation.Resource
    private lateinit var vectorStore: VectorStore

    @BeforeTest
    fun cleanupBeforeEach() {
        // 中文：每个用例开始前，尽量清理上一次写入（避免跨用例相互影响）
        // 说明：delete(String) 是 VectorStore 的便捷方法，会内部转换为 Filter.Expression
        runCatching { vectorStore.delete("tenant in ['t0','t1','t2']") }
        runCatching { vectorStore.delete("category in ['basic','tech','batch','native']") }
    }

    @Test
    fun shouldAutoInitializeSchemaAndAddDocuments() {
        // 中文：验证 initialize-schema 能让 collection 自动可用，并完成最基本写入与检索
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
        // 中文：使用通用 SearchRequest（非 MilvusSearchRequest）也能完成检索
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
        // 中文：用字符串过滤表达式进行“元数据过滤 + 相似检索”
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
        // 中文：用 FilterExpressionBuilder（DSL）进行过滤
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
        // 中文：按 id 列表删除
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
        // 中文：按“字符串过滤表达式”删除
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
        // 中文：Milvus/Spring AI VectorStore 一般没有“原地 update”抽象；常见做法是 delete + add（同 id 覆盖）
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
        // 中文：MilvusSearchRequest 支持 nativeExpression（Milvus 原生过滤表达式），并会覆盖 filterExpression
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
        // 中文：通过 MilvusVectorStore.getNativeClient() 拿到底层 MilvusServiceClient（用于 Spring AI 抽象未覆盖的 Milvus 特性）
        val milvusStore = vectorStore as? MilvusVectorStore
        assertNotNull(milvusStore)

        val native = milvusStore.getNativeClient<Any>()
        assertTrue(native.isPresent)
    }

    // -------------------------
    // helpers
    // -------------------------

    private fun doc(id: String, text: String, metadata: Map<String, Any> = emptyMap()): Document {
        // 说明：尽量用构造器（而不是 Document.builder），避免某些 store 的兼容性差异
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

    // -------------------------
    // Spring Boot test app + test beans
    // -------------------------

    @SpringBootApplication
    open class TestApp

    class TestConfig {

        @Bean
        @Primary
        fun deterministicEmbeddingModel(): EmbeddingModel = DeterministicEmbeddingModel(embeddingDimensions = EMBEDDING_DIM)

        /**
         * 一个稳定、可重复的本地 EmbeddingModel：
         * - 不依赖任何外部服务（与你“与 AI 无关”的向量库测试目标匹配）
         * - 维度固定，便于和 milvus.embedding-dimension 对齐
         */
        private class DeterministicEmbeddingModel(
            private val embeddingDimensions: Int
        ) : EmbeddingModel {

            override fun call(request: EmbeddingRequest): EmbeddingResponse {
                val embeddings = request.instructions.mapIndexed { idx, text ->
                    Embedding(embedText(text), idx)
                }
                return EmbeddingResponse(embeddings)
            }

            override fun embed(document: Document): FloatArray {
                return embedText(document.text)
            }

            override fun dimensions(): Int = embeddingDimensions

            private fun embedText(text: String): FloatArray {
                val seed = stableHash(text)
                return FloatArray(embeddingDimensions) { i ->
                    // [-1, 1] 的稳定浮点分布
                    val x = seed xor (i * 0x9E3779B9.toInt())
                    ((x % 2000) / 1000.0f) - 1.0f
                }
            }

            private fun stableHash(s: String): Int {
                // 使用 String.hashCode 作为稳定基准，再做一次扰动
                var h = s.hashCode()
                h = h xor (h ushr 16)
                h *= 0x85ebca6b.toInt()
                h = h xor (h ushr 13)
                h *= 0xc2b2ae35.toInt()
                h = h xor (h ushr 16)
                return h
            }
        }
    }

    companion object {
        private const val EMBEDDING_DIM = 8
        private const val COLLECTION_NAME = "kudos_milvus_vector_store_test"

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            // 启动你封装好的容器，并让它把 host/port 等注册进来
            MilvusTestContainer.startIfNeeded(registry)

            // 额外补齐 Spring AI Milvus VectorStore 必要配置（维度必须匹配 EmbeddingModel）
            registry.add("spring.ai.vectorstore.milvus.initialize-schema") { "true" }
            registry.add("spring.ai.vectorstore.milvus.collection-name") { COLLECTION_NAME }
            registry.add("spring.ai.vectorstore.milvus.embedding-dimension") { EMBEDDING_DIM.toString() }

            // 指标/索引等可按需调整；测试默认给一个比较通用的组合
            registry.add("spring.ai.vectorstore.milvus.metric-type") { "COSINE" }
            registry.add("spring.ai.vectorstore.milvus.index-type") { "IVF_FLAT" }
            registry.add("spring.ai.vectorstore.milvus.index-parameters") { """{"nlist":64}""" }
        }
    }
}
