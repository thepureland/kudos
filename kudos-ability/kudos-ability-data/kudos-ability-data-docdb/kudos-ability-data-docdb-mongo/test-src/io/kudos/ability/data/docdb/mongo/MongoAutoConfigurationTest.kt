package io.kudos.ability.data.docdb.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MongoTestContainer
import jakarta.annotation.Resource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Mongo MVP auto-config, against a real Mongo 7 via testcontainer.
 *
 * Coverage:
 *  - `MongoTemplate` is wired automatically when spring-data-mongodb is on the classpath; the
 *    auto-config doesn't get in its way.
 *  - The registered `MongoCustomConversions` lets [BigInteger] fields round-trip with full
 *    precision, including values well past `Long.MAX_VALUE`.
 *  - The persisted BSON field is stored as String — verified by reading the raw `Document`
 *    rather than the strongly-typed POKO, which is what enables 100-digit precision.
 *
 * Why an explicit [MongoClient] bean: in Spring Boot 4, `MongoAutoConfiguration` resolves the
 * connection details DURING context refresh, BEFORE `@DynamicPropertySource` rewrites
 * `spring.data.mongodb.*`. So even though the environment ends up with the testcontainer URI,
 * the `MongoClient` bean is already wired against the default `localhost:27017` and ignores the
 * rewrite (this is a Mongo-specific quirk; `@DynamicPropertySource` works fine for Redis). The
 * `@TestConfiguration` builds the [MongoClient] directly from [Environment] after the dynamic
 * properties have landed, and marks it `@Primary` so Spring Boot's default client steps aside.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(MongoAutoConfigurationTest.TestMongoClientConfig::class)
internal class MongoAutoConfigurationTest {

    @Resource
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeTest
    fun cleanUp() {
        mongoTemplate.dropCollection(WalletDoc.COLLECTION)
    }

    @AfterTest
    fun cleanUpAfter() {
        mongoTemplate.dropCollection(WalletDoc.COLLECTION)
    }

    @Test
    fun mongoTemplate_isAutowiredBySpringBoot() {
        assertNotNull(mongoTemplate, "MongoTemplate must be present in the context")
    }

    @Test
    fun bigInteger_roundTripsThroughMongo() {
        val balance = BigInteger("12345678901234567890123456789012345")
        val saved = mongoTemplate.save(WalletDoc(id = "wallet-1", owner = "alice", balance = balance))

        val read = mongoTemplate.findById(saved.id, WalletDoc::class.java)

        assertNotNull(read, "wallet must round-trip")
        assertEquals(balance, read.balance, "35-digit BigInteger must survive the round trip with full precision")
    }

    @Test
    fun bigInteger_storedAsStringFieldNotNumeric() {
        // Read the raw BSON to confirm the on-wire encoding. If this fails the converter isn't
        // being applied (the field would land as Long / Decimal128 / "$numberLong" etc.).
        val balance = BigInteger("99999999999999999999")
        mongoTemplate.save(WalletDoc(id = "wallet-2", owner = "bob", balance = balance))

        val rawDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("_id").`is`("wallet-2")),
            org.bson.Document::class.java,
            WalletDoc.COLLECTION,
        )

        assertNotNull(rawDoc, "raw BSON document must be retrievable")
        val rawBalance = rawDoc["balance"]
        assertTrue(
            rawBalance is String,
            "the persisted `balance` must be a String — that's the proof the BigIntegerConverters are wired in; got: ${rawBalance?.javaClass}",
        )
        assertEquals("99999999999999999999", rawBalance)
    }

    @Test
    fun bigInteger_overLongMaxValuePrecisionSurvives() {
        // 30+ digits is past every native BSON numeric type (Long: 19 digits; Decimal128: 34).
        // The converter is the only way to retain exact value here.
        val huge = BigInteger("1${"0".repeat(30)}5") // 1 followed by 30 zeros + 5 = 32 digits
        mongoTemplate.save(WalletDoc(id = "wallet-3", owner = "carol", balance = huge))

        val read = mongoTemplate.findById("wallet-3", WalletDoc::class.java)
        assertEquals(huge, read?.balance)
        assertTrue(
            (read?.balance?.toString()?.length ?: 0) >= 32,
            "32-digit value must come back at full length, not truncated to Long range",
        )
    }

    @Test
    fun queryByOwner_worksWhenBigIntegerIsStored() {
        // Sanity check: storing BigInteger fields as String does not break standard non-BigInteger
        // queries on sibling fields. (Soul shipped this without a regression test; it's cheap
        // insurance.)
        val balance = BigInteger("1000")
        mongoTemplate.save(WalletDoc(id = "wallet-4", owner = "dave", balance = balance))

        val read = mongoTemplate.findOne(
            Query.query(Criteria.where("owner").`is`("dave")),
            WalletDoc::class.java,
        )

        assertNotNull(read)
        assertEquals("wallet-4", read.id)
        assertEquals(balance, read.balance)
    }

    @Document(collection = WalletDoc.COLLECTION)
    data class WalletDoc(
        @Id val id: String,
        val owner: String,
        val balance: BigInteger,
    ) {
        companion object {
            const val COLLECTION: String = "test-mongo-mvp-wallets"
        }
    }

    /**
     * Builds the [MongoClient] from `spring.data.mongodb.uri` AFTER `@DynamicPropertySource` has
     * rewritten the environment. `@Primary` ensures Spring Boot's default (which races with the
     * dynamic rewrite and ends up on `localhost:27017`) steps aside.
     */
    @TestConfiguration(proxyBeanMethods = false)
    open class TestMongoClientConfig {
        @Bean
        @Primary
        open fun testMongoClient(environment: Environment): MongoClient {
            val uri = requireNotNull(environment.getProperty("spring.data.mongodb.uri")) {
                "spring.data.mongodb.uri was not registered; MongoTestContainer.startIfNeeded() should have set it"
            }
            return MongoClients.create(uri)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            MongoTestContainer.startIfNeeded(registry)
        }
    }
}
