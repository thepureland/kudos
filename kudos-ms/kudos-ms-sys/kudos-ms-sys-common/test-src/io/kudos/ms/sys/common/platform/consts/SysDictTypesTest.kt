package io.kudos.ms.sys.common.platform.consts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for SysDictTypes
 *
 * Covers every dict-type constant value defined in Sys.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictTypesTest {

    @Test
    fun constants() {
        assertEquals("ds_use", SysDictTypes.DS_USE)
        assertEquals("ds_type", SysDictTypes.DS_TYPE)
        assertEquals("resource_type", SysDictTypes.RESOURCE_TYPE)
        assertEquals("cache_strategy", SysDictTypes.CACHE_STRATEGY)
        assertEquals("locale", SysDictTypes.LOCALE)
        assertEquals("i18n_type", SysDictTypes.I18N_TYPE)
        assertEquals("timezone", SysDictTypes.TIMEZONE)
        assertEquals("domain_type", SysDictTypes.DOMAIN_TYPE)
        assertEquals("terminal_type", SysDictTypes.TERMINAL_TYPE)
        assertEquals("access_rule_type", SysDictTypes.ACCESS_RULE_TYPE)
        assertEquals("ip_type", SysDictTypes.IP_TYPE)
    }

}
