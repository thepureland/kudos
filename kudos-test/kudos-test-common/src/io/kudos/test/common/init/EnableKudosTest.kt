package io.kudos.test.common.init

import io.kudos.test.common.TestApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.*
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
annotation class EnableKudosTest(

    @get:AliasFor(annotation = SpringBootTest::class, attribute = "classes")
    val classes: Array<KClass<*>> = [TestApplication::class],

    @get:AliasFor(annotation = SpringBootTest::class, attribute = "webEnvironment")
    val webEnvironment: WebEnvironment = WebEnvironment.MOCK

)
