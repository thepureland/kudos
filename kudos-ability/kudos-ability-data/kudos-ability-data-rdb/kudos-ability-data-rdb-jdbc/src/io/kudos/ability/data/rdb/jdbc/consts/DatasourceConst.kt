package io.kudos.ability.data.rdb.jdbc.consts

/**
 * Collection of literal constants related to data sources.
 *
 * Using an interface + companion is the traditional way to package singleton constants in Kotlin;
 * external code can reference them directly as `DatasourceConst.MODE_MASTER`. When adding new
 * constants, please keep the "string literal + kdoc describing the semantics" pattern.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface DatasourceConst {
    companion object {

        /** Marker for master mode. Master mode handles writes + strongly consistent reads and is the default suffix for the data source key. */
        const val MODE_MASTER: String = "master"

        /** Marker for read-only replica mode. The read-only path is appended as a data source key suffix; triggered by `DbParam.readonly=true`. */
        const val MODE_READONLY: String = "readonly"

        /** Conventional "console tenant" id. Represents the global administrator identity and bypasses normal tenant-level data source routing (goes directly to the master). */
        const val CONSOLE_TENANT_ID: String = "-99"
    }
}
