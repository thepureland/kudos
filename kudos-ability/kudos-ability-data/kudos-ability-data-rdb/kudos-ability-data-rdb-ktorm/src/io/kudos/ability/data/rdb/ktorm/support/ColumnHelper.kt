package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.lang.string.humpToUnderscore
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Kotlin 实体属性名 ↔ Ktorm [Column] 解析与缓存。
 *
 * DAO / 查询封装在拼 SELECT / WHERE / ORDER BY 时只持有属性名（如 `userId`），
 * 需要换回 Ktorm 表对象上的实际 [Column]。本工具按以下顺序查找：
 *  1. 驼峰转下划线小写（`userId` → `user_id`）走 `table[name]`
 *  2. 转大写再试一次（兼容 H2 / Oracle 默认大写命名）
 *  3. 遍历 `table.columns` 用大小写不敏感对比列名或属性名
 *  4. 属性名等于 `id` 时回落到 `table.primaryKeys[0]`
 *
 * 命中结果按表名 + 属性名缓存到 [ConcurrentHashMap]，避免每次 DAO 调用都重复反射。
 *
 * 并发：[columnOf] 在 DAO 查询路径上会被多线程并发触发，缓存读写必须线程安全。
 * Locale：大小写转换走 [Locale.ROOT]，规避 Turkish locale 等环境下 `"i" → "İ"`
 * 引发的列名解析失败。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object ColumnHelper {

    /**
     * 列信息缓存 Map(表名，Map(属性名, 列对象))。
     * 使用 [ConcurrentHashMap] 保证多线程下读写安全。
     */
    private val columnCache: ConcurrentHashMap<String, ConcurrentHashMap<String, Column<Any>>> = ConcurrentHashMap()

    /**
     * 根据属性名得到列对象
     *
     * @param table ktorm表对象
     * @param propertyNames 属性名可变数组
     * @return Map(属性名，列对象)
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
                    ?: error("无法推测属性【${propertyName}】在表【${tableName}】中的字段名！")
                @Suppress("UNCHECKED_CAST")
                val typed = resolved as Column<Any>
                resultMap[propertyName] = typed
                columnMap[propertyName] = typed
            }
        }
        return resultMap
    }

    /**
     * 解析单个属性名 → Ktorm [Column]；找不到返回 null。
     * 多步回退见类级 kdoc。
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
