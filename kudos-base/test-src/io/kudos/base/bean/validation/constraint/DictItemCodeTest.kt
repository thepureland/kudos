package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.validator.DictItemCodeValidator
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.ConstraintValidatorContext
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for DictItemCode.
 *
 * The dictionary codes are provided deterministically by [TestDictItemCodeFinder],
 * registered through the ServiceLoader SPI in test-resources/META-INF/services.
 *
 * Coverage:
 * - null / empty / blank values pass directly.
 * - A code present in the dictionary passes; an absent code fails with the constraint message.
 * - initialize accepts the annotation read from the property getter.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DictItemCodeTest {

    @Test
    fun testIsValidWithNull() {
        val bean = TestDictCodeBean(null)
        val violations = ValidationKit.validateBean(bean)
        // null values should pass validation (per the implementation, isNullOrBlank returns true)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithEmpty() {
        val bean = TestDictCodeBean("")
        val violations = ValidationKit.validateBean(bean)
        // Empty strings should pass validation
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithBlank() {
        val bean = TestDictCodeBean("   ")
        val violations = ValidationKit.validateBean(bean)
        // Blank strings should pass validation
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithValidCode() {
        // TestDictItemCodeFinder returns VALID_CODE for module "test" / dictType "test"
        val bean = TestDictCodeBean("VALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithInvalidCode() {
        val bean = TestDictCodeBean("INVALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        assertFalse(violations.isEmpty())
        assertEquals("invalid dictionary code", violations.first().message)
    }

    /**
     * When no IDictItemCodeFinder SPI implementation is visible on the context
     * classloader, the valid code set is empty and any non-blank value fails.
     */
    @Test
    fun testIsValidWithoutAnyFinder() {
        val validator = DictItemCodeValidator()
        val annotation = TestDictCodeBean::class.java.getMethod("getCode")
            .getAnnotation(DictItemCode::class.java)
        assertNotNull(annotation)
        validator.initialize(annotation)
        val context = Proxy.newProxyInstance(
            ConstraintValidatorContext::class.java.classLoader,
            arrayOf(ConstraintValidatorContext::class.java)
        ) { _, _, _ -> null } as ConstraintValidatorContext

        val original = Thread.currentThread().contextClassLoader
        try {
            // The platform classloader cannot see the test SPI registration
            Thread.currentThread().contextClassLoader = ClassLoader.getPlatformClassLoader()
            assertFalse(validator.isValid("VALID_CODE", context))
        } finally {
            Thread.currentThread().contextClassLoader = original
        }
    }

    @Test
    fun testInitialize() {
        val validator = DictItemCodeValidator()
        // The constraint is declared on the getter (@get:), so read it from the method
        val annotation = TestDictCodeBean::class.java.getMethod("getCode")
            .getAnnotation(DictItemCode::class.java)
        assertNotNull(annotation)
        validator.initialize(annotation)
    }

    data class TestDictCodeBean(
        @get:DictItemCode(atomicServiceCode = "test", dictType = "test", message = "invalid dictionary code")
        val code: String?
    )
}
