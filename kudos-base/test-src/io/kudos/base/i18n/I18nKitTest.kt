package io.kudos.base.i18n

import kotlin.test.*


/**
 * I18nKit test cases
 *
 * @author K
 * @since 1.0.0
 */
internal class I18nKitTest {

    @BeforeTest
    fun setup() {
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(), "zh_CN")
    }

    @Test
    fun getI18nMap() {
        var i18nMap = I18nKit.getI18nMap("zh_TW")
        assertEquals("中文簡體", i18nMap[""]!!["kudos-base-test"]!!["language.zh_CN"])

        // Default Locale
        i18nMap = I18nKit.getI18nMap()
        assertEquals("中文简体", i18nMap[""]!!["kudos-base-test"]!!["language.zh_CN"])

        // Unsupported Locale falls back to the default Locale
        i18nMap = I18nKit.getI18nMap("zh_cn")
        assertEquals("中文简体", i18nMap[""]!!["kudos-base-test"]!!["language.zh_CN"])
    }

    @Test
    fun getLocalStr() {
        assertEquals("中文簡體", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_TW"))

        // Default Locale
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))

        // Cases where the key cannot be found
        assertEquals("xxx", I18nKit.getLocalStr("xxx", "kudos-base-test", "", "zh_TW"))
        assertEquals("language.zh_CN", I18nKit.getLocalStr("language.zh_CN", "xxx", "", "zh_TW"))
        assertEquals("language.zh_CN", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "xxx", "zh_TW"))
        assertEquals("xxx", I18nKit.getLocalStr("xxx", "xx", "", "zh_TW"))

        // Unsupported Locale falls back to the default Locale
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_cn"))
    }

    @Test
    fun isSupport() {
        assert(I18nKit.isSupport("zh_CN"))
        assertFalse(I18nKit.isSupport("zh_cn"))
    }

    @Test
    fun initI18n() {
        // The default Locale is not in the supported list; initialization fails (effectively as if initI18n was never called, defaulting to zh_CN)
        assertFailsWith<IllegalStateException> { I18nKit.initI18n(setOf(), setOf(""), "en_US") }
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_TW"))

        // Re-initialize
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(""), "en_US")
        assertEquals("Simplified Chinese", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))
    }

}