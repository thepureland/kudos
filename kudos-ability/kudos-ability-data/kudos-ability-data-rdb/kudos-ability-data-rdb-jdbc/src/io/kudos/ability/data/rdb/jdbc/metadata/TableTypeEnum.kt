package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * Enumeration of relational database table types.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class TableTypeEnum {

    /**
     * Regular table.
     */
    TABLE,

    /**
     * View.
     */
    VIEW,

    /**
     * System table.
     */
    SYSTEM_TABLE,

    /**
     * Global temporary table.
     */
    GLOBAL_TEMPORARY,

    /**
     * Local temporary table.
     */
    LOCAL_TEMPORARY,

    /**
     * Alias.
     */
    ALIAS,

    /**
     * Synonym.
     */
    SYNONYM
}
