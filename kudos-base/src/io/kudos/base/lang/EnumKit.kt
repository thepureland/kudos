package io.kudos.base.lang

import io.kudos.base.support.Consts
import org.soul.base.enums.EnumTool
import org.soul.base.ienums.ICodeEnum
import java.util.*
import kotlin.reflect.KClass

/**
 * 枚举工具类
 *
 * @author K
 * @since 1.0.0
 */
object EnumKit {

    /**
     * 根据字典枚举全类名字符串和字典代码，取得对应的枚举元素的译文
     *
     * @param enumClassStr 字典枚举全类名字符串，不能为空白串, 且对应的类必须实现ICodeEnum接口
     * @param code 字典代码
     * @return 字典枚举元素的译文，不存在时返回null
     * @throws IllegalArgumentException 参数不合法时
     * @author K
     * @since 1.0.0
     */
    fun trans(enumClassStr: String, code: String): String? = EnumTool.trans(enumClassStr, code)

    /**
     * 根据字典枚举类型和字典代码，取得对应的枚举元素
     *
     * @param E 枚举类型
     * @param enumClass 字典枚举类型，该枚举类必须实现ICodeEnum接口
     * @param code 字典代码
     * @return 字典枚举元素，不存在时返回null
     * @throws IllegalArgumentException 参数不合法时
     * @author K
     * @since 1.0.0
     */
    fun <E : ICodeEnum> enumOf(enumClass: KClass<E>, code: String): E? = EnumTool.enumOf(enumClass.java, code)

    /**
     * 根据枚举全类名和字典代码，取得对应的枚举元素
     *
     * @param enumClassStr 字典枚举全类名字符串，不能为空白串, 且对应的类必须实现ICodeEnum接口
     * @param code 字典代码
     * @return 字典枚举元素的译文，不存在时返回null
     * @throws IllegalArgumentException 参数不合法时
     * @author K
     * @since 1.0.0
     */
    fun enumOf(enumClassStr: String, code: String): ICodeEnum? = EnumTool.enumOf(enumClassStr, code)

    /**
     * 取得字典枚举的所有代码及其翻译信息
     *
     * @param enumClass 字典枚举类
     * @return Map(字典代码，译文)
     * @author K
     * @since 1.0.0
     */
    fun getCodeMap(enumClass: KClass<out ICodeEnum>): Map<String, String> = EnumTool.getCodeMap(enumClass.java)

    /**
     * 取得字典枚举的所有代码及其翻译信息
     *
     * @param enumClassStr 字典枚举全类名字符串，不能为空白串, 且对应的类必须实现ICodeEnum接口
     * @return Map(字典代码，译文)
     * @throws IllegalArgumentException 参数不合法时
     * @author K
     * @since 1.0.0
     */
    fun getCodeMap(enumClassStr: String): Map<String, String> = EnumTool.getCodeMap(enumClassStr)

    /**
     * 根据枚举全类名，取得对应的枚举元素
     *
     * @param enumClassStr 枚举全类名，不能为null或空串, 且对应的类必须实现ICodeEnum接口
     * @return 枚举类
     * @throws IllegalArgumentException 参数为空或根据参数查找失败时
     * @author K
     * @since 1.0.0
     */
    @Suppress(Consts.Suppress.UNCHECKED_CAST)
    fun getCodeEnumClass(enumClassStr: String): KClass<out ICodeEnum> = EnumTool.getCodeEnumClass(enumClassStr).kotlin


    /**
     * 将枚举中的元素放以Map的形式返回
     *
     * @param E 枚举类型
     * @param enumClass 待查找的枚举类
     * @return 可修改的map, 不会为null. Map(枚举元素name，枚举元素)
     * @throws IllegalArgumentException enumClass参数为null时
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnumMap(enumClass: KClass<E>): Map<String, E> = EnumTool.getEnumMap(enumClass.java)

    /**
     * 将枚举中的元素放以List的形式返回
     *
     *
     * @param E 枚举类型
     * @param enumClass 待查找的枚举类
     * @return 可修改的list, 不会为null. List<枚举元素>
     * @throws IllegalArgumentException enumClass参数为null时
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnumList(enumClass: KClass<E>): List<E> = EnumTool.getEnumList(enumClass.java)

    /**
     * 检查指定是名字是否为指定的枚举类的有效枚举元素
     * 该方法[Enum.valueOf]不同，当枚举名无效时它不会抛出异常。
     *
     * @param E 枚举类型
     * @param enumClass  待查找的枚举类
     * @param enumName   枚举元素名， null将返回false
     * @return true 如果枚举元素名有效, 否则为 false
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> isValidEnum(enumClass: KClass<E>, enumName: String?): Boolean =
        EnumTool.isValidEnum(enumClass.java, enumName)

    /**
     * 根据枚举元素名称获取对应的枚举元素，如果没找到返回null
     * 该方法[Enum.valueOf]不同，当枚举名无效时它不会抛出异常。
     *
     * @param E 枚举类型
     * @param enumClass  待查找的枚举类
     * @param enumName   枚举元素名， null将返回null
     * @return 枚举元素, 如果没找到返回null
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnum(enumClass: KClass<E>, enumName: String?): E = EnumTool.getEnum(enumClass.java, enumName)

    /**
     * 创建一个long型位向量来表示指定的枚举子集。
     * 该方法生成的值可以作为[processBitVector]的输入
     * 当您的枚举中有超过64个值时不要使用该方法，因为这将创建一个超过long型所允许的最大值的值。
     *
     * @param E       枚举类型
     * @param enumClass 枚举类
     * @param values    需要转换的枚举元素的迭代器
     * @return 一个长整形值， 它的位值代表枚举元素的值
     * @throws NullPointerException 如果 `enumClass` 或 `values` 为 `null`
     * @throws IllegalArgumentException 如果 `enumClass` 不是一个枚举类或超过64个枚举元素
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> generateBitVector(enumClass: KClass<E>, values: Iterable<E>): Long =
        EnumTool.generateBitVector(enumClass.java, values)

    /**
     * 创建一个long型位向量来表示指定的枚举数组
     * 该方法生成的值可以作为[processBitVector]的输入
     * 当您的枚举中有超过64个值时不要使用该方法，因为这将创建一个超过long型所允许的最大值的值。
     *
     * @param E       枚举类型
     * @param enumClass 枚举类, 不能为 `null`
     * @param values    需要转换的枚举元素的可变数组, 不能为 `null`
     * @return 一个长整形值， 它的位值代表枚举元素的值
     * @throws NullPointerException 如果 `enumClass` 或 `values` 为 `null`
     * @throws IllegalArgumentException 如果 `enumClass` 不是一个枚举类或超过64个枚举元素
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> generateBitVector(enumClass: KClass<E>, vararg values: E): Long =
        EnumTool.generateBitVector(enumClass.java, *values)

    /**
     * 将[generateBitVector]创建的长整形值转换为它所表示的枚举元素集合
     * 如果您存储了该值，谨防枚举任何更改会影响序号值。
     *
     * @param E       枚举类型
     * @param enumClass 枚举类
     * @param value     表示的枚举元素集合的长整形值
     * @return 枚举元素集合
     * @throws NullPointerException 如果 `enumClass` 为 `null`
     * @throws IllegalArgumentException 如果 `enumClass` 不是一个枚举类或超过64个枚举元素
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> processBitVector(enumClass: KClass<E>, value: Long): EnumSet<E> =
        EnumTool.processBitVector(enumClass.java, value)

}