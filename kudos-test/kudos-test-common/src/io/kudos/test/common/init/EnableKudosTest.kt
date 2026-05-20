package io.kudos.test.common.init

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited
import kotlin.reflect.KClass


/**
 * Kudos单元测试使能注解
 *
 * 添加于测试用例类上，为其构建必要的运行上下文。
 * 要排除特定的 IComponentInitializer，请在 [TestApplication] 子类或自定义启动类上用
 * `@EnableKudos(exclusions = [...])`，本注解仅负责 Spring Boot Test 的上下文装配。
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
     * 设置SpringBootTest注解的classes值，用来指定测试的入口应用类
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "classes")
    val classes: Array<KClass<*>> = [TestApplication::class],

    /**
     * 设置SpringBootTest注解的webEnvironment值，用来指定测试web环境类型
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "webEnvironment")
    val webEnvironment: WebEnvironment = WebEnvironment.MOCK,

    /**
     * 设置SpringBootTest注解的properties值，用来定义properties的key-value
     *
     * @author K
     * @since 1.0.0
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
