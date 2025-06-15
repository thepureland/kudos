package io.kudos.ability.data.rdb.jdbc.kit

import org.soul.ability.data.rdb.jdbc.metadata.Column
import org.soul.ability.data.rdb.jdbc.metadata.RdbMetadataTool
import org.soul.ability.data.rdb.jdbc.metadata.Table
import org.soul.ability.data.rdb.jdbc.metadata.TableTypeEnum
import java.sql.Connection

/**
 * 关系型数据库元数据工具类
 *
 * @author K
 * @since 1.0.0
 */
object RdbMetadataKit {

    /**
     * 根据表类型取得所有表信息
     *
     * @param tableTypes 表类型枚举的可变数组
     * @param conn 数据库连接。为null将用当前上下文数据源新建一个连接，在使用完关掉。不为null时由用户自行处理连接的关闭。
     * @return List(表对象信息)
     * @author K
     * @since 1.0.0
     */
    fun getTablesByType(vararg tableTypes: TableTypeEnum?, conn: Connection? = null): List<Table> =
        RdbMetadataTool.getTablesByType(tableTypes, conn)

    /**
     * 根据表名取得对应表信息
     *
     * @param tableName 表名
     * @param conn 数据库连接。为null将用当前上下文数据源新建一个连接，在使用完关掉。不为null时由用户自行处理连接的关闭。
     * @return 表对象信息，找不到是返回null
     * @author K
     * @since 1.0.0
     */
    fun getTableByName(tableName: String, conn: Connection? = null): Table? =
        RdbMetadataTool.getTableByName(tableName, conn)

    /**
     * 根据表名取得对应表的所有列信息
     *
     * @param tableName 表名
     * @param conn 数据库连接。为null将用当前上下文数据源新建一个连接，在使用完关掉。不为null时由用户自行处理连接的关闭。
     * @return Map(列名, 列对象信息)
     * @author K
     * @since 1.0.0
     */
    fun getColumnsByTableName(tableName: String, conn: Connection? = null): Map<String, Column> =
        RdbMetadataTool.getColumnsByTableName(tableName, conn)

}