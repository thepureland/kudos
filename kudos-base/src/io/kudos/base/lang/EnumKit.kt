package io.kudos.base.lang

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.lang.EnumKit.generateBitVector
import io.kudos.base.lang.EnumKit.processBitVector
import io.kudos.base.lang.reflect.isEnum
import io.kudos.base.logger.LogFactory
import org.apache.commons.lang3.EnumUtils
import java.util.EnumSet
import kotlin.reflect.KClass

/**
 * Enum utility.
 *
 * @author K
 * @since 1.0.0
 */
object EnumKit {

    private val LOG = LogFactory.getLog(this::class)

    /**
     * Returns the display text of the dictionary enum element identified by the given enum class FQN and dictionary code.
     *
     * @param enumClassStr the fully qualified name of the dictionary enum class; must not be blank, and must implement ICodeEnum
     * @param code the dictionary code
     * @return the display text of the dictionary enum element, or null if not found
     * @throws IllegalArgumentException if the arguments are invalid
     * @author K
     * @since 1.0.0
     */
    fun displayText(enumClassStr: String, code: String): String? {
        enumOf(enumClassStr, code)?.let { return it.displayText }
        LOG.warn("Enum class [${enumClassStr}] has no element with code [${code}]")
        return null
    }

    /**
     * Returns the dictionary enum element matching the given enum type and dictionary code.
     *
     * @param E the enum type
     * @param enumClass the dictionary enum type; must implement ICodeEnum
     * @param code the dictionary code
     * @return the dictionary enum element, or null if not found
     * @throws IllegalArgumentException if the arguments are invalid
     * @author K
     * @since 1.0.0
     */
    fun <E : IDictEnum> enumOf(enumClass: KClass<E>, code: String): E? {
        require(enumClass.isEnum()) { "The given class [${enumClass}] is not an enum" }
        require(code.isNotBlank()) { "The dictionary code argument must not be blank" }

        enumClass.java.enumConstants.firstOrNull { it.code == code }?.let { return it }
        LOG.warn("Enum class [${enumClass}] has no element with code [$code]")
        return null
    }

    /**
     * Returns the dictionary enum element matching the given enum FQN and dictionary code.
     *
     * @param enumClassStr the fully qualified name of the dictionary enum class; must not be blank, and must implement ICodeEnum
     * @param code the dictionary code
     * @return the display text of the dictionary enum element, or null if not found
     * @throws IllegalArgumentException if the arguments are invalid
     * @author K
     * @since 1.0.0
     */
    fun enumOf(enumClassStr: String, code: String): IDictEnum? {
        require(code.isNotBlank()) { "The dictionary code argument must not be blank" }

        val enumClazz = getCodeEnumClass(enumClassStr)
        return enumOf(enumClazz, code)
    }

    /**
     * Returns the codes and display texts of all items in the dictionary enum.
     *
     * @param enumClass the dictionary enum class
     * @return a map of dictionary item code to display text
     * @author K
     * @since 1.0.0
     */

    fun getCodeMap(enumClass: KClass<out IDictEnum>): Map<String, String> =
        enumClass.java.enumConstants
            .filterIsInstance<IDictEnum>()
            .associate { it.code to it.displayText }

    /**
     * Returns the codes and display texts of all items in the dictionary enum.
     *
     * @param enumClassStr the fully qualified name of the dictionary enum class; must not be blank, and must implement ICodeEnum
     * @return a map of dictionary code to display text
     * @throws IllegalArgumentException if the arguments are invalid
     * @author K
     * @since 1.0.0
     */
    fun getCodeMap(enumClassStr: String): Map<String, String> {
        val codeEnumClass = getCodeEnumClass(enumClassStr)
        return getCodeMap(codeEnumClass)
    }

    /**
     * Returns the enum class for the given FQN.
     *
     * @param enumClassStr the FQN of the enum; must not be null or blank, and must implement ICodeEnum
     * @return the enum class
     * @throws IllegalArgumentException if the argument is blank or the lookup fails
     * @author K
     * @since 1.0.0
     */
    fun getCodeEnumClass(enumClassStr: String): KClass<out IDictEnum> {
        require(enumClassStr.isNotBlank()) { "The dictionary enum FQN argument must not be blank" }
        val enumClazz = runCatching { Class.forName(enumClassStr) }
            .getOrElse { throw IllegalArgumentException("Class [$enumClassStr] does not exist!") }
        require(enumClazz.isEnum) { "Class [$enumClassStr] is not an enum!" }
        require(IDictEnum::class.java.isAssignableFrom(enumClazz)) {
            "Class [$enumClassStr] does not implement the [${IDictEnum::class}] interface!"
        }
        return enumClazz.asSubclass(IDictEnum::class.java).kotlin
    }


    /**
     * Returns the enum elements as a Map.
     *
     * @param E the enum type
     * @param enumClass the enum class to look up
     * @return a mutable map, never null. Map(enum element name -> enum element)
     * @throws IllegalArgumentException if the enumClass argument is null
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnumMap(enumClass: KClass<E>): Map<String, E> = EnumUtils.getEnumMap(enumClass.java)

    /**
     * Returns the enum elements as a List.
     *
     *
     * @param E the enum type
     * @param enumClass the enum class to look up
     * @return a mutable list, never null. List of enum elements
     * @throws IllegalArgumentException if the enumClass argument is null
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnumList(enumClass: KClass<E>): List<E> = EnumUtils.getEnumList(enumClass.java)

    /**
     * Checks whether the given name is a valid enum element of the given enum class.
     * Unlike enum's valueOf method, this method does not throw an exception when the enum name is invalid.
     *
     * @param E the enum type
     * @param enumClass the enum class to look up
     * @param enumName the enum element name; null returns false
     * @return true if the enum element name is valid; otherwise false
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> isValidEnum(enumClass: KClass<E>, enumName: String?): Boolean =
        EnumUtils.isValidEnum(enumClass.java, enumName)

    /**
     * Returns the enum element with the given name, or null if not found.
     * Unlike enum's valueOf method, this method does not throw an exception when the enum name is invalid.
     *
     * @param E the enum type
     * @param enumClass the enum class to look up
     * @param enumName the enum element name; null returns null
     * @return the enum element, or null if not found
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> getEnum(enumClass: KClass<E>, enumName: String?): E? = EnumUtils.getEnum(enumClass.java, enumName)

    /**
     * Creates a long-typed bit vector representing the given enum subset.
     * The value generated by this method can be used as input to [processBitVector].
     * Do not use this method when your enum has more than 64 values, because it would create a value
     * exceeding the maximum allowed for a long.
     *
     * @param E       the enum type
     * @param enumClass the enum class
     * @param values    iterable of enum elements to convert
     * @return a long value whose bit values represent the enum elements
     * @throws NullPointerException if `enumClass` or `values` is `null`
     * @throws IllegalArgumentException if `enumClass` is not an enum class or has more than 64 enum elements
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> generateBitVector(enumClass: KClass<E>, values: Iterable<E>): Long =
        EnumUtils.generateBitVector(enumClass.java, values)

    /**
     * Creates a long-typed bit vector representing the given enum array.
     * The value generated by this method can be used as input to [processBitVector].
     * Do not use this method when your enum has more than 64 values, because it would create a value
     * exceeding the maximum allowed for a long.
     *
     * @param E       the enum type
     * @param enumClass the enum class; must not be `null`
     * @param values    variable-length array of enum elements to convert; must not be `null`
     * @return a long value whose bit values represent the enum elements
     * @throws NullPointerException if `enumClass` or `values` is `null`
     * @throws IllegalArgumentException if `enumClass` is not an enum class or has more than 64 enum elements
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> generateBitVector(enumClass: KClass<E>, vararg values: E): Long =
        EnumUtils.generateBitVector(enumClass.java, *values)

    /**
     * Converts the long value created by [generateBitVector] back into the set of enum elements it represents.
     * If you store this value, beware that any changes to the enum may affect the ordinal values.
     *
     * @param E       the enum type
     * @param enumClass the enum class
     * @param value     the long value representing the set of enum elements
     * @return the set of enum elements
     * @throws NullPointerException if `enumClass` is `null`
     * @throws IllegalArgumentException if `enumClass` is not an enum class or has more than 64 enum elements
     * @author K
     * @since 1.0.0
     */
    fun <E : Enum<E>> processBitVector(enumClass: KClass<E>, value: Long): EnumSet<E> =
        EnumUtils.processBitVector(enumClass.java, value)

}
