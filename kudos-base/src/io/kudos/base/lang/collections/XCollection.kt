package io.kudos.base.lang.collections

/**
 * kotlin.Collection extension functions
 *
 * @author K
 * @since 1.0.0
 */


///**
// * Converts all elements of a Collection to String (via toString()), prepending each element with prefix
// * and appending postfix, e.g. <div>mymessage</div>.
// *
// * @param prefix prefix to prepend, defaults to empty string
// * @param postfix postfix to append, defaults to empty string
// * @param seperator separator between elements, defaults to empty string
// * @return concatenated string of each element's toString value with prefix and postfix
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use kotlin's built-in joinToString() directly")
//fun Collection<*>.joinEachToString(prefix: String = "", postfix: String = "", seperator: String = ""): String {
//    if (this.isEmpty()) return ""
//    val builder = StringBuilder()
//    this.forEachIndexed { index, elem ->
//        builder.append(prefix).append(elem).append(postfix)
//        if (index != this.size - 1) {
//            builder.append(seperator)
//        }
//    }
//    return builder.toString()
//}
//
//
///**
// * Returns the number of occurrences of each identical element in the collection.
// *
// * @return Map(element in collection, count of occurrences)
// * @author K
// * @since 1.0.0
// */
//@Deprecated("Use kotlin's groupingBy { it }.eachCount() directly")
//fun <T> Collection<T>.getCardinalityMap(): Map<T, Int> = CollectionUtils.getCardinalityMap(this) as Map<T, Int>

/**
 * Tests whether two collections have the same size and contain the same elements.
 *
 * @param other the other collection; returns false if null
 * @return `true` if the two collections have the same size and contain the same elements
 * @author K
 * @since 1.0.0
 */
fun <T> Collection<T?>.isEqualCollection(other: Collection<T?>?): Boolean {
    if (other == null) return false
    if (size != other.size) return false
    // Build frequency maps for the two collections
    val map1 = groupingBy { it }.eachCount()
    val map2 = other.groupingBy { it }.eachCount()
    return map1 == map2
}
