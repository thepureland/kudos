package io.kudos.base.i18n

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * I18nKit supplemental test cases (complements I18nKitTest).
 *
 * Targets the public incremental loader initI18nByType (uninitialized error path, the type-less
 * no-op path and the typed path), the blank-default-locale handling, and the getI18nMap
 * unsupported-locale fallback / empty branches.
 *
 * @author K
 * @since 1.0.0
 */
internal class I18nKitExtTest {

    @BeforeTest
    fun setup() {
        // a clean, well-known baseline; the type-less load reads test-resources/i18n/*.properties
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(), "zh_CN")
    }

    @AfterTest
    fun tearDown() {
        // restore the baseline other tests in this module rely on
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(), "zh_CN")
    }

    @Test
    fun initI18nByType_typeless_isNoOp() {
        // empty args -> type is "" -> the typed-load block is skipped, returns true
        assertTrue(I18nKit.initI18nByType())
        // an explicit empty type behaves the same
        assertTrue(I18nKit.initI18nByType(""))
        // data is still intact
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))
    }

    @Test
    fun initI18nByType_withType_loadsAndReturnsTrue() {
        // there is no i18n/common subdirectory in test resources; the scan yields nothing but the
        // typed branch (otherLocales handling + initI18nByType private) still executes.
        assertTrue(I18nKit.initI18nByType("common"))
        // a prefix argument exercises the args.size > 1 branch
        assertTrue(I18nKit.initI18nByType("common", "user"))
    }

    @Test
    fun getI18nMap_unsupportedLocale_fallsBackToDefault() {
        val mapDefault = I18nKit.getI18nMap() // default locale
        val mapFallback = I18nKit.getI18nMap("not_REAL") // unsupported -> falls back to default
        assertEquals(
            mapDefault[""]?.get("kudos-base-test")?.get("language.zh_CN"),
            mapFallback[""]?.get("kudos-base-test")?.get("language.zh_CN")
        )
        assertEquals("中文简体", mapFallback[""]?.get("kudos-base-test")?.get("language.zh_CN"))
    }

    @Test
    fun initI18n_blankDefaultLocale_usesZhCnButRequiresInSupportList() {
        // a blank default locale is in the support set, so it is accepted; the
        // `if (defaultLocale.isNotBlank())` guard is false, so the stored default stays "zh_CN".
        I18nKit.initI18n(setOf("", "zh_CN", "zh_TW", "en_US"), setOf(), "")
        // blank IS supported now
        assertTrue(I18nKit.isSupport(""))
    }

    @Test
    fun initI18n_defaultNotInSupport_throws() {
        assertFailsWith<IllegalStateException> {
            I18nKit.initI18n(setOf("zh_CN"), setOf(), "fr_FR")
        }
    }

    @Test
    fun initI18n_defaultLocaleArgOmitted_usesZhCn() {
        // omit the defaultLocale argument so the default value ("zh_CN") branch is exercised
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf())
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))
    }

    @Test
    fun missingKeyAndModule_fallBackToDefaultLocale() {
        // The "extra" module exists only in zh_CN (k1,k2) and en_US (k1 only); zh_TW has no file.
        // -> zh_TW must COPY the whole module from the default locale (the copy branch);
        // -> en_US must FILL the missing k2 from the default locale (the fill branch).
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(), "zh_CN")

        // en_US keeps its own k1 but inherits k2 from zh_CN
        assertEquals("value-one", I18nKit.getLocalStr("extra.k1", "extra", "", "en_US"))
        assertEquals("值二", I18nKit.getLocalStr("extra.k2", "extra", "", "en_US"))

        // zh_TW has no file for "extra" -> both keys copied from zh_CN
        assertEquals("值一", I18nKit.getLocalStr("extra.k1", "extra", "", "zh_TW"))
        assertEquals("值二", I18nKit.getLocalStr("extra.k2", "extra", "", "zh_TW"))
    }

}
