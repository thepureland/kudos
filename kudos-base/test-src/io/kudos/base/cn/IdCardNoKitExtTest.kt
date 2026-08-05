package io.kudos.base.cn

import io.kudos.base.enums.impl.SexEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * IdCardNoKit supplemental test cases (complements IdCardNoKitTest).
 *
 * Targets the non-numeric / invalid-length branches, the Taiwan check-digit and invalid-letter
 * branches, the Hong Kong 2-letter-prefix and trailing-'A' branches, the male-sex branch and the
 * valiDate boundary branches (year/month/day out of range, February leap-year handling).
 *
 * @author K
 * @since 1.0.0
 */
internal class IdCardNoKitExtTest {

    private val MAIN_M_18 = "210502198412020911" // a male variant differing only in the sequence digit

    @Test
    fun convert15To18_invalidInputs() {
        // wrong length
        assertNull(IdCardNoKit.convert15To18("123"))
        // correct length but non-numeric
        assertNull(IdCardNoKit.convert15To18("21050284120209X"))
    }

    @Test
    fun isIdCardNo18_nonNumericPrefix() {
        // 18 chars, but the leading 17 are not all digits
        assertFalse(IdCardNoKit.isIdCardNo18("A10502198412020944"))
        // 18 chars, numeric but wrong check digit
        assertFalse(IdCardNoKit.isIdCardNo18("210502198412020945"))
    }

    @Test
    fun isIdCardNo15_nonNumeric() {
        assertFalse(IdCardNoKit.isIdCardNo15("21050284120209X"))
    }

    @Test
    fun isTwIdCardNo_invalidLeadingLetterAndCheckDigit() {
        // matches regex but with a wrong check digit
        assertFalse(IdCardNoKit.isTwIdCardNo("J109830255"))
        // does not match the regex at all (two letters)
        assertFalse(IdCardNoKit.isTwIdCardNo("JJ09830254"))
    }

    @Test
    fun isHkIdCardNo_twoLetterPrefixAndTrailingA() {
        // 2-letter prefix exercises the length==9 branch.
        // "AB1234569": (10*9 + 11*8) + (1*7+2*6+3*5+4*4+5*3+6*2) + 9 = 178 + 77 + 9 = 264 = 24*11 -> valid
        assertTrue(IdCardNoKit.isHkIdCardNo("AB1234569"))

        // Single-letter prefix with trailing 'A' exercises the 'A' -> 10 branch.
        // "A000002A": (522 + (65-55)*8) + (2*2) + 10 = 602 + 4 + 10 = 616 = 56*11 -> valid
        assertTrue(IdCardNoKit.isHkIdCardNo("A000002A"))

        // a 2-letter prefix with a deliberately wrong check digit -> invalid
        assertFalse(IdCardNoKit.isHkIdCardNo("AB1234560"))
    }

    @Test
    fun getBirthday_invalid15Convert() {
        // 15-length but non-numeric -> convert15To18 returns null -> getBirthday returns null
        assertNull(IdCardNoKit.getBirthday("21050284120209X"))
    }

    @Test
    fun getSex_maleAnd15Variants() {
        assertEquals(SexEnum.MALE, IdCardNoKit.getSex(MAIN_M_18))
        // 15-length but non-numeric -> convert fails -> SECRET
        assertEquals(SexEnum.SECRET, IdCardNoKit.getSex("21050284120209X"))
        // wrong length -> SECRET
        assertEquals(SexEnum.SECRET, IdCardNoKit.getSex("12345"))
    }

    @Test
    fun isIdCardNo15_dateBoundaries() {
        // month 00 -> invalid (valiDate month branch)
        assertFalse(IdCardNoKit.isIdCardNo15("210502840002091"))
        // month 13 -> invalid
        assertFalse(IdCardNoKit.isIdCardNo15("210502841302091"))
        // day 00 -> invalid
        assertFalse(IdCardNoKit.isIdCardNo15("210502840100091"))
        // April 31 -> invalid (30-day month)
        assertFalse(IdCardNoKit.isIdCardNo15("210502840431091"))
    }

    @Test
    fun isIdCardNo15_yearOutOfRangeFuture() {
        // birth year part "29" -> SimpleDateFormat("yy") resolves to 2029 (a future year, >= now)
        // -> valiDate year-range guard returns false
        assertFalse(IdCardNoKit.isIdCardNo15("210502290101001"))
    }

    @Test
    fun isIdCardNo15_februaryLeapYearValid() {
        // birth year part "00" -> 2000 (a leap year), Feb 29 is valid -> exercises the leap-year branch
        assertTrue(IdCardNoKit.isIdCardNo15("210502000229001"))
    }

}
