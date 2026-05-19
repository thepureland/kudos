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
 * 身份证工具类.
 *
 * @author K
 * @since 1.0.0
 */
object IdCardNoKit {

    /** 中国大陆公民身份证号码最小长度。  */
    private const val MAINLAND_ID_MIN_LENGTH = 15

    /** 中国大陆公民身份证号码最大长度。  */
    private const val MAINLAND_ID_MAX_LENGTH = 18

    /**
     * 每位加权因子
     */
    private val power = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)

    /**
     * 第18位校检码
     */
    private val verifyCode = arrayOf("1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2")

    /** 最低年限  */
    private const val MIN = 1930

    /** 台湾身份首字母对应数字  */
    private val twFirstCode: Map<String, Int> = mapOf(
        "A" to 10, "B" to 11, "C" to 12, "D" to 13, "E" to 14, "F" to 15, "G" to 16, "H" to 17,
        "J" to 18, "K" to 19, "L" to 20, "M" to 21, "N" to 22, "P" to 23, "Q" to 24, "R" to 25,
        "S" to 26, "T" to 27, "U" to 28, "V" to 29, "X" to 30, "Y" to 31, "W" to 32, "Z" to 33,
        "I" to 34, "O" to 35,
    )

    /**
     * 将15位身份证号码转换为18位(大陆)
     *
     * @param idCardNo15 15位身份编码, 非法值将返回null
     * @return 18位身份编码
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
     * 检查指定字符串是否为身份证号(包括大陆、港、澳、台)
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为身份证号
     * @author K
     * @since 1.0.0
     */
    fun isIdCardNo(str: CharSequence): Boolean {
        return if (str.isBlank()) {
            false
        } else isIdCardNo18(str) || isIdCardNo15(str) || isHkIdCardNo(str) || isMacauIdCardNo(str) || isTwIdCardNo(str)
    }

    /**
     * 检查是否为18位身份号(大陆)
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为18位身份证号
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
     * 检查是否为15位身份号(大陆)
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为18位身份证号
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
     * 检查是否为台湾身份号
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为台湾身份证号
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
     * 检查是否为香港身份号(存在Bug，部份特殊身份证无法检查)
     * 身份证前2位为英文字符，如果只出现一个英文字符则表示第一位是空格，对应数字58 前2位英文字符A-Z分别对应数字10-35
     * 最后一位校验码为0-9的数字加上字符"A"，"A"代表10
     * 将身份证号码全部转换为数字，分别对应乘9-1相加的总和，整除11则证件号码有效
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为香港身份证号
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
     * 检查是否为澳门身份号
     *
     * @param str 待检查的字符串, 为null返回false
     * @return true: 为澳门身份证号
     * @author K
     * @since 1.0.0
     */
    fun isMacauIdCardNo(str: CharSequence): Boolean {
        return !str.isBlank() && str.matches("^[1|57][0-9]{6}\\(?[0-9A-Z]\\)?$".toRegex())
    }

    /**
     * 将字符数组按 ASCII 偏移转换为数字数组（'0'..'9' -> 0..9）。
     * 入参未做数字字符校验，调用方需先保证输入仅含数字字符。
     *
     * @param ca 待转换的字符数组
     * @return 与入参等长的整型数组
     * @author K
     * @since 1.0.0
     */
    private fun convertCharToInt(ca: CharArray): IntArray =
        IntArray(ca.size) { ca[it].code - 48 }

    /**
     * 将身份证的每位和对应位的加权因子相乘之后，再得到和值
     *
     * @param iArr
     * @return 身份证编码。
     * @author K
     * @since 1.0.0
     */
    private fun getPowerSum(iArr: IntArray): Int =
        if (power.size != iArr.size) 0
        else iArr.indices.sumOf { iArr[it] * power[it] }

    /**
     * 将power和值与11取模获得余数进行校验码判断
     *
     * @param iSum
     * @return 校验位
     * @author K
     * @since 1.0.0
     */
    private fun getCheckCode18(iSum: Int): String = verifyCode[iSum % 11]

    /**
     * 根据身份编号获取生日(仅限大陆身份证)
     *
     * @param idCardNo 身份证号, 为null或空或不是大陆身份证将返回null
     * @return 生日(yyyyMMdd)
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
     * 根据身份证号获取性别(仅限大陆和台湾)
     *
     * @param idCardNo 身份证号，为null返回SexEnum.UNKNOWN
     * @return 性别枚举
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
     * 根据身份证号获取户籍省份(包括大陆、港、澳、台)
     *
     * @param idCardNo 身份证号 为null或空返回null
     * @return 省枚举，未匹配返回null
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
     * 验证小于当前日期 是否有效
     *
     * @param iYear 待验证日期(年)
     * @param iMonth 待验证日期(月 1-12)
     * @param iDate 待验证日期(日)
     * @return 是否有效
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