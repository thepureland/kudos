package io.kudos.ability.data.docdb.mongo.init

import io.kudos.ability.data.docdb.mongo.init.properties.MongoCustomProperties
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure-unit + conditional-configuration tests for [MongoAutoConfiguration], complementing the
 * testcontainer-backed integration test (`MongoAutoConfigurationTest`) which only exercises the
 * happy path against a real Mongo.
 *
 * Coverage:
 *  - Direct unit: [MongoAutoConfiguration.mongoCustomConversions] registers both directions of
 *    the BigInteger<->String converter pair (write target and read target), and
 *    [MongoAutoConfiguration.getComponentName] returns the module name used in init logging.
 *  - Conditional wiring via [ApplicationContextRunner] (no Mongo connection needed — the bean
 *    method never touches a client):
 *      * default (property absent, `matchIfMissing = true`): bean present and `@Primary`;
 *      * explicit `big-integer-as-string=true`: bean present;
 *      * explicit `big-integer-as-string=false`: bean absent — the documented opt-out path;
 *      * [MongoCustomProperties] is bound from the bundled module yml (default true) and from
 *        an override (false);
 *      * `@ConditionalOnClass(MongoTemplate)`: with spring-data-mongodb filtered off the
 *        classpath the whole configuration backs off — a transitive depender without the
 *        starter must not get the conversions bean.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MongoAutoConfigurationConditionTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(MongoAutoConfiguration::class.java)

    // ---------- pure unit, no Spring context ----------

    @Test
    fun componentName_matchesModuleArtifactId() {
        assertEquals("kudos-ability-data-docdb-mongo", MongoAutoConfiguration().getComponentName())
    }

    @Test
    fun mongoCustomConversions_registersBothConverterDirections() {
        val conversions = MongoAutoConfiguration().mongoCustomConversions()
        assertTrue(
            conversions.hasCustomWriteTarget(BigInteger::class.java, String::class.java),
            "writing converter BigInteger -> String must be registered",
        )
        assertTrue(
            conversions.hasCustomReadTarget(String::class.java, BigInteger::class.java),
            "reading converter String -> BigInteger must be registered",
        )
    }

    // ---------- conditional wiring ----------

    @Test
    fun default_publishesPrimaryConversionsBean() {
        runner.run { ctx ->
            val conversions = ctx.getBean(MongoCustomConversions::class.java)
            assertNotNull(conversions, "matchIfMissing=true: bean must exist with no property set")
            assertTrue(
                conversions.hasCustomWriteTarget(BigInteger::class.java, String::class.java),
                "published bean must carry the BigInteger converters",
            )
            assertTrue(
                ctx.beanFactory.getBeanDefinition("mongoCustomConversions").isPrimary,
                "@Primary must survive into the bean definition so an app's vanilla MongoCustomConversions can't win",
            )
        }
    }

    @Test
    fun explicitTrue_publishesConversionsBean() {
        runner
            .withPropertyValues("kudos.ability.docdb.mongo.big-integer-as-string=true")
            .run { ctx ->
                assertEquals(1, ctx.getBeanNamesForType(MongoCustomConversions::class.java).size)
            }
    }

    @Test
    fun explicitFalse_skipsConversionsBean() {
        runner
            .withPropertyValues("kudos.ability.docdb.mongo.big-integer-as-string=false")
            .run { ctx ->
                assertEquals(
                    0, ctx.getBeanNamesForType(MongoCustomConversions::class.java).size,
                    "big-integer-as-string=false is the documented opt-out: no conversions bean",
                )
            }
    }

    @Test
    fun missingMongoTemplateOnClasspath_backsOffEntirely() {
        runner
            .withClassLoader(FilteredClassLoader(MongoTemplate::class.java))
            .run { ctx ->
                assertFalse(
                    ctx.containsBean("mongoCustomConversions"),
                    "@ConditionalOnClass(MongoTemplate): without spring-data-mongodb the conversions bean must not register",
                )
                assertEquals(
                    0, ctx.getBeanNamesForType(MongoCustomProperties::class.java).size,
                    "the whole configuration (incl. @EnableConfigurationProperties) must back off, not just the bean method",
                )
            }
    }

    @Test
    fun properties_bindFromBundledYmlAndOverride() {
        runner.run { ctx ->
            assertTrue(
                ctx.getBean(MongoCustomProperties::class.java).bigIntegerAsString,
                "bundled kudos-ability-data-docdb-mongo.yml sets big-integer-as-string: true",
            )
        }
        runner
            .withPropertyValues("kudos.ability.docdb.mongo.big-integer-as-string=false")
            .run { ctx ->
                assertFalse(ctx.getBean(MongoCustomProperties::class.java).bigIntegerAsString)
            }
    }
}
