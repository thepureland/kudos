package io.kudos.test.common.init

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited
import kotlin.reflect.KClass


/**
 * Annotation that enables Kudos unit tests.
 *
 * Applied to test case classes to build the runtime context they need.
 * To exclude specific IComponentInitializer instances, use `@EnableKudos(exclusions = [...])` on a
 * [TestApplication] subclass or custom bootstrap class; this annotation only wires up the Spring Boot
 * Test context.
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@SpringBootTest
annotation class EnableKudosTest(

    /**
     * Sets the SpringBootTest annotation's classes value, used to specify the entry application class for the test.
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "classes")
    val classes: Array<KClass<*>> = [TestApplication::class],

    /**
     * Sets the SpringBootTest annotation's webEnvironment value, used to specify the test web environment type.
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "webEnvironment")
    val webEnvironment: WebEnvironment = WebEnvironment.MOCK,

    /**
     * Sets the SpringBootTest annotation's properties value, used to define properties key-value pairs.
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
