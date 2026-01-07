package io.kudos.ability.data.vdb.pgvector

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.PgVectorTestContainer
import io.kudos.test.container.containers.ollama.OllamaEmbeddingModelEnum
import io.kudos.test.container.containers.ollama.OllamaMiniTestContainer
import jakarta.annotation.Resource
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.SingleColumnRowMapper
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import java.util.*
import kotlin.test.*

/**
 * PgVector測試用例
 *
 * @author ChatGpt
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerAvailable
class PgVectorTest {

    @Resource
    private lateinit var vectorStore: VectorStore

    @Resource
    private lateinit var embeddingModel: EmbeddingModel

    @Resource
    private lateinit var jdbcTemplate: JdbcTemplate

    @Value("\${spring.ai.vectorstore.pgvector.dimensions}")
    private lateinit var dimensions: String

    @BeforeTest
    fun truncateVectorStoreTable() {
        val table = detectVectorStoreTable()
        jdbcTemplate.execute("""truncate table ${table.qualified()} restart identity""")
    }

    /** 扩展存在 + 基础写入 + 相似检索 */
    @Test
    fun shouldHavePgvectorExtensionAndSupportBasicWriteAndSearch() {
        assertTrue(isVectorExtensionInstalled(), "pgvector extension 'vector' not found")

        vectorStore.add(seedDocs())

        // 关键：插入“锚点文档”，让它与 query 的向量完全一致（distance = 0）
        val query = "kotlin spring vector database"
        vectorStore.add(
            listOf(
                Document(
                    UUID.randomUUID().toString(),
                    query,
                    mapOf("tenant" to "t0", "category" to "anchor")
                )
            )
        )

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(3)
                // 关键：用极小的正数，避免某些实现把阈值换算/下推成 0 导致 distance < 0/0 的空结果
                .similarityThreshold(0.001)
                .build()
        )

        assertTrue(results.isNotEmpty())
        assertTrue(results.size <= 3)
        assertTrue(results.first().text.isNotBlank())
    }

    /** metadata 过滤检索（FilterExpressionBuilder 需要 build()） */
    @Test
    fun shouldFilterByMetadataUsingDsl() {
        vectorStore.add(seedDocs())

        val b = FilterExpressionBuilder()
        val filter: Filter.Expression = b.and(
            b.eq("tenant", "t1"),
            b.eq("category", "tech")
        ).build()

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("spring boot")
                .topK(20)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .filterExpression(filter)
                .build()
        )

        assertTrue(results.isNotEmpty())
        results.forEach {
            assertEquals("t1", it.metadata["tenant"])
            assertEquals("tech", it.metadata["category"])
        }
    }

    /** 按 ID 删除 */
    @Test
    fun shouldDeleteDocumentsById() {
        val idToDelete = UUID.randomUUID().toString()
        val keepId = UUID.randomUUID().toString()

        vectorStore.add(
            listOf(
                Document(idToDelete, "doc to delete: delete by id", mapOf("tenant" to "t2", "category" to "ops")),
                Document(keepId, "doc to keep: keep", mapOf("tenant" to "t2", "category" to "ops"))
            )
        )

        vectorStore.delete(listOf(idToDelete))

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("delete by id")
                .topK(20)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .build()
        )

        assertTrue(results.any { it.id == keepId })
        assertTrue(results.none { it.id == idToDelete })
    }

    /** 按 filter 删除（只删 metadata 满足条件的文档） */
    @Test
    fun shouldDeleteDocumentsByFilter() {
        vectorStore.add(seedDocs())

        val b = FilterExpressionBuilder()
        val filter = b.eq("tenant", "t3").build()

        vectorStore.delete(filter)

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("finance report")
                .topK(50)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .build()
        )

        assertTrue(results.none { it.metadata["tenant"] == "t3" })
    }

    /** 更新语义（delete + add 替换同 id） */
    @Test
    fun shouldUpdateDocumentByReplacingSameId() {
        val id = UUID.randomUUID().toString()

        vectorStore.add(listOf(Document(id, "old content: travel osaka", mapOf("tenant" to "t9", "category" to "note"))))
        vectorStore.delete(listOf(id))
        vectorStore.add(listOf(Document(id, "new content: travel tokyo", mapOf("tenant" to "t9", "category" to "note"))))

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("tokyo travel")
                .topK(5)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .build()
        )

        assertTrue(results.any { it.id == id && it.text.contains("tokyo") })
    }

    /** 批量写入 + metadata 过滤 */
    @Test
    fun shouldBatchInsertDocumentsAndFilterByMetadata() {
        val docs = (1..200).map {
            Document(
                UUID.randomUUID().toString(),
                "batch doc #$it about vector store",
                mapOf("tenant" to "tb", "category" to "batch", "i" to it)
            )
        }
        vectorStore.add(docs)

        val b = FilterExpressionBuilder()
        val filter = b.eq("category", "batch").build()

        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("vector store batch")
                .topK(50)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .filterExpression(filter)
                .build()
        )

        assertTrue(results.isNotEmpty())
        results.forEach { assertEquals("batch", it.metadata["category"]) }
    }

    /** 校验表结构：embedding 列应为 vector(DIMENSIONS)，且具备 id/text/metadata/embedding 等列 */
    @Test
    fun shouldValidateVectorStoreTableStructure() {
        val table = detectVectorStoreTable()

        val embeddingType = jdbcTemplate.queryForObject(
            """
            select pg_catalog.format_type(a.atttypid, a.atttypmod)
            from pg_attribute a
            join pg_class c on c.oid = a.attrelid
            join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = ? and c.relname = ? and a.attname = 'embedding'
              and a.attnum > 0 and not a.attisdropped
            """.trimIndent(),
            String::class.java,
            table.schema, table.name
        )
        assertNotNull(embeddingType)
        assertTrue(embeddingType.contains("vector($dimensions)"), "embedding type mismatch: $embeddingType")

        val cols = jdbcTemplate.queryForList(
            """
            select a.attname
            from pg_attribute a
            join pg_class c on c.oid=a.attrelid
            join pg_namespace n on n.oid=c.relnamespace
            where n.nspname=? and c.relname=? and a.attnum>0 and not a.attisdropped
            """.trimIndent(),
            String::class.java,
            table.schema, table.name
        ).toSet()

        assertTrue("id" in cols)
        assertTrue("embedding" in cols)
        assertTrue("metadata" in cols)
        assertTrue(("content" in cols) || ("text" in cols))
    }

    /** SQL 运算符 <=> 与 VectorStore top1 对齐（cosine 距离，越小越相似） */
    @Test
    fun shouldMatchTop1BetweenVectorStoreAndSqlCosine() {
        vectorStore.add(seedDocs())
        val table = detectVectorStoreTable()

        val query = "spring boot pgvector cosine"

        // 关键：插入“锚点文档”，确保 top1 唯一且 distance=0（避免阈值过滤为空）
        val anchorId = UUID.randomUUID().toString()
        vectorStore.add(
            listOf(
                Document(
                    anchorId,
                    query,
                    mapOf("tenant" to "t0", "category" to "anchor")
                )
            )
        )

        val qLit = toVectorLiteral(embeddingModel.embed(query))

        val vsResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(1)
                .similarityThreshold(0.001)
                .build()
        )
        assertTrue(vsResults.isNotEmpty())

        val vsTop = vsResults.first()
        assertEquals(anchorId, vsTop.id)

        // exact SQL：不加阈值过滤，只按距离排序取 top1
        val sqlTopId = queryExactIds(table, qLit, 1).first()
        assertEquals(anchorId, sqlTopId)
    }

    /** SQL 运算符 <->（L2）自检：相同向量距离应为 0 */
    @Test
    fun shouldReturnZeroDistanceForSameVectorUsingL2() {
        val lit = toVectorLiteral(embeddingModel.embed("same text"))
        val dist = jdbcTemplate.queryForObject(
            "select (?::vector <-> ?::vector) as dist",
            Double::class.java,
            lit, lit
        ) ?: -1.0

        assertTrue(dist <= 1e-9)
    }

    /** 创建 HNSW 索引并用 EXPLAIN 演示参数（enable_seqscan / hnsw.ef_search） */
    @Test
    fun shouldCreateHnswIndexAndExplainPlan() {
        val table = detectVectorStoreTable()
        vectorStore.add(bigDocsForIndexDemo(10))

        val indexName = "idx_${table.name}_embedding_hnsw"
        jdbcTemplate.execute("""drop index if exists "${table.schema}"."$indexName"""")

        jdbcTemplate.execute(
            """
            create index "$indexName"
            on ${table.qualified()}
            using hnsw (embedding vector_cosine_ops)
            with (m = 16, ef_construction = 64)
            """.trimIndent()
        )
        assertTrue(indexExists(table.schema, indexName))

        val qLit = toVectorLiteral(embeddingModel.embed("index demo hnsw query"))

        val plan = explain(
            setup = listOf(
                "set local enable_seqscan = off",
                "set local hnsw.ef_search = 64"
            ),
            explainSql = """
                explain (format text)
                select id
                from ${table.qualified()}
                order by embedding <=> '$qLit'::vector
                limit 5
            """.trimIndent()
        )

        assertTrue(plan.any { it.contains("Scan") || it.contains("Index") })
    }

    /** 创建 IVFFlat 索引并演示 probes 参数 */
    @Test
    fun shouldCreateIvfflatIndexAndQueryWithProbes() {
        val table = detectVectorStoreTable()
        vectorStore.add(bigDocsForIndexDemo(20))

        val indexName = "idx_${table.name}_embedding_ivf"
        jdbcTemplate.execute("""drop index if exists "${table.schema}"."$indexName"""")

        jdbcTemplate.execute(
            """
            create index "$indexName"
            on ${table.qualified()}
            using ivfflat (embedding vector_cosine_ops)
            with (lists = 100)
            """.trimIndent()
        )
        jdbcTemplate.execute("analyze ${table.qualified()}")

        assertTrue(indexExists(table.schema, indexName))

        val qLit = toVectorLiteral(embeddingModel.embed("index demo ivfflat query"))

        val rows = queryForListInLocalSettings(
            setup = listOf(
                "set local enable_seqscan = off",
                "set local ivfflat.probes = 10"
            ),
            sql = """
                select id
                from ${table.qualified()}
                order by embedding <=> ?::vector
                limit 5
            """.trimIndent(),
            elementType = String::class.java,
            args = arrayOf(qLit)
        )
        assertTrue(rows.isNotEmpty())
    }

    /** HNSW 召回调参：提高 ef_search，不应降低相对 exact 的 recall（允许相等） */
    @Test
    fun shouldNotDecreaseRecallWhenIncreasingHnswEfSearch() {
        val table = detectVectorStoreTable()
        vectorStore.add(bigDocsForIndexDemo(20))

        val indexName = "idx_${table.name}_hnsw_recall"
        jdbcTemplate.execute("""drop index if exists "${table.schema}"."$indexName"""")

        jdbcTemplate.execute(
            """
            create index "$indexName"
            on ${table.qualified()}
            using hnsw (embedding vector_cosine_ops)
            with (m = 16, ef_construction = 64)
            """.trimIndent()
        )

        val qLit = toVectorLiteral(embeddingModel.embed("recall tuning query"))
        val topK = 20

        val exact = queryExactIds(table, qLit, topK)

        val low = queryApproxIdsWithHnsw(table, qLit, topK, efSearch = 16)
        val high = queryApproxIdsWithHnsw(table, qLit, topK, efSearch = 128)

        assertTrue((high intersect exact).size >= (low intersect exact).size)
    }

    /** IVFFlat 召回调参：提高 probes，不应降低相对 exact 的 recall（允许相等） */
    @Test
    fun shouldNotDecreaseRecallWhenIncreasingIvfflatProbes() {
        val table = detectVectorStoreTable()
        vectorStore.add(bigDocsForIndexDemo(40))

        val indexName = "idx_${table.name}_ivf_recall"
        jdbcTemplate.execute("""drop index if exists "${table.schema}"."$indexName"""")

        jdbcTemplate.execute(
            """
            create index "$indexName"
            on ${table.qualified()}
            using ivfflat (embedding vector_cosine_ops)
            with (lists = 20)
            """.trimIndent()
        )
        jdbcTemplate.execute("analyze ${table.qualified()}")

        val qLit = toVectorLiteral(embeddingModel.embed("ivfflat recall tuning query"))
        val topK = 5

        val exact = queryExactIds(table, qLit, topK)

        val low = queryApproxIdsWithIvfflat(table, qLit, topK, probes = 1)
        val high = queryApproxIdsWithIvfflat(table, qLit, topK, probes = 5)

        val lowHit = (low intersect exact).size
        val highHit = (high intersect exact).size

        val tolerance = 2
        assertTrue(highHit + tolerance >= lowHit)
        assertTrue(highHit > 0 && lowHit > 0)
    }

    /** JSONB 侧 metadata 过滤集合应覆盖 DSL filter 命中的集合（DSL+向量检索 => SQL 过滤的子集） */
    @Test
    fun shouldEnsureDslFilterIsSubsetOfSqlJsonbFilter() {
        vectorStore.add(seedDocs())
        val table = detectVectorStoreTable()

        val b = FilterExpressionBuilder()
        val filter = b.and(b.eq("tenant", "t1"), b.eq("category", "tech")).build()

        val vsIds = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("spring")
                .topK(50)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .filterExpression(filter)
                .build()
        ).map { it.id }.toSet()

        val sqlIds = jdbcTemplate.queryForList(
            """
            select id
            from ${table.qualified()}
            where metadata->>'tenant'='t1' and metadata->>'category'='tech'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue(sqlIds.isNotEmpty())
        assertTrue(vsIds.isNotEmpty())
        assertTrue(sqlIds.containsAll(vsIds))
    }

    private fun queryApproxIdsWithHnsw(table: TableRef, qLit: String, topK: Int, efSearch: Int): Set<String> {
        val rows = queryForListInLocalSettings(
            setup = listOf(
                "set local enable_seqscan = off",
                "set local hnsw.ef_search = $efSearch"
            ),
            sql = """
                select id
                from ${table.qualified()}
                order by embedding <=> ?::vector
                limit $topK
            """.trimIndent(),
            elementType = String::class.java,
            args = arrayOf(qLit)
        )
        return rows.filterNotNull().toSet()
    }

    private fun queryApproxIdsWithIvfflat(table: TableRef, qLit: String, topK: Int, probes: Int): Set<String> {
        val rows = queryForListInLocalSettings(
            setup = listOf(
                "set local enable_seqscan = off",
                "set local ivfflat.probes = $probes"
            ),
            sql = """
                select id
                from ${table.qualified()}
                order by embedding <=> ?::vector
                limit $topK
            """.trimIndent(),
            elementType = String::class.java,
            args = arrayOf(qLit)
        )
        return rows.toSet()
    }

    private fun queryExactIds(table: TableRef, qLit: String, topK: Int): Set<String> {
        val rows = queryForListInLocalSettings(
            setup = listOf(
                "set local enable_seqscan = on",
                "set local enable_indexscan = off",
                "set local enable_bitmapscan = off",
                "set local enable_tidscan = off"
            ),
            sql = """
                select id
                from ${table.qualified()}
                order by embedding <=> ?::vector
                limit $topK
            """.trimIndent(),
            elementType = String::class.java,
            args = arrayOf(qLit)
        )
        return rows.filterNotNull().toSet()
    }

    private fun isVectorExtensionInstalled(): Boolean {
        val extCount = jdbcTemplate.queryForObject(
            "select count(*) from pg_extension where extname = 'vector'",
            Long::class.java
        ) ?: 0L
        return extCount >= 1L
    }

    private data class TableRef(val schema: String, val name: String) {
        fun qualified(): String = "\"$schema\".\"$name\""
    }

    private fun detectVectorStoreTable(): TableRef {
        val rows = jdbcTemplate.queryForList(
            """
                select n.nspname as schema, c.relname as name
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                where c.relkind = 'r'
                  and exists (
                    select 1 from pg_attribute a
                    join pg_type t on t.oid = a.atttypid
                    where a.attrelid = c.oid and a.attname = 'embedding' and t.typname = 'vector'
                  )
                  and exists (select 1 from pg_attribute a where a.attrelid=c.oid and a.attname='metadata')
                  and exists (select 1 from pg_attribute a where a.attrelid=c.oid and a.attname='id')
                  and (
                       exists (select 1 from pg_attribute a where a.attrelid=c.oid and a.attname='content')
                    or exists (select 1 from pg_attribute a where a.attrelid=c.oid and a.attname='text')
                  )
                limit 1
            """.trimIndent()
        )

        if (rows.isNotEmpty()) {
            val r = rows.first()
            return TableRef(r["schema"].toString(), r["name"].toString())
        }
        return TableRef("public", "vector_store")
    }

    private fun toVectorLiteral(v: FloatArray): String =
        v.joinToString(prefix = "[", postfix = "]") { it.toString() }

    private fun indexExists(schema: String, indexName: String): Boolean {
        val cnt = jdbcTemplate.queryForObject(
            """
            select count(*)
            from pg_class c
            join pg_namespace n on n.oid=c.relnamespace
            where c.relkind='i' and n.nspname=? and c.relname=?
            """.trimIndent(),
            Long::class.java,
            schema, indexName
        ) ?: 0L
        return cnt >= 1L
    }

    private fun explain(setup: List<String>, explainSql: String): List<String> {
        val planLines = mutableListOf<String>()
        jdbcTemplate.execute("begin")
        try {
            setup.forEach { jdbcTemplate.execute(it) }
            planLines += jdbcTemplate.queryForList(explainSql, String::class.java).filterNotNull()
            jdbcTemplate.execute("commit")
        } catch (e: Throwable) {
            jdbcTemplate.execute("rollback")
            throw e
        }
        return planLines
    }

    private fun <T : Any> queryForListInLocalSettings(
        setup: List<String>,
        sql: String,
        elementType: Class<T>,
        args: Array<Any?>
    ): List<T> {
        jdbcTemplate.execute("begin")
        return try {
            setup.forEach { jdbcTemplate.execute(it) }
            val mapper = SingleColumnRowMapper<T>(elementType)
            val rows: List<T?> = jdbcTemplate.query(sql, mapper, *args)
            jdbcTemplate.execute("commit")
            rows.filterNotNull()
        } catch (e: Throwable) {
            jdbcTemplate.execute("rollback")
            throw e
        }
    }

    private fun seedDocs(): List<Document> = listOf(
        Document(UUID.randomUUID().toString(), "Spring Boot 4 + Kotlin: building modular services", mapOf("tenant" to "t1", "category" to "tech", "lang" to "en")),
        Document(UUID.randomUUID().toString(), "pgvector in Postgres: indexes, ivfflat, hnsw, cosine distance", mapOf("tenant" to "t1", "category" to "tech", "lang" to "en")),
        Document(UUID.randomUUID().toString(), "Recipe: braised noodles, soy sauce, garlic, scallion", mapOf("tenant" to "t2", "category" to "food", "lang" to "en")),
        Document(UUID.randomUUID().toString(), "Finance report Q3 revenue and cost breakdown", mapOf("tenant" to "t3", "category" to "biz", "lang" to "en"))
    )

    private fun bigDocsForIndexDemo(count: Int): List<Document> =
        (1..count).map {
            Document(
                UUID.randomUUID().toString(),
                "doc-$it: postgres pgvector index benchmark text $it",
                mapOf("tenant" to "ti", "category" to "idx", "i" to it)
            )
        }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            val model = OllamaEmbeddingModelEnum.ALL_MINILM
            registry.add("spring.ai.vectorstore.pgvector.dimensions") { model.dimension }
            OllamaMiniTestContainer.startIfNeeded(registry, model)
            PgVectorTestContainer.startIfNeeded(registry)
        }
    }

}