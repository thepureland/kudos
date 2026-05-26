package io.kudos.base.lang.collections

import io.kudos.base.lang.string.toType
import kotlin.reflect.KClass

/**
 * kotlin.Array extension functions
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Converts a String array to an array of the specified numeric type.
 *
 * @param T element type of the resulting array
 * @param clazz element type of the resulting array
 * @return the converted array
 * @author K
 * @since 1.0.0
 */
inline fun <reified T : Number> Array<String>.toNumberArray(clazz: KClass<T>): Array<T> {
    val list = this.map { it.toType(clazz) }
    return list.toTypedArray()
}

///**
// * Returns the string value of a (one-dimensional) array (unlike toString, no braces are added around it).
// *
// * @return the string representation of the array, or empty string if the array argument is null
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use kotlin's built-in contentToString()")
//fun Array<*>.toPlainString(): String {
//    val s = this.toString()
//    return s.substring(1, s.length - 1)
//}
//
//
//
///**
// * Produces a new array containing the elements of the original array from index `start` (inclusive) to `end` (exclusive).
// * The start index is included and the end index is excluded. Returns null if the input array is null.
// * The element type of the subarray is the same as the original. So, if the input array's element type is `Date`, the following usage is expected:
// *
// * @param T the element type of the array
// * @param startIndexInclusive starting index (inclusive). Values < 0 are treated as 0; values > array length return an empty array.
// * @param endIndexExclusive ending index (exclusive). Values < start index return an empty array; values > array length are treated as array length.
// * @return a new array containing the elements of the original array from `start` (inclusive) to `end` (exclusive)
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use kotlin's built-in copyOfRange(fromIndex, toIndex)")
//fun <T> Array<T>.subarray(startIndexInclusive: Int, endIndexExclusive: Int): Array<T> =
//    ArrayUtils.subarray(this, startIndexInclusive, endIndexExclusive)
//
//
///**
// * Adds all elements of the given arrays to a new array.
// * The new array contains all elements of `array1` and `array2`. Always returns a new array.
// *
// *
// * <pre>
// * array1.addAll(null)   = cloned copy of array1
// * [].addAll([])         = []
// * [null].addAll([null]) = [null, null]
// * ["a", "b", "c"].addAll(["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
// * </pre>
// *
// * @param T element type of the array
// * @param array2 the second array whose elements are to be added to the new array; may be `null`
// * @return the new array
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use the + operator directly")
//fun <T> Array<T>.addAll(vararg array2: T?): Array<T> = ArrayUtils.addAll(this, *array2)
//
///**
// * Copies the given array into a new array and appends the given element at the end.
// *
// * <pre>
// * ["a"].add(null)     = ["a", null]
// * ["a"].add("b")      = ["a", "b"]
// * ["a", "b"].add("c") = ["a", "b", "c"]
// * </pre>
// *
// * @param T element type of the array
// * @param element the element to add at the end; may be `null`
// * @return a new array containing all elements of the given array plus the specified element appended at the end
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use the + operator directly")
//fun <T> Array<T>.add(element: T?): Array<T> = ArrayUtils.add(this, element)

/**
 * Inserts the given element at the specified index in the array, shifting the element at that index and all subsequent elements to the right.
 *
 * <pre>
 * ["a"].add(1, null)     = ["a", null]
 * ["a"].add(1, "b")      = ["a", "b"]
 * ["a", "b"].add(3, "c") = ["a", "b", "c"]
 * </pre>
 *
 * @param T element type of the array
 * @param index the position to insert at
 * @param element the element to insert
 * @return an array containing all elements of the input array plus the specified element
 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
 * @author K
 * @since 1.0.0
 */
fun <T> Array<T?>.add(index: Int, element: T?) : Array<T?> {
    if (index !in 0..size) {
        throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
    }
    return copyOfRange(0, index) + element + copyOfRange(index, size)
}

/**
 * Removes the element at the specified index from the array, shifting subsequent elements to the left.
 *
 * <pre>
 * ["a"].remove(0)           = []
 * ["a", "b"].remove(0)      = ["b"]
 * ["a", "b"].remove(1)      = ["a"]
 * ["a", "b", "c"].remove(1) = ["a", "c"]
 * </pre>
 *
 * @param T element type of the array
 * @param index index of the element to remove
 * @return a new array containing all elements of the original except the one at the specified index
 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length)
 * @author K
 * @since 1.0.0
 */
fun <T> Array<T?>.remove(index: Int): Array<T?> {
    if (index !in 0..size) {
        throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
    }
    return copyOfRange(0, index) + copyOfRange(index + 1, size)
}

/**
 * Removes the first occurrence of the specified element from the array, shifting subsequent elements to the left.
 * If the array does not contain the element, no element is removed.
 *
 * <pre>
 * [].removeElement("a")              = []
 * ["a"].removeElement("b")           = ["a"]
 * ["a", "b"].removeElement("a")      = ["b"]
 * ["a", "b", "a"].removeElement("a") = ["b", "a"]
 * </pre>
 *
 * @param T element type of the array
 * @param element the element to remove
 * @return a new array containing all elements of the original except the one at the matched position
 * @author K
 * @since 1.0.0
 */
fun <T> Array<T?>.removeElement(element: T?): Array<T?> {
    val idx = this.indexOf(element)
    return if (idx < 0) this else this.remove(idx)
}

/**
 * Removes the elements at all of the specified indices from the array, shifting subsequent elements to the left. This method returns a new array.
 *
 * <pre>
 * ["a", "b", "c"].removeAll(0, 2) = ["b"]
 * ["a", "b", "c"].removeAll(1, 2) = ["a"]
 * </pre>
 *
 * @param T element type of the array
 * @param indices vararg of indices of the elements to remove
 * @return a new array containing all elements of the original except those at the specified indices
 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= array.length)
 * @author K
 * @since 1.0.0
 */
inline fun <reified T> Array<T?>.removeAll(vararg indices: Int): Array<T?> {
    val toRemove = indices.toSet()
    return filterIndexed { idx, _ -> idx !in toRemove }.toTypedArray()
}

/**
 * Removes all occurrences of the specified elements from the array, shifting subsequent elements to the left.
 * If a specified element is not present in the array, only the elements that are present will be removed.
 * This method returns a new array.
 *
 * <pre>
 * [].removeElements("a", "b")              = []
 * ["a"].removeElements("b", "c")           = ["a"]
 * ["a", "b"].removeElements("a", "c")      = ["b"]
 * ["a", "b", "a"].removeElements("a")      = ["b", "a"]
 * ["a", "b", "a"].removeElements("a", "a") = ["b"]
 * </pre>
 *
 * @param T element type of the array
 * @param values the values to remove from the array
 * @return a new array containing all elements of the original except the values to be removed
 * @author K
 * @since 1.0.0
 */
inline fun <reified T> Array<T?>.removeElements(vararg values: T?): Array<T?> {
    val toRemove = values.toSet()
    return this.filter { it !in toRemove }.toTypedArray()
}
