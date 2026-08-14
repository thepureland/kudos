package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DatasourceKeyTool]. Pure string processing — covers separator filling,
 * null/blank inputs, single-segment keys, trailing-empty-segment trimming and the
 * readonly-suffix check.
 *
 * @author K
 * @since 1.0.0
 */
internal class DatasourceKeyToolTest {

    @Test
    fun convertCacheMapKey_fillsDefaultServerCodeWhenNoSeparator() {
        assertEquals(
            "ds1::default::t1::master",
            DatasourceKeyTool.convertCacheMapKey("ds1", "t1", DatasourceConst.MODE_MASTER)
        )
    }

    @Test
    fun convertCacheMapKey_keepsConfigAsIsWhenSeparatorPresent() {
        assertEquals(
            "_context::billing::t1::readonly",
            DatasourceKeyTool.convertCacheMapKey("_context::billing", "t1", DatasourceConst.MODE_READONLY)
        )
    }

    @Test
    fun convertCacheMapKey_rendersNullTenantAndSuffixAsLiteralNull() {
        // joinToString renders nulls as the string "null" — documents the actual key shape so a
        // change in behavior (e.g. filtering nulls) is caught.
        assertEquals("ds1::default::null::null", DatasourceKeyTool.convertCacheMapKey("ds1", null, null))
        assertEquals("a::b::null::null", DatasourceKeyTool.convertCacheMapKey("a::b", null, null))
    }

    @Test
    fun getServerCode_blankInputsReturnEmptyString() {
        assertEquals("", DatasourceKeyTool.getServerCode(null))
        assertEquals("", DatasourceKeyTool.getServerCode(""))
        assertEquals("", DatasourceKeyTool.getServerCode("   "))
    }

    @Test
    fun getServerCode_singleSegmentReturnsNull() {
        assertNull(DatasourceKeyTool.getServerCode("master"), "no separator means 'use the default service'")
    }

    @Test
    fun getServerCode_returnsSecondSegment() {
        assertEquals("billing", DatasourceKeyTool.getServerCode("_context::billing::t1::master"))
        assertEquals("svc", DatasourceKeyTool.getServerCode("ds::svc"))
    }

    @Test
    fun getServerCode_dropsTrailingEmptySegments() {
        // "ds1::::" -> after dropLastWhile only ["ds1"] remains -> single segment -> null
        assertNull(DatasourceKeyTool.getServerCode("ds1::::"))
    }

    @Test
    fun getSuffix_blankInputsReturnEmptyString() {
        assertEquals("", DatasourceKeyTool.getSuffix(null))
        assertEquals("", DatasourceKeyTool.getSuffix(""))
        assertEquals("", DatasourceKeyTool.getSuffix("  "))
    }

    @Test
    fun getSuffix_returnsLastSegment() {
        assertEquals("master", DatasourceKeyTool.getSuffix("_context::billing::t1::master"))
        assertEquals("readonly", DatasourceKeyTool.getSuffix("ds1::default::t1::readonly"))
        assertEquals("only", DatasourceKeyTool.getSuffix("only"))
    }

    @Test
    fun getSuffix_allSeparatorsReturnsEmptyString() {
        // every segment empty -> dropLastWhile removes all -> lastOrNull is null -> ""
        assertEquals("", DatasourceKeyTool.getSuffix("::::"))
    }

    @Test
    fun isReadOnly_checksLastSegmentNotStringSuffix() {
        assertTrue(DatasourceKeyTool.isReadOnly("ds1::default::t1::readonly"))
        assertTrue(DatasourceKeyTool.isReadOnly("readonly"))
        assertFalse(DatasourceKeyTool.isReadOnly("ds1::default::t1::master"))
        assertFalse(DatasourceKeyTool.isReadOnly(""))
        // keys merely *ending with the word* readonly are ordinary data source keys
        assertFalse(DatasourceKeyTool.isReadOnly("my_readonly"))
        assertFalse(DatasourceKeyTool.isReadOnly("ds1::default::t1::my_readonly"))
    }

    @Test
    fun serverCodeDefaultConstant() {
        assertEquals("default", DatasourceKeyTool.SERVER_CODE_DEFAULT)
    }
}
