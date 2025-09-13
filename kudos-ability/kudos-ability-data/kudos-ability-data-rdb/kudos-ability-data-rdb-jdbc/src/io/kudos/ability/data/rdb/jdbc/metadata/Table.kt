package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * 关系型数据库表对象信息
 *
 * @author K
 * @since 1.0.0
 */
class Table {
    /**
     * 表名
     */
    var name: String? = null

    /**
     * 注释
     */
    var comment: String? = null

    /**
     * 所属Schema
     */
    var schema: String? = null

    /**
     * 所属Catalog
     */
    var catalog: String? = null

    /**
     * 表类型
     */
    var type: TableTypeEnum? = null

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
