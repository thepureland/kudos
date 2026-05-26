package io.kudos.base.cn

import io.kudos.base.enums.impl.ProvinceEnum
import io.kudos.base.enums.impl.SexEnum
import io.kudos.base.lang.string.isNumeric
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ID card utility.
 *
 * @author K
 * @since 1.0.0
 */
object IdCardNoKit {

    /** Minimum length of a Chinese mainland citizen ID card number. */
    private const val MAINLAND_ID_MIN_LENGTH = 15

    /** Maximum length of a Chinese mainland citizen ID card number. */
    private const val MAINLAND_ID_MAX_LENGTH = 18

    /**
     * Weighting factor for each digit.
     */
    private val power = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)

    /**
     * Check codes for the 18th digit.
     */
    private val verifyCode = arrayOf("1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2")

    /** Minimum year. */
    private const val MIN = 1930

    /** Mapping from Taiwan ID card leading letters to numeric values. */
    private val twFirstCode: Map<String, Int> = mapOf(
        "A" to 10, "B" to 11, "C" to 12, "D" to 13, "E" to 14, "F" to 15, "G" to 16, "H" to 17,
        "J" to 18, "K" to 19, "L" to 20, "M" to 21, "N" to 22, "P" to 23, "Q" to 24, "R" to 25,
        "S" to 26, "T" to 27, "U" to 28, "V" to 29, "X" to 30, "Y" to 31, "W" to 32, "Z" to 33,
        "I" to 34, "O" to 35,
    )

    /**
     * Convert a 15-digit ID card number to 18 digits (mainland).
     *
     * @param idCardNo15 15-digit ID code; returns null for invalid input
     * @return 18-digit ID code
     * @author K
     * @since 1.0.0
     */
    fun convert15To18(idCardNo15: String): String? {
        if (idCardNo15.isBlank() || idCardNo15.length != MAINLAND_ID_MIN_LENGTH) return null
        if (!idCardNo15.isNumeric()) return null
        val birthday = idCardNo15.substring(6, 12)
        val cal = Calendar.getInstance()
        runCatching { SimpleDateFormat("yyMMdd").parse(birthday) }
            .getOrNull()?.let { cal.time = it }
        val sYear = cal[Calendar.YEAR].toString()
        val idCard18 = idCardNo15.substring(0, 6) + sYear + idCardNo15.substring(8)
        val iSum17 = getPowerSum(convertCharToInt(idCard18.toCharArray()))
        val sVal = getCheckCode18(iSum17).ifEmpty { return null }
        return idCard18 + sVal
    }

    /**
     * Check whether the specified string is an ID card number (mainland, Hong Kong, Macau, Taiwan).
     *
     * @param str the string to check; returns false for null
     * @return true if it is an ID card number
     * @author K
     * @since 1.0.0
     */
    fun isIdCardNo(str: CharSequence): Boolean {
        return if (str.isBlank()) {
            false
        } else isIdCardNo18(str) || isIdCardNo15(str) || isHkIdCardNo(str) || isMacauIdCardNo(str) || isTwIdCardNo(str)
    }

    /**
     * Check whether the string is an 18-digit ID number (mainland).
     *
     * @param str the string to check; returns false for null
     * @return true if it is an 18-digit ID card number
     * @author K
     * @since 1.0.0
     */
    fun isIdCardNo18(str: CharSequence): Boolean {
        if (str.isBlank() || str.length != MAINLAND_ID_MAX_LENGTH) return false
        val code17 = str.substring(0, 17)
        if (!code17.isNumeric()) return false
        val expected = getCheckCode18(getPowerSum(convertCharToInt(code17.toCharArray())))
        return expected.isNotEmpty() && expected.equals(str.last().toString(), ignoreCase = true)
    }

    /**
     * Check whether the string is a 15-digit ID number (mainland).
     *
     * @param str the string to check; returns false for null
     * @return true if it is an 18-digit ID card number
     * @author K
     * @since 1.0.0
     */
    fun isIdCardNo15(str: CharSequence): Boolean {
        if (str.isBlank() || str.length != MAINLAND_ID_MIN_LENGTH) return false
        if (!str.isNumeric()) return false
        val birthCode = str.substring(6, 12)
        val cal = Calendar.getInstance()
        runCatching { SimpleDateFormat("yy").parse(birthCode.substring(0, 2)) }
            .getOrNull()?.let { cal.time = it }
        return valiDate(
            cal[Calendar.YEAR],
            birthCode.substring(2, 4).toInt(),
            birthCode.substring(4, 6).toInt()
        )
    }

    /**
     * Check whether the string is a Taiwan ID number.
     *
     * @param str the string to check; returns false for null
     * @return true if it is a Taiwan ID card number
     * @author K
     * @since 1.0.0
     */
    fun isTwIdCardNo(str: CharSequence): Boolean {
        if (str.isBlank() || !str.matches(Regex("^[a-zA-Z][0-9]{9}$"))) return false
        val iStart = twFirstCode[str.substring(0, 1)] ?: return false
        var sum = iStart / 10 + iStart % 10 * 9
        str.substring(1, 9).forEachIndexed { idx, c -> sum += c.digitToInt() * (8 - idx) }
        val checkDigit = if (sum % 10 == 0) 0 else 10 - sum % 10
        return checkDigit == str.substring(9, 10).toInt()
    }

    /**
     * Check whether the string is a Hong Kong ID number (has bugs; some special ID cards cannot be validated).
     * The first two characters of the ID are English letters; if only one English letter appears, the first position is a space, corresponding to the number 58. The English letters A-Z in the first two positions map to the numbers 10-35.
     * The last digit is a 0-9 digit or the character "A"; "A" represents 10.
     * Convert the entire ID number to digits, multiply each digit by 9-1 respectively and sum them; if the sum is divisible by 11, the ID is valid.
     *
     * @param str the string to check; returns false for null
     * @return true if it is a Hong Kong ID card number
     * @author K
     * @since 1.0.0
     */
    fun isHkIdCardNo(str: CharSequence): Boolean {
        if (str.isBlank() || !str.matches(Regex("^[A-Z]{1,2}[0-9]{6}\\(?[0-9A]\\)?$"))) return false
        var card = str.replace(Regex("[(|)]"), "")
        var sum = if (card.length == 9) {
            val s = (card[0].uppercaseChar().code - 55) * 9 + (card[1].uppercaseChar().code - 55) * 8
            card = card.substring(1, 9)
            s
        } else {
            522 + (card[0].uppercaseChar().code - 55) * 8
        }
        card.substring(1, 7).forEachIndexed { idx, c -> sum += c.digitToInt() * (7 - idx) }
        val end = card.substring(7, 8)
        sum += if (end.equals("A", ignoreCase = true)) 10 else end.toInt()
        return sum % 11 == 0
    }

    /**
     * Check whether the string is a Macau ID number.
     *
     * @param str the string to check; returns false for null
     * @return true if it is a Macau ID card number
     * @author K
     * @since 1.0.0
     */
    fun isMacauIdCardNo(str: CharSequence): Boolean {
        return !str.isBlank() && str.matches("^[1|57][0-9]{6}\\(?[0-9A-Z]\\)?$".toRegex())
    }

    /**
     * Convert a char array to a digit array by ASCII offset ('0'..'9' -> 0..9).
     * The input is not validated for digit characters; the caller must ensure the input contains only digits.
     *
     * @param ca the char array to convert
     * @return an int array of the same length as the input
     * @author K
     * @since 1.0.0
     */
    private fun convertCharToInt(ca: CharArray): IntArray =
        IntArray(ca.size) { ca[it].code - 48 }

    /**
     * Multiply each digit of the ID number by the corresponding weighting factor and then sum the results.
     *
     * @param iArr
     * @return the ID code.
     * @author K
     * @since 1.0.0
     */
    private fun getPowerSum(iArr: IntArray): Int =
        if (power.size != iArr.size) 0
        else iArr.indices.sumOf { iArr[it] * power[it] }

    /**
     * Determine the check code by taking the power sum modulo 11.
     *
     * @param iSum
     * @return the check digit
     * @author K
     * @since 1.0.0
     */
    private fun getCheckCode18(iSum: Int): String = verifyCode[iSum % 11]

    /**
     * Get the birth date from an ID number (mainland ID cards only).
     *
     * @param idCardNo the ID number; returns null for null, blank, or non-mainland ID cards
     * @return birth date (yyyyMMdd)
     * @author K
     * @since 1.0.0
     */
    fun getBirthday(idCardNo: String): String? {
        var idNo = idCardNo
        if (idNo.isBlank()) {
            return null
        }
        val len = idNo.length
        if (len < MAINLAND_ID_MIN_LENGTH) {
            return null
        } else if (len == MAINLAND_ID_MIN_LENGTH) {
            idNo = convert15To18(idNo) ?: return null
        }
        return idNo.substring(6, 14)
    }

    /**
     * Get the sex from an ID number (mainland and Taiwan only).
     *
     * @param idCardNo the ID number; returns SexEnum.UNKNOWN for null
     * @return the sex enum
     * @author K
     * @since 1.0.0
     */
    fun getSex(idCardNo: String): SexEnum {
        if (idCardNo.isBlank()) return SexEnum.SECRET
        if (isTwIdCardNo(idCardNo)) {
            return if (idCardNo[1] == '1') SexEnum.MALE else SexEnum.FEMALE
        }
        if (idCardNo.length !in setOf(MAINLAND_ID_MIN_LENGTH, MAINLAND_ID_MAX_LENGTH)) return SexEnum.SECRET
        val idNo = if (idCardNo.length == MAINLAND_ID_MIN_LENGTH) {
            convert15To18(idCardNo) ?: return SexEnum.SECRET
        } else idCardNo
        return if (idNo.substring(16, 17).toInt() % 2 != 0) SexEnum.MALE else SexEnum.FEMALE
    }

    /**
     * Get the registered province from an ID number (mainland, Hong Kong, Macau, Taiwan).
     *
     * @param idCardNo the ID number; returns null for null or empty
     * @return the province enum, or null if no match
     * @author K
     * @since 1.0.0
     */
    fun getProvince(idCardNo: String): ProvinceEnum? {
        if (idCardNo.isBlank()) return null
        return when {
            isIdCardNo15(idCardNo) || isIdCardNo18(idCardNo) -> ProvinceEnum.enumOf(idCardNo.substring(0, 2))
            isHkIdCardNo(idCardNo) -> ProvinceEnum.XIANG_GANG
            isTwIdCardNo(idCardNo) -> ProvinceEnum.TAI_WAN
            isMacauIdCardNo(idCardNo) -> ProvinceEnum.AO_MEN
            else -> null
        }
    }

    /**
     * Validate whether the date is valid and earlier than the current date.
     *
     * @param iYear year to validate
     * @param iMonth month to validate (1-12)
     * @param iDate day to validate
     * @return whether the date is valid
     * @author K
     * @since 1.0.0
     */
    private fun valiDate(iYear: Int, iMonth: Int, iDate: Int): Boolean {
        val cal = Calendar.getInstance()
        val year = cal[Calendar.YEAR]
        if (iYear !in MIN..<year) {
            return false
        }
        if (iMonth !in 1..12) {
            return false
        }
        val datePerMonth: Int = when (iMonth) {
            4, 6, 9, 11 -> 30
            2 -> {
                val dm = (iYear % 4 == 0 && iYear % 100 != 0 || iYear % 400 == 0) && iYear > MIN
                if (dm) 29 else 28
            }
            else -> 31
        }
        return iDate in 1..datePerMonth
    }

}
