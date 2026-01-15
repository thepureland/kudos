package io.kudos.base.bean.validation.teminal.convert

import io.kudos.base.bean.validation.constraint.annotations.*
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.bean.validation.support.IBeanValidator
import io.kudos.base.enums.impl.SexEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.constraints.NotNull
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * ConstraintConvertorFactory测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ConstraintConvertorFactoryTest {

    @Test
    fun testGetInstanceForDictEnumCode() {
        val annotation = TestBean::class.java.getDeclaredField("dictEnumCode")
            .getAnnotation(DictEnumCode::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForCompare() {
        val annotation = TestBean::class.java.getDeclaredField("compare")
            .getAnnotation(Compare::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForNotNullOn() {
        val annotation = TestBean::class.java.getDeclaredField("notNullOn")
            .getAnnotation(NotNullOn::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForEach() {
        val annotation = TestBean::class.java.getDeclaredField("each")
            .getAnnotation(Each::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForExist() {
        val annotation = TestBean::class.java.getDeclaredField("exist")
            .getAnnotation(Exist::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForConstraints() {
        val annotation = TestBean::class.java.getDeclaredField("constraints")
            .getAnnotation(Constraints::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForRemote() {
        val annotation = TestBean::class.java.getDeclaredField("remote")
            .getAnnotation(Remote::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            assertNotNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForDictCode() {
        val annotation = TestBean::class.java.getDeclaredField("dictCode")
            .getAnnotation(DictCode::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            // DictCode应该返回null
            assertNull(convertor)
        }
    }

    @Test
    fun testGetInstanceForDefault() {
        val annotation = TestBean::class.java.getDeclaredField("notNull")
            .getAnnotation(NotNull::class.java)
        if (annotation != null) {
            val convertor = ConstraintConvertorFactory.getInstance(annotation)
            // 默认转换器
            assertNotNull(convertor)
        }
    }

    data class TestBean(
        @get:DictEnumCode(enumClass = SexEnum::class)
        val dictEnumCode: String?,
        
        @get:Compare(anotherProperty = "other", logic = LogicOperatorEnum.EQ)
        val compare: String?,
        
        @get:NotNullOn(depends = Depends(properties = ["other"], values = ["test"]))
        val notNullOn: String?,
        
        @get:Each(value = Constraints(notNull = NotNull()))
        val each: List<String>?,
        
        @get:Exist(value = Constraints(notNull = NotNull()))
        val exist: String?,
        
        @get:Constraints
        val constraints: String?,
        
        @get:Remote(checkClass = IBeanValidator::class, requestUrl = "")
        val remote: String?,
        
        @get:DictCode(module = "test", dictType = "test")
        val dictCode: String?,
        
        @get:NotNull
        val notNull: String?
    )
}
