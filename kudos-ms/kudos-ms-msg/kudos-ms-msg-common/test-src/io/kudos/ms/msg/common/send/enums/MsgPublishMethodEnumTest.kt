package io.kudos.ms.msg.common.send.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for MsgPublishMethodEnum
 *
 * Covers dictCode of every entry, the derived listenerType ("msg.dispatch." + dictCode),
 * uniqueness of dict codes and the fromDictCode lookup (hit / miss / case sensitivity).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgPublishMethodEnumTest {

    @Test
    fun dictCodes() {
        assertEquals("email", MsgPublishMethodEnum.EMAIL.dictCode)
        assertEquals("sms", MsgPublishMethodEnum.SMS.dictCode)
        assertEquals("siteMsg", MsgPublishMethodEnum.SITE_MSG.dictCode)
        assertEquals("all_user", MsgPublishMethodEnum.ALL_USER.dictCode)
        assertEquals(4, MsgPublishMethodEnum.entries.size)
    }

    @Test
    fun dictCodesAreUnique() {
        val codes = MsgPublishMethodEnum.entries.map { it.dictCode }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun listenerTypeDerivesFromDictCode() {
        assertEquals("msg.dispatch.email", MsgPublishMethodEnum.EMAIL.listenerType)
        assertEquals("msg.dispatch.sms", MsgPublishMethodEnum.SMS.listenerType)
        assertEquals("msg.dispatch.siteMsg", MsgPublishMethodEnum.SITE_MSG.listenerType)
        assertEquals("msg.dispatch.all_user", MsgPublishMethodEnum.ALL_USER.listenerType)
        MsgPublishMethodEnum.entries.forEach {
            assertEquals("msg.dispatch.${it.dictCode}", it.listenerType)
        }
    }

    @Test
    fun fromDictCodeHit() {
        assertEquals(MsgPublishMethodEnum.EMAIL, MsgPublishMethodEnum.fromDictCode("email"))
        assertEquals(MsgPublishMethodEnum.SMS, MsgPublishMethodEnum.fromDictCode("sms"))
        assertEquals(MsgPublishMethodEnum.SITE_MSG, MsgPublishMethodEnum.fromDictCode("siteMsg"))
        assertEquals(MsgPublishMethodEnum.ALL_USER, MsgPublishMethodEnum.fromDictCode("all_user"))
    }

    @Test
    fun fromDictCodeMissReturnsNull() {
        assertNull(MsgPublishMethodEnum.fromDictCode("unknown"))
        assertNull(MsgPublishMethodEnum.fromDictCode(""))
        // case sensitive: dict codes are lower-camel, an upper-cased value must not match
        assertNull(MsgPublishMethodEnum.fromDictCode("EMAIL"))
        assertNull(MsgPublishMethodEnum.fromDictCode("sitemsg"))
    }

}
