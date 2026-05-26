package io.kudos.base.lang.collections


/**
 * Array operation utility class.
 *
 * @author K
 * @since 1.0.0
 */
object ArrayKit {

    /**
     * Checks whether a byte array is null or empty.
     *
     * @param array the byte array
     * @return true if the byte array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isByteArrayEmpty(array: ByteArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a char array is null or empty.
     *
     * @param array the char array
     * @return true if the char array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isCharArrayEmpty(array: CharArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a Short array is null or empty.
     *
     * @param array the Short array
     * @return true if the Short array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isShortArrayEmpty(array: ShortArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether an Int array is null or empty.
     *
     * @param array the Int array
     * @return true if the Int array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isIntArrayEmpty(array: IntArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a Long array is null or empty.
     *
     * @param array the Long array
     * @return true if the Long array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isLongArrayEmpty(array: LongArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a Float array is null or empty.
     *
     * @param array the Float array
     * @return true if the Float array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isFloatArrayEmpty(array: FloatArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a Double array is null or empty.
     *
     * @param array the Double array
     * @return true if the Double array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isDoubleArrayEmpty(array: DoubleArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether a boolean array is null or empty.
     *
     * @param array the boolean array
     * @return true if the boolean array is null or empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isBooleanArrayEmpty(array: BooleanArray?): Boolean = array == null || array.isEmpty()

    /**
     * Checks whether an array is both non-null and non-empty.
     *
     * @param array the array
     * @return true if the array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isNotEmpty(array: Array<*>?): Boolean = !array.isNullOrEmpty()

    /**
     * Checks whether a byte array is both non-null and non-empty.
     *
     * @param array the byte array
     * @return true if the byte array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isByteArrayNotEmpty(array: ByteArray?): Boolean = !isByteArrayEmpty(array)

    /**
     * Checks whether a char array is both non-null and non-empty.
     *
     * @param array the char array
     * @return true if the char array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isCharArrayNotEmpty(array: CharArray?): Boolean = !isCharArrayEmpty(array)

    /**
     * Checks whether a Short array is both non-null and non-empty.
     *
     * @param array the Short array
     * @return true if the Short array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isShortArrayNotEmpty(array: ShortArray?): Boolean = !isShortArrayEmpty(array)

    /**
     * Checks whether an Int array is both non-null and non-empty.
     *
     * @param array the Int array
     * @return true if the Int array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isIntArrayNotEmpty(array: IntArray?): Boolean = !isIntArrayEmpty(array)

    /**
     * Checks whether a Long array is both non-null and non-empty.
     *
     * @param array the Long array
     * @return true if the Long array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isLongArrayNotEmpty(array: LongArray?): Boolean = !isLongArrayEmpty(array)

    /**
     * Checks whether a Float array is both non-null and non-empty.
     *
     * @param array the Float array
     * @return true if the Float array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isFloatArrayNotEmpty(array: FloatArray?): Boolean = !isFloatArrayEmpty(array)

    /**
     * Checks whether a Double array is both non-null and non-empty.
     *
     * @param array the Double array
     * @return true if the Double array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isDoubleArrayNotEmpty(array: DoubleArray?): Boolean = !isDoubleArrayEmpty(array)

    /**
     * Checks whether a boolean array is both non-null and non-empty.
     *
     * @param array the boolean array
     * @return true if the boolean array is non-null and non-empty, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isBooleanArrayNotEmpty(array: BooleanArray?): Boolean = !isBooleanArrayEmpty(array)

}
