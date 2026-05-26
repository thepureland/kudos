package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.lang.string.humpToUnderscore
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolution and caching between Kotlin entity property names and Ktorm [Column]s.
 *
 * When DAOs / query helpers build SELECT / WHERE / ORDER BY, they only hold property names
 * (e.g. `userId`) and must resolve them back to actual [Column]s on the Ktorm table. The lookup
 * order is:
 *  1. camelCase to lowercase underscore (`userId` → `user_id`), then `table[name]`
 *  2. uppercase the result and try again (compatible with H2 / Oracle's default uppercase naming)
 *  3. iterate `table.columns` and compare column name or property name case-insensitively
 *  4. when the property name equals `id`, fall back to `table.primaryKeys[0]`
 *
 * Resolved results are cached by table name + property name into a [ConcurrentHashMap] to avoid
 * repeated reflection on every DAO call.
 *
 * Concurrency: [columnOf] is invoked concurrently on DAO query paths, so cache reads and writes
 * must be thread-safe.
 * Locale: case conversions use [Locale.ROOT] to avoid `"i" → "İ"` issues under e.g. Turkish locale
 * that would otherwise break column name resolution.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object ColumnHelper {

    /**
     * Column info cache Map(table name, Map(property name, column)).
     * Uses [ConcurrentHashMap] to guarantee thread-safe read/write under multi-threaded access.
     */
    private val columnCache: ConcurrentHashMap<String, ConcurrentHashMap<String, Column<Any>>> = ConcurrentHashMap()

    /**
     * Resolve columns by property names.
     *
     * @param table ktorm table object
     * @param propertyNames varargs of property names
     * @return Map(property name, column)
     */
    fun columnOf(table: BaseTable<*>, vararg propertyNames: String): Map<String, Column<Any>> {
        if (propertyNames.isEmpty()) return emptyMap()

        val tableName = table.tableName
        val columnMap = columnCache.computeIfAbsent(tableName) { ConcurrentHashMap() }

        val resultMap = linkedMapOf<String, Column<Any>>()
        propertyNames.forEach { propertyName ->
            val cached = columnMap[propertyName]
            if (cached != null) {
                resultMap[propertyName] = cached
            } else {
                val resolved = resolveColumn(table, propertyName)
                    ?: error("Cannot resolve the column name for property [${propertyName}] in table [${tableName}]!")
                @Suppress("UNCHECKED_CAST")
                val typed = resolved as Column<Any>
                resultMap[propertyName] = typed
                columnMap[propertyName] = typed
            }
        }
        return resultMap
    }

    /**
     * Resolve a single property name to a Ktorm [Column]; returns null if not found.
     * See the class-level KDoc for the multi-step fallback order.
     */
    private fun resolveColumn(table: BaseTable<*>, propertyName: String): Column<*>? {
        val lowerName = propertyName.humpToUnderscore(false)
        var column: Column<*>? = try {
            table[lowerName]
        } catch (_: NoSuchElementException) {
            null
        }
        if (column == null) {
            val upperName = lowerName.uppercase(Locale.ROOT)
            column = try {
                table[upperName]
            } catch (_: NoSuchElementException) {
                table.columns.firstOrNull {
                    it.name.equals(upperName, true) || it.name.equals(propertyName, true)
                }
            }
        }
        if (column == null && propertyName == "id") {
            column = table.primaryKeys.firstOrNull()
        }
        return column
    }

}
