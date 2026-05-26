package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * Metadata of a table object in a relational database. Populated reflectively from JDBC
 * `DatabaseMetaData` by [io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit]; does not hold data
 * directly, only describes structure.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Table {
    /** Table name (without schema / catalog prefix). */
    var name: String? = null

    /** Table comment (database `REMARKS`); `null` when the database or driver does not return one. */
    var comment: String? = null

    /** Owning schema; semantics vary by database (in PostgreSQL it is the schema, in MySQL it is usually empty). */
    var schema: String? = null

    /** Owning catalog; in MySQL the catalog is the database name, in PostgreSQL it is usually empty. */
    var catalog: String? = null

    /** Table type enum (regular table / view / system table / temporary table ...). */
    var type: TableTypeEnum? = null

    /** Debug-friendly string representation, concatenated in field order — do not rely on this format in production logs. */
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
