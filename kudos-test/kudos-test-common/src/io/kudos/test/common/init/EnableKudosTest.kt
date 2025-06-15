package io.kudos.test.common.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited
import kotlin.reflect.KClass


/**
 * Kudos单元测试使能注解
 *
 * 添加于测试用例类上，为其构建必要的运行上下文
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@SpringBootTest
//@ImportAutoConfiguration(ComponentInitializerSelector::class)
//@SpringBootConfiguration
//@EnableAutoConfiguration
annotation class EnableKudosTest(

    /**
     * 要排除的初始化器类
     *
     * @author K
     * @since 1.0.0
     */
    val excludeInitializers: Array<KClass<out IComponentInitializer>> = [],

    /**
     * 要排除的其他Configuration类(除了实现IComponentInitializer接口的)
     *
     * @author K
     * @since 1.0.0
     */
//    @get:AliasFor(annotation = EnableAutoConfiguration::class, attribute = "exclude")
//    val excludeOtherConfigurations: Array<KClass<*>> = [],

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
