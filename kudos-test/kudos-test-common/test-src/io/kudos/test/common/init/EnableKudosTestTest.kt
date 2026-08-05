package io.kudos.test.common.init

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EnableKudosTest].
 *
 * Covers the annotation's default attribute values, its meta-annotations (Target/Retention/MustBeDocumented/
 * Inherited/SpringBootTest), and the [AliasFor] wiring on each attribute so that classes/webEnvironment/properties
 * are aliased onto the underlying [SpringBootTest] annotation. These are reflection-only assertions; no Spring
 * container is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class EnableKudosTestTest {

    @Test
    fun defaultAttributeValues() {
        // An instance produced via reflection on a usage carries the defaults.
        @EnableKudosTest
        class Sample

        val ann = Sample::class.annotations.filterIsInstance<EnableKudosTest>().single()
        assertEquals(WebEnvironment.MOCK, ann.webEnvironment)
        assertEquals(0, ann.properties.size)
        assertEquals(listOf(TestApplication::class), ann.classes.toList())
    }

    @Test
    fun explicitAttributeValues() {
        @EnableKudosTest(
            classes = [TestApplication::class, EnableKudosTestTest::class],
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = ["a=1", "b=2"]
        )
        class Sample

        val ann = Sample::class.annotations.filterIsInstance<EnableKudosTest>().single()
        assertEquals(WebEnvironment.RANDOM_PORT, ann.webEnvironment)
        assertEquals(listOf("a=1", "b=2"), ann.properties.toList())
        assertEquals(listOf(TestApplication::class, EnableKudosTestTest::class), ann.classes.toList())
    }

    @Test
    fun metaAnnotationsPresent() {
        val kClass = EnableKudosTest::class
        assertTrue(kClass.hasAnnotation<MustBeDocumented>(), "should be @MustBeDocumented")
        assertTrue(kClass.hasAnnotation<SpringBootTest>(), "should be meta-annotated with @SpringBootTest")

        val javaClass = EnableKudosTest::class.java
        assertNotNull(
            javaClass.getAnnotation(Inherited::class.java),
            "should be @Inherited"
        )
        assertNotNull(
            javaClass.getAnnotation(Retention::class.java),
            "should declare @Retention"
        )
        assertEquals(AnnotationRetention.RUNTIME, kClass.findAnnotation<Retention>()!!.value)

        val target = kClass.findAnnotation<Target>()
        assertNotNull(target)
        assertEquals(listOf(AnnotationTarget.CLASS), target.allowedTargets.toList())
    }

    @Test
    fun aliasForWiringOnEachAttribute() {
        val expected = mapOf(
            "classes" to "classes",
            "webEnvironment" to "webEnvironment",
            "properties" to "properties"
        )
        expected.forEach { (member, springAttr) ->
            val getter = EnableKudosTest::class.java.getMethod(member)
            val aliasFor = getter.getAnnotation(AliasFor::class.java)
            assertNotNull(aliasFor, "attribute '$member' should carry @AliasFor")
            assertEquals(
                SpringBootTest::class.java, aliasFor.annotation.java,
                "attribute '$member' should alias onto SpringBootTest"
            )
            assertEquals(
                springAttr, aliasFor.attribute,
                "attribute '$member' should alias onto SpringBootTest.$springAttr"
            )
        }
    }
}
