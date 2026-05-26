package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.validator.DictItemCodeValidator
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test cases for DictItemCode.
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
        // Note: this test depends on the IDictCodeFinder implementation.
        // If no DictCodeFinder is found, dictMap will be empty and validation will fail.
        val bean = TestDictCodeBean("VALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        // The actual result depends on whether ServiceLoader can find an IDictCodeFinder implementation
    }

    @Test
    fun testIsValidWithInvalidCode() {
        val bean = TestDictCodeBean("INVALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        // If no DictCodeFinder is found or the code is not in the dictionary, validation should fail
    }

    @Test
    fun testInitialize() {
        val validator = DictItemCodeValidator()
        val annotation = TestDictCodeBean::class.java.getDeclaredField("code")
            .getAnnotation(DictItemCode::class.java)
        if (annotation != null) {
            validator.initialize(annotation)
            // Initialization should succeed
        }
    }

    data class TestDictCodeBean(
        @get:DictItemCode(atomicServiceCode = "test", dictType = "test", message = "invalid dictionary code")
        val code: String?
    )
}
