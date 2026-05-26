package io.kudos.base.lang.collections

import java.io.PrintStream
import java.util.ResourceBundle

/**
 * kotlin.Map extension functions
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Converts a Map to a two-dimensional array, each row containing two elements: the Map key and value, in order.
 *
 * @return a two-dimensional array
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.toArrOfArr(): Array<Array<Any?>> =
    // entries preserves the Map's order; an empty Map yields an empty 2-D array
    entries.map { (k, v) -> arrayOf(k, v) }.toTypedArray()



/**
 * Returns whether this map contains the given submap (every key and value matches).
 *
 * @param <K> Key
 * @param V Value
 * @param subMap submap; returns false if null or empty
 * @return true if every key and value matches, false otherwise
 * @author K
 * @since 1.0.0
 */
fun <K, V> Map<*, *>.containsAll(subMap: Map<K, V>): Boolean {
    if (isEmpty() || subMap.isEmpty()) return false
    return subMap.all { (k, v) -> containsKey(k) && this[k] == v }
}


// Conversion methods
// -------------------------------------------------------------------------

/**
 * Converts a ResourceBundle to a Map.
 *
 * @return HashMap
 * @author K
 * @since 1.0.0
 */
fun ResourceBundle.toMap(): MutableMap<String, Any?> =
    keySet().associateWithTo(mutableMapOf()) { getObject(it) }


//region Printing

/**
 * Prints the contents of the given Map line by line.
 * Produces a nicely-formatted string description of the Map.
 * Each Map entry prints its key and value. When a value is itself a Map, this behavior recurses.
 * This method is not thread-safe. You must synchronize on the class or stream manually.
 *
 * @param out   the stream to print to
 * @param label the label to use; may be null. When null no label is printed. Often represents a bean (or similar) property name.
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.verbosePrint(out: PrintStream, label: Any?)
    = verbosePrintInternal(out, label, this, mutableListOf(), debug = false)

/**
 * Prints the contents of the given Map line by line.
 * Produces a nicely-formatted string description of the Map.
 * Each Map entry prints its key, value, and class name. When a value is itself a Map, this behavior recurses.
 * This method is not thread-safe. You must synchronize on the class or stream manually.
 *
 * @param out   the stream to print to
 * @param label the label to use; may be null. When null no label is printed. Often represents a bean (or similar) property name.
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.debugPrint(out: PrintStream, label: Any?)
    = verbosePrintInternal(out, label, this, mutableListOf(), debug = true)

//endregion Printing

/**
 * Inverts a Map. Returns a new HashMap whose keys and values are swapped from the given Map.
 * This method assumes the Map to be inverted is well-defined. If the input map has multiple
 * entries with the same value mapped to different keys, the returned map will only map one
 * such key to the value, but which key is chosen is unspecified.
 *
 * @param K Key
 * @param V Value
 * @return a new HashMap containing the inverted data
 * @author K
 * @since 1.0.0
 */
fun <K, V> Map<K, V>.invertMap(): Map<V, K> =
    entries.associate { (k, v) -> v to k }


/**
 * Recursively prints a Map structure.
 *
 * Prints the contents of a Map recursively in a tree structure, with support for nested Maps and cyclic reference detection.
 *
 * Workflow:
 * 1. Null handling: if map is null, print "null" and return immediately.
 * 2. Print label: if a label is provided, print the label followed by an equals sign.
 * 3. Begin printing: print the opening brace and start the current Map's contents.
 * 4. Push to lineage: add the current Map to lineage for cyclic reference detection.
 * 5. Iterate entries:
 *    - If value is a Map and not in lineage: print recursively.
 *    - If value is a Map but already in lineage: print a cyclic reference marker.
 *    - If value is not a Map: print the value (with type info in debug mode).
 * 6. Pop from lineage: remove the current Map from lineage after printing.
 * 7. End printing: print the closing brace, ending the current Map.
 *
 * Cyclic reference detection:
 * - Uses the lineage list to track visited Maps.
 * - If a value is a Map already in lineage, a cyclic reference has occurred.
 * - Prints "(this Map)" to indicate it points to itself.
 * - Prints "(ancestor[idx] Map)" to indicate it points to an ancestor Map.
 *
 * Indentation:
 * - The indent level is determined by lineage.size.
 * - Each level indents by 2 spaces.
 * - Produces a tree-shaped visual layout.
 *
 * Debug mode:
 * - If debug=true, the type information of each value is also printed.
 * - Format: value (type name)
 * - Useful for debugging and type checking.
 *
 * Output format:
 * ```
 * label = {
 *   key1 => value1
 *   key2 => {
 *     nestedKey => nestedValue
 *   }
 *   key3 => (this Map)  // cyclic reference
 * }
 * ```
 *
 * Notes:
 * - Uses lineage to avoid infinite recursion.
 * - Recursion depth is unbounded but is in practice limited by the JVM stack depth.
 * - Cyclic references are detected and marked; they will not cause stack overflow.
 *
 * @param out output stream used for printing
 * @param label label for this print invocation, used to identify the current Map; may be null
 * @param map Map to print; may be null
 * @param lineage list of ancestor Maps already entered into recursive printing, used to detect cyclic references
 * @param debug if true, prints the type name of each non-Map value alongside it; otherwise prints only the value itself
 */
private fun verbosePrintInternal(
    out: PrintStream,
    label: Any?,
    map: Map<*, *>?,
    lineage: MutableList<Map<*, *>>,
    debug: Boolean
) {
    // The number of indent spaces is decided by the current recursion depth, two spaces per level.
    fun printIndent(level: Int) {
        repeat(level) { out.print("  ") }
    }

    // 1. First print the label and check whether the map is null.
    printIndent(lineage.size)
    if (map == null) {
        // If the Map itself is null, simply print "label = null" or "null".
        if (label != null) {
            out.print(label)
            out.print(" = ")
        }
        out.println("null")
        return
    }

    // 2. If a label was passed and the map is not null, print "label =" first.
    if (label != null) {
        printIndent(lineage.size)
        out.print(label)
        out.println(" = ")
    }

    // 3. Open the brace and start printing the current Map's contents.
    printIndent(lineage.size)
    out.println("{")

    // 4. Add the current map to lineage so that cyclic references can be detected later.
    lineage.add(map)

    // 5. Iterate over each entry of the map.
    for ((childKey, childValue) in map.entries) {
        // If the value is also a Map and is not in the ancestry, print it recursively.
        if (childValue is Map<*, *> && !lineage.contains(childValue)) {
            // Recursive call: pass childKey as the label of the next level and childValue as the Map to print at that level.
            verbosePrintInternal(
                out,
                childKey ?: "null",
                childValue,
                lineage,
                debug
            )
        } else {
            // Otherwise either the value is not a Map, or it is a Map already in lineage (cyclic reference).
            printIndent(lineage.size)

            // Print the key
            out.print(childKey)

            // Print the separator " => "
            out.print(" => ")

            if (childValue is Map<*, *>) {
                // If childValue is a Map already in lineage, a cyclic reference has occurred.
                val idx = lineage.indexOf(childValue)
                if (idx == 0) {
                    // Same level (exactly the map passed in) indicates it points to itself.
                    out.println("(this Map)")
                } else {
                    // Otherwise it points to some level in the ancestor chain.
                    out.println("(ancestor[$idx] Map)")
                }
            } else {
                // Ordinary values or null fall through here.
                out.print(childValue)
                if (debug && childValue != null) {
                    // In debug mode, also output the type information.
                    out.print(" (${childValue.javaClass.name})")
                }
                out.println()
            }
        }
    }

    // 6. After printing all entries of the current Map, remove it from lineage.
    lineage.removeLast()

    // 7. Indent and print the closing brace, ending this level.
    printIndent(lineage.size)
    out.println("}")
}
