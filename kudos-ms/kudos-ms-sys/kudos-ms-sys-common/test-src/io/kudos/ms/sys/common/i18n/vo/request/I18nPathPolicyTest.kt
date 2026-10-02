package io.kudos.ms.sys.common.i18n.vo.request

import jakarta.validation.Validation
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class I18nPathPolicyTest {
    @Test
    fun reservedSegmentsAreRejectedAtEveryDepth() {
        listOf("__proto__", "constructor", "prototype", "a.__proto__.b", "a.constructor.b", "a.prototype", "safe\n.__proto__.x", "a..b", "")
            .forEach { path -> assertFailsWith<IllegalArgumentException>(path) { I18nPathPolicy.requireSafe(path) } }
        listOf("columns.name", "sys.error-msg.default.200", "common", "构造函数").forEach(I18nPathPolicy::requireSafe)
    }

    @Test
    fun formsRejectReservedKeysNamespacesTypesAndServices() {
        Validation.buildDefaultValidatorFactory().use { factory ->
            val validator = factory.validator
            val form = SysI18nFormCreate("zh_CN", "sys", "ui", "common", "columns.name", "Name", null)
            listOf("key", "namespace", "i18nTypeDictCode", "atomicServiceCode").forEach { property ->
                assertTrue(validator.validateValue(SysI18nFormCreate::class.java, property, "a.__proto__.b").isNotEmpty())
                assertTrue(validator.validateValue(SysI18nFormCreate::class.java, property, "safe\n.__proto__.x").isNotEmpty())
            }
            assertTrue(validator.validateProperty(form, "key").isEmpty())
        }
    }
}
