package io.kudos.base.io

import io.kudos.base.bean.Person
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.logger.ILogCreator
import io.kudos.base.logger.slf4j.Slf4jLoggerCreator
import jakarta.xml.bind.annotation.XmlRootElement
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis
import kotlin.test.Test


/**
 * ScanKit test cases.
 *
 * @author K
 * @since 1.0.0
 */
internal class ScanKitTest {

    @Test
    fun findClassesWithAnnotation() {
        val basePackage = "io.kudos.base"
        val annoClass = XmlRootElement::class
        var classes: List<KClass<*>>
        val timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesWithAnnotation(basePackage, annoClass)
        }
        println("findClassesWithAnnotation-1 took ${timeMillis}ms")
        assert(classes.contains(Person::class))
    }

    @Test
    fun findSubclassesOf() {
        // class
        var basePackage = "io.kudos.base"
        val clazz = RuntimeException::class
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findSubclassesOf(basePackage, clazz)
        }
        println("findSubclassesOf-1 took ${timeMillis}ms")
        assert(classes.contains(ObjectNotFoundException::class))

        // interface, should not pass
        basePackage = "io.kudos.base"
        val iClazz = ILogCreator::class
        timeMillis = measureTimeMillis {
            classes = ScanKit.findSubclassesOf(basePackage, iClazz)
        }
        println("findSubclassesOf-2 took ${timeMillis}ms")
        assert(!classes.contains(Slf4jLoggerCreator::class))
    }

    @Test
    fun findImplementations() {
        // interface
        var basePackage = "io.kudos.base"
        val iClazz = ILogCreator::class
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findImplementations(basePackage, iClazz)
        }
        println("findImplementations-1 took ${timeMillis}ms")
        assert(classes.contains(Slf4jLoggerCreator::class))

        // class, should not pass
        basePackage = "io.kudos.base"
        val clazz = RuntimeException::class
        timeMillis = measureTimeMillis {
            classes = ScanKit.findImplementations(basePackage, clazz)
        }
        println("findImplementations-2 took ${timeMillis}ms")
        assert(!classes.contains(ObjectNotFoundException::class))
    }

    @Test
    fun findClassesMatching() {
        // recursive
        val basePackage = "io.kudos.base"
        val pattern = "V.+Kit"
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesMatching(basePackage, pattern)
        }
        println("findClassesMatching-1 took ${timeMillis}ms")
        assert(classes.contains(ValidationKit::class))

        // non-recursive
        timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesMatching(basePackage, pattern, false)
        }
        println("findClassesMatching-2 took ${timeMillis}ms")
        assert(!classes.contains(ValidationKit::class))
    }

    @Test
    fun findResourcesMatching() {
        var paths: List<String>
        val timeMillis = measureTimeMillis {
            paths = ScanKit.findResourcesMatching("", "logo.*")
        }
        println("findResourcesMatching took ${timeMillis}ms")
        assert(paths.any { it.endsWith(".png") })
    }
}
