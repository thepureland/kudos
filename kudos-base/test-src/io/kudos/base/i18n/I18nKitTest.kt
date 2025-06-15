package io.kudos.base.i18n

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse


/**
 * I18nKit测试用例
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

        // 默认Locale
        i18nMap = I18nKit.getI18nMap()
        assertEquals("中文简体", i18nMap[""]!!["kudos-base-test"]!!["language.zh_CN"])

        // 不支持的Locale，按默认Locale处理
        i18nMap = I18nKit.getI18nMap("zh_cn")
        assertEquals("中文简体", i18nMap[""]!!["kudos-base-test"]!!["language.zh_CN"])
    }

    @Test
    fun getLocalStr() {
        assertEquals("中文簡體", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_TW"))

        // 默认Locale
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))

        // 找不到的情况
        assertEquals("xxx", I18nKit.getLocalStr("xxx", "kudos-base-test", "", "zh_TW"))
        assertEquals("language.zh_CN", I18nKit.getLocalStr("language.zh_CN", "xxx", "", "zh_TW"))
        assertEquals("language.zh_CN", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "xxx", "zh_TW"))
        assertEquals("xxx", I18nKit.getLocalStr("xxx", "xx", "", "zh_TW"))

        // 不支持的Locale，按默认Locale处理
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_cn"))
    }

    @Test
    fun isSupport() {
        assert(I18nKit.isSupport("zh_CN"))
        assertFalse(I18nKit.isSupport("zh_cn"))
    }

    @Test
    fun initI18n() {
        // 默认Locale不在支持的列表中，初始化失败 (相当于未调用初始化方法initI18n，以默认的zh_CN处理)
        assertFailsWith<IllegalStateException> { I18nKit.initI18n(setOf(), setOf(""), "en_US") }
        assertEquals("中文简体", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", "", "zh_TW"))

        // 重新初始化
        I18nKit.initI18n(setOf("zh_CN", "zh_TW", "en_US"), setOf(""), "en_US")
        assertEquals("Simplified Chinese", I18nKit.getLocalStr("language.zh_CN", "kudos-base-test", ""))
    }

}