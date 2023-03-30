package io.kudos.base.io

import io.kudos.base.bean.Person
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.logger.ILoggerCreator
import io.kudos.base.logger.slf4j.Slf4jLoggerCreator
import jakarta.xml.bind.annotation.XmlRootElement
import org.junit.jupiter.api.Test
import org.soul.base.bean.validation.constraint.annotaions.AtLeast
import org.soul.base.exception.CustomRuntimeException
import org.soul.base.exception.ObjectNotFoundException
import javax.validation.Constraint
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis


/**
 * ScanKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class ScanKitTest {

    @Test
    fun findClassesWithAnnotation() {
        // 当前jar中就有
        var basePackage = "io.kudos.base"
        val annoClass = XmlRootElement::class
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesWithAnnotation(basePackage, annoClass)
        }
        println("findClassesWithAnnotation-1耗时${timeMillis}ms")
        assert(classes.contains(Person::class))

        // 依赖的jar中的
        basePackage = "org.soul.base"
        val annoClazz = Constraint::class
        timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesWithAnnotation(basePackage, annoClazz)
        }
        println("findClassesWithAnnotation-2耗时${timeMillis}ms")
        assert(classes.contains(AtLeast::class))
    }

    @Test
    fun findSubclassesOf() {
        // 类
        var basePackage = "org.soul.base"
        val clazz = CustomRuntimeException::class
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findSubclassesOf(basePackage, clazz)
        }
        println("findSubclassesOf-1耗时${timeMillis}ms")
        assert(classes.contains(ObjectNotFoundException::class))

        // 接口,不通过
        basePackage = "io.kudos.base"
        val iClazz = ILoggerCreator::class
        timeMillis = measureTimeMillis {
            classes = ScanKit.findSubclassesOf(basePackage, iClazz)
        }
        println("findSubclassesOf-2耗时${timeMillis}ms")
        assert(!classes.contains(Slf4jLoggerCreator::class))
    }

    @Test
    fun findImplementations() {
        // 接口
        var basePackage = "io.kudos.base"
        val iClazz = ILoggerCreator::class
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findImplementations(basePackage, iClazz)
        }
        println("findImplementations-1耗时${timeMillis}ms")
        assert(classes.contains(Slf4jLoggerCreator::class))

        // 类,不通过
        basePackage = "org.soul.base"
        val clazz = CustomRuntimeException::class
        timeMillis = measureTimeMillis {
            classes = ScanKit.findImplementations(basePackage, clazz)
        }
        println("findImplementations-2耗时${timeMillis}ms")
        assert(!classes.contains(ObjectNotFoundException::class))
    }

    @Test
    fun findClassesMatching() {
        // 递归
        val basePackage = "io.kudos.base"
        val pattern = "V.+Kit"
        var classes: List<KClass<*>>
        var timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesMatching(basePackage, pattern)
        }
        println("findClassesMatching-1耗时${timeMillis}ms")
        assert(classes.contains(ValidationKit::class))

        // 不递归
        timeMillis = measureTimeMillis {
            classes = ScanKit.findClassesMatching(basePackage, pattern, false)
        }
        println("findClassesMatching-2耗时${timeMillis}ms")
        assert(!classes.contains(ValidationKit::class))
    }

    @Test
    fun findResourcesMatching() {
        var paths: List<String>
        val timeMillis = measureTimeMillis {
            paths = ScanKit.findResourcesMatching("", "logo.*")
        }
        println("findResourcesMatching耗时${timeMillis}ms")
        assert(paths.any { it.endsWith(".png") })
    }
}