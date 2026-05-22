package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * 关系型数据库的表对象元数据。由 [io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit] 反射式从
 * JDBC `DatabaseMetaData` 读出来填充；不直接持有数据，仅描述结构。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Table {
    /** 表名（不含 schema / catalog 前缀）。 */
    var name: String? = null

    /** 表的注释（数据库 `REMARKS`），数据库或驱动不返回时为 `null`。 */
    var comment: String? = null

    /** 所属 schema；不同数据库语义不同（PostgreSQL 是 schema，MySQL 通常为空）。 */
    var schema: String? = null

    /** 所属 catalog；MySQL 里 catalog 即 database 名，PostgreSQL 通常为空。 */
    var catalog: String? = null

    /** 表类型枚举（普通表 / 视图 / 系统表 / 临时表 ...）。 */
    var type: TableTypeEnum? = null

    /** 调试友好的字符串展示，按字段顺序拼接 —— 不要在生产日志中依赖这种格式。 */
    override fun toString(): String {
        return "Table{" +
                "name='" + name + '\'' +
                ", comment='" + comment + '\'' +
                ", schema='" + schema + '\'' +
                ", catalog='" + catalog + '\'' +
                ", type=" + type +
                '}'
    }
}
