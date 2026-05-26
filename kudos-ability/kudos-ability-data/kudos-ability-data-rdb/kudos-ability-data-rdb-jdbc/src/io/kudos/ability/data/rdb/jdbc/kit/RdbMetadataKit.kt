package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.*
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.util.Locale

/**
 * Utility class for relational database metadata.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object RdbMetadataKit {

    /**
     * Returns all table information matching the given table types.
     *
     * @param tableTypes vararg of table type enums
     * @param conn database connection. When null, a new connection is created from
     *   the current context data source and closed after use. When non-null, the
     *   caller is responsible for closing the connection.
     * @return List of table info objects
     * @author K
     * @since 1.0.0
     */
    fun getTablesByType(vararg tableTypes: TableTypeEnum?, conn: Connection? = null): List<Table> =
        withConn(conn) { _getTablesByType(it, *tableTypes) }

    /**
     * Returns the table info for the given table name.
     *
     * @param tableName table name
     * @param conn database connection. When null, a new connection is created from
     *   the current context data source and closed after use. When non-null, the
     *   caller is responsible for closing the connection.
     * @return table info object; null if not found
     * @author K
     * @since 1.0.0
     */
    fun getTableByName(tableName: String, conn: Connection? = null): Table? =
        withConn(conn) { _getTableByName(it, tableName) }

    /**
     * Returns all column info for the given table name.
     *
     * @param tableName table name
     * @param conn database connection. When null, a new connection is created from
     *   the current context data source and closed after use. When non-null, the
     *   caller is responsible for closing the connection.
     * @return Map of (column name, column info object)
     * @author K
     * @since 1.0.0
     */
    fun getColumnsByTableName(tableName: String, conn: Connection? = null): Map<String, Column> =
        withConn(conn) { _getColumnsByTableName(it, tableName) }

    private inline fun <T> withConn(conn: Connection?, action: (Connection) -> T): T =
        if (conn != null) action(conn) else RdbKit.getDataSource().connection.use(action)

    /**
     * Internal implementation: uses JDBC [DatabaseMetaData.getTables] to fetch all
     * tables of matching types and assembles them into a [Table] list. Maps
     * `"BASE TABLE"` (returned by some JDBC drivers) back to the standard `TABLE`.
     */
    private fun _getTablesByType(conn: Connection, vararg tableTypes: TableTypeEnum?): List<Table> {
        val dbMetaData = conn.metaData
        val types = tableTypes.map { checkNotNull(it) { "table type must not be null" }.name }.toTypedArray()
        val talbes = mutableListOf<Table>()
        val tableRs = dbMetaData.getTables(conn.catalog, conn.schema, "%", types)
        tableRs.use {
            while (tableRs.next()) {
                val rawType = tableRs.getString("TABLE_TYPE")
                val tableTypeStr = if (rawType == "BASE TABLE") "TABLE" else rawType
                talbes.add(Table().apply {
                    name = tableRs.getString("TABLE_NAME")
                    catalog = tableRs.getString("TABLE_CAT")
                    schema = tableRs.getString("TABLE_SCHEM")
                    comment = tableRs.getString("REMARKS") ?: ""
                    type = TableTypeEnum.valueOf(tableTypeStr)
                })
            }
        }
        return talbes
    }

    /**
     * Internal implementation: looks up the table by name via JDBC
     * [DatabaseMetaData.getTables]; returns the first row or null.
     */
    private fun _getTableByName(conn: Connection, tableName: String): Table? {
        val dbMetaData = conn.metaData
        val rs = dbMetaData.getTables(conn.catalog, conn.schema, tableName, null)
        rs.use {
            if (!rs.next()) return null
            val rawType = rs.getString("TABLE_TYPE")
            val tableTypeStr = if (rawType == "BASE TABLE") "TABLE" else rawType
            return Table().apply {
                name = tableName
                catalog = rs.getString("TABLE_CAT")
                schema = rs.getString("TABLE_SCHEM")
                comment = rs.getString("REMARKS") ?: ""
                type = TableTypeEnum.valueOf(tableTypeStr)
            }
        }
    }

    /**
     * Internal implementation: runs 5 queries via JDBC `DatabaseMetaData` (columns,
     * primary keys, foreign keys, indexes, unique constraints) and aggregates the
     * results into a [linkedMapOf] keyed by column name (preserving the order
     * returned by the DB). Convention: columns whose names end with `__CODE` are
     * marked `dictCode=true` (kudos dictionary-code column convention).
     *
     * Note: the 5 consecutive `ResultSet.use{}` blocks can be costly for some JDBC
     * drivers / large tables; one-off calls in code-generation scenarios are fine,
     * but hot paths should cache the result.
     */
    private fun _getColumnsByTableName(conn: Connection, tableName: String): Map<String, Column> {
        val dbMetaData = conn.metaData
        val rdbType = RdbTypeEnum.ofProductName(dbMetaData.databaseProductName)
        val linkedMap = linkedMapOf<String, Column>()

        // Fetch all columns.
        val columnRs = dbMetaData.getColumns(conn.catalog, conn.schema, tableName, null)
        columnRs.use {
            while (columnRs.next()) {
                val column = Column().apply {
                    name = columnRs.getString("COLUMN_NAME")
                    comment = columnRs.getString("REMARKS") ?: ""
                    jdbcType = columnRs.getInt("DATA_TYPE")
                    jdbcTypeName = columnRs.getString("TYPE_NAME")
                    kotlinType = JdbcTypeToKotlinType.getKotlinType(rdbType, this)
                    length = columnRs.getInt("COLUMN_SIZE")
                    decimalDigits = columnRs.getInt("DECIMAL_DIGITS")
                    defaultValue = columnRs.getString("COLUMN_DEF")
                    nullable = columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                    dictCode = name.uppercase(Locale.getDefault()).endsWith("__CODE")
                    autoIncrement = columnRs.getString("IS_AUTOINCREMENT")
                }
                linkedMap[column.name] = column
            }
        }

        // Primary keys.
        val primaryKeyRs = dbMetaData.getPrimaryKeys(conn.catalog, conn.schema, tableName)
        primaryKeyRs.use {
            while (primaryKeyRs.next()) {
                val columnName = primaryKeyRs.getString("COLUMN_NAME")
                linkedMap.getValue(columnName).primaryKey = true
            }
        }

        // Foreign keys.
        val foreignKeyRs = dbMetaData.getImportedKeys(conn.catalog, conn.schema, tableName)
        foreignKeyRs.use {
            while (foreignKeyRs.next()) {
                val columnName = foreignKeyRs.getString("FKCOLUMN_NAME")
                linkedMap.getValue(columnName).foreignKey = true
            }
        }

        // Indexes.
        val indexInfoRs = dbMetaData.getIndexInfo(conn.catalog, conn.schema, tableName, false, false)
        indexInfoRs.use {
            while (indexInfoRs.next()) {
                val columnName = indexInfoRs.getString("COLUMN_NAME")
                linkedMap.getValue(columnName).indexed = true
            }
        }


        // Unique constraints.
        val uniqueInfoRs = dbMetaData.getIndexInfo(conn.catalog, conn.schema, tableName, true, false)
        uniqueInfoRs.use {
            while (uniqueInfoRs.next()) {
                val columnName = uniqueInfoRs.getString("COLUMN_NAME")
                linkedMap.getValue(columnName).unique = true
            }
        }

        return linkedMap
    }

}