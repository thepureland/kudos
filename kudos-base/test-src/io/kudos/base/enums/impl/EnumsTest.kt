package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.ienums.IModuleEnum
import kotlin.test.*

/**
 * Tests for the enum implementations under io.kudos.base.enums.impl as well as the
 * default methods of the io.kudos.base.enums.ienums interfaces.
 *
 * Covers: code/displayText accessors, extra properties (abbr/url/bool/status), companion
 * helpers (enumOf / enumOfBool / initTrans), toString overrides, and the IErrorCodeEnum
 * displayText default-getter (both the blank-prefix and non-blank-prefix branches).
 *
 * @author K
 * @since 1.0.0
 */
internal class EnumsTest {

    // ---------------- SexEnum ----------------

    @Test
    fun sexEnum_codesAndTexts() {
        assertEquals("0", SexEnum.FEMALE.code)
        assertEquals("1", SexEnum.MALE.code)
        assertEquals("9", SexEnum.SECRET.code)
        assertEquals("Female", SexEnum.FEMALE.displayText)
        assertEquals("Male", SexEnum.MALE.displayText)
        assertEquals("Undisclosed", SexEnum.SECRET.displayText)
        assertEquals(3, SexEnum.entries.size)
        // is an IDictEnum
        assertTrue(SexEnum.MALE is IDictEnum)
    }

    // ---------------- OsEnum ----------------

    @Test
    fun osEnum_hasAllExpectedValues() {
        val names = OsEnum.entries.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("MAC", "WINDOWS", "LINUX", "ANDROID", "IOS", "HARMONY",
            "FREEBSD", "OPENBSD", "NETBSD", "DRAGONFLYBSD", "SOLARIS", "ILLUMOS",
            "AIX", "HPUX", "TVOS", "WATCHOS", "OTHER")))
        assertEquals(17, OsEnum.entries.size)
        assertEquals(OsEnum.WINDOWS, OsEnum.valueOf("WINDOWS"))
    }

    // ---------------- KudosModuleEnum ----------------

    @Suppress("DEPRECATION")
    @Test
    fun kudosModuleEnum_logAudit() {
        assertEquals("log_audit", KudosModuleEnum.LOG_AUDIT.code)
        assertEquals("Log Audit", KudosModuleEnum.LOG_AUDIT.displayText)
        assertEquals(1, KudosModuleEnum.entries.size)
        assertTrue(KudosModuleEnum.LOG_AUDIT is IModuleEnum)
        assertEquals(KudosModuleEnum.LOG_AUDIT, KudosModuleEnum.valueOf("LOG_AUDIT"))
    }

    // ---------------- ProvinceEnum ----------------

    @Test
    fun provinceEnum_codeTextAbbr() {
        assertEquals("11", ProvinceEnum.BEI_JING.code)
        assertEquals("北京", ProvinceEnum.BEI_JING.displayText)
        assertEquals("京", ProvinceEnum.BEI_JING.abbr)
        // toString is overridden to return displayText
        assertEquals("北京", ProvinceEnum.BEI_JING.toString())
        assertEquals("粤", ProvinceEnum.GUANG_DONG.abbr)
        assertEquals(34, ProvinceEnum.entries.size)
    }

    @Test
    fun provinceEnum_enumOf_validCode() {
        assertEquals(ProvinceEnum.SHANG_HAI, ProvinceEnum.enumOf("31"))
        assertEquals(ProvinceEnum.AO_MEN, ProvinceEnum.enumOf("82"))
    }

    @Test
    fun provinceEnum_enumOf_invalidCode_throws() {
        val ex = assertFailsWith<IllegalArgumentException> { ProvinceEnum.enumOf("9999") }
        assertTrue(ex.message!!.contains("Invalid province code"))
    }

    @Test
    fun provinceEnum_enumOf_blankCode_throws() {
        // blank code is rejected by EnumKit.enumOf before lookup
        assertFailsWith<IllegalArgumentException> { ProvinceEnum.enumOf("  ") }
    }

    // ---------------- YesNotEnum ----------------

    @Test
    fun yesNotEnum_basicProperties() {
        assertTrue(YesNotEnum.YES.bool)
        assertFalse(YesNotEnum.NOT.bool)
        assertEquals("1", YesNotEnum.YES.code)
        assertEquals("0", YesNotEnum.NOT.code)
        assertEquals("yes_not", YesNotEnum.CODE_TABLE_ID)
    }

    @Test
    fun yesNotEnum_enumOf_and_enumOfBool() {
        assertEquals(YesNotEnum.YES, YesNotEnum.enumOf("true"))
        assertEquals(YesNotEnum.NOT, YesNotEnum.enumOf("false"))
        // any non-"true" string maps to false per Kotlin's String.toBoolean
        assertEquals(YesNotEnum.NOT, YesNotEnum.enumOf("anything"))
        assertEquals(YesNotEnum.YES, YesNotEnum.enumOfBool(true))
        assertEquals(YesNotEnum.NOT, YesNotEnum.enumOfBool(false))
    }

    @Test
    fun yesNotEnum_initTrans_overridesDisplayTextThenRestores() {
        val originalYes = YesNotEnum.YES.displayText
        val originalNot = YesNotEnum.NOT.displayText
        try {
            // map provides translation for "1" only; "0" should keep its original text (?: branch)
            YesNotEnum.initTrans(mapOf("1" to "是"))
            assertEquals("是", YesNotEnum.YES.displayText)
            assertEquals(originalNot, YesNotEnum.NOT.displayText)

            // full map updates both
            YesNotEnum.initTrans(mapOf("1" to "Y", "0" to "N"))
            assertEquals("Y", YesNotEnum.YES.displayText)
            assertEquals("N", YesNotEnum.NOT.displayText)
        } finally {
            // restore to avoid leaking mutated state to other tests
            YesNotEnum.initTrans(mapOf("1" to originalYes, "0" to originalNot))
        }
        assertEquals(originalYes, YesNotEnum.YES.displayText)
        assertEquals(originalNot, YesNotEnum.NOT.displayText)
    }

    // ---------------- CommonErrorCodeEnum ----------------

    @Test
    fun commonErrorCodeEnum_codeAndDisplayTextUsesI18nPrefix() {
        assertEquals("200", CommonErrorCodeEnum.SUCCESS.code)
        assertEquals("Operation succeeded", CommonErrorCodeEnum.SUCCESS.defaultDisplayText)
        // i18nKeyPrefix is non-blank, so displayText is "<prefix>.<code>"
        assertEquals("sys.error-msg.default", CommonErrorCodeEnum.SUCCESS.i18nKeyPrefix)
        assertEquals("sys.error-msg.default.200", CommonErrorCodeEnum.SUCCESS.displayText)
        assertEquals("sys.error-msg.default.4003", CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE.displayText)
        assertEquals(10, CommonErrorCodeEnum.entries.size)
    }

    // ---------------- ErrorStatusEnum ----------------

    @Test
    fun errorStatusEnum_propertiesAndBlankPrefixBranch() {
        assertEquals("403", ErrorStatusEnum.SC_FORBIDDEN.code)
        assertEquals("/errors/403", ErrorStatusEnum.SC_FORBIDDEN.url)
        assertEquals(403, ErrorStatusEnum.SC_FORBIDDEN.status)
        // blank i18nKeyPrefix => displayText falls back to defaultDisplayText
        assertEquals("", ErrorStatusEnum.SC_FORBIDDEN.i18nKeyPrefix)
        assertEquals("SC_FORBIDDEN", ErrorStatusEnum.SC_FORBIDDEN.displayText)
        assertEquals(600, ErrorStatusEnum.SC_SESSION_EXPIRE.status)
        // an entry with empty url
        assertEquals("", ErrorStatusEnum.SC_PRIVILEGE.url)
        assertEquals(15, ErrorStatusEnum.entries.size)
        // every status is parseable to its int code
        ErrorStatusEnum.entries.forEach { assertEquals(it.code.toInt(), it.status) }
    }

    // ---------------- IErrorCodeEnum default getter, both branches ----------------

    private data class FakeErrorCode(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String
    ) : IErrorCodeEnum

    @Test
    fun iErrorCodeEnum_displayText_blankPrefixUsesDefault() {
        val ec = FakeErrorCode("E1", "fallback text", "")
        assertEquals("fallback text", ec.displayText)
    }

    @Test
    fun iErrorCodeEnum_displayText_nonBlankPrefixBuildsKey() {
        val ec = FakeErrorCode("E2", "ignored", "my.prefix")
        assertEquals("my.prefix.E2", ec.displayText)
    }
}
