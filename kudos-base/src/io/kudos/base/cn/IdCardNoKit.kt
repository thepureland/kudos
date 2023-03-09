package io.kudos.base.cn

import org.soul.base.cn.IdCardNoTool
import org.soul.base.cn.ProvinceEnum
import org.soul.base.enums.SexEnum

/**
 * 身份证工具类.
 *
 * @author K
 * @since 1.0.0
 */
object IdCardNoKit {

    /**
     * 将15位身份证号码转换为18位(大陆)
     *
     * @param idCardNo15 15位身份编码, 非法值将返回null
     * @return 18位身份编码
     * @author K
     * @since 1.0.0
     */
    fun convert15To18(idCardNo15: String): String? {
        return IdCardNoTool.convert15To18(idCardNo15)
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
        return IdCardNoTool.isIdCardNo(str.toString())
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
        return IdCardNoTool.isIdCardNo18(str.toString())
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
        return IdCardNoTool.isIdCardNo15(str.toString())
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
        return IdCardNoTool.isTwIdCardNo(str.toString())
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
        return IdCardNoTool.isHkIdCardNo(str.toString())
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
        return IdCardNoTool.isMacauIdCardNo(str.toString())
    }

    /**
     * 根据身份编号获取生日(仅限大陆身份证)
     *
     * @param idCardNo 身份证号, 为null或空或不是大陆身份证将返回null
     * @return 生日(yyyyMMdd)
     * @author K
     * @since 1.0.0
     */
    fun getBirthday(idCardNo: String): String? {
        return IdCardNoTool.getBirthday(idCardNo)
    }

    /**
     * 根据身份证号获取性别(仅限大陆和台湾)
     *
     * @param idCardNo 身份证号，为null返回Sex.UNKNOWN
     * @return 性别枚举
     * @author K
     * @since 1.0.0
     */
    fun getSex(idCardNo: String): SexEnum {
        return IdCardNoTool.getSex(idCardNo)
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
        return IdCardNoTool.getProvince(idCardNo)
    }

}