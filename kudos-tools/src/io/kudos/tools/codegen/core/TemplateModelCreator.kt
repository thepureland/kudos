package io.kudos.tools.codegen.core

import io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit
import io.kudos.ability.data.rdb.jdbc.metadata.Column
import io.kudos.ability.data.rdb.ktorm.support.*
import io.kudos.base.bean.BeanKit
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.lang.string.capitalizeString
import io.kudos.base.lang.string.humpToUnderscore
import io.kudos.base.lang.string.underscoreToHump
import io.kudos.tools.codegen.model.vo.Config

/**
 * Template data-model creator. Users may subclass to customize the data fed into the templates.
 *
 * @author K
 * @since 1.0.0
 */
open class TemplateModelCreator {

    /**
     * Builds the "base model" that is independent of any specific table: package prefix, module name, author,
     * version and other template placeholders that are common across tables. This part can be reused when
     * generating multiple tables in a batch.
     *
     * @return Model map that can be further appended
     * @author K
     * @since 1.0.0
     */
    fun createBaseModel(): MutableMap<String, Any?> {
        val templateModel = mutableMapOf<String, Any?>()
        val config = CodeGeneratorContext.config
        templateModel["project"] = config.getTemplateInfo()!!.name
        templateModel[Config.PROP_KEY_PACKAGE_PREFIX] = config.getPackagePrefix()
        templateModel[Config.PROP_KEY_MODULE_NAME] = config.getModuleName()
        templateModel["moduleCapitalize"] = config.getModuleName().capitalizeString()
        templateModel[Config.PROP_KEY_AUTHOR] = config.getAuthor()
        templateModel[Config.PROP_KEY_VERSION] = config.getVersion()
        return templateModel
    }

    /**
     * Public entry point for the template-population model: merges the base model with the table-specific model.
     *
     * @return Full map needed for template rendering
     * @author K
     * @since 1.0.0
     */
    fun create(): Map<String, Any?> {
        val templateBaseModel = createBaseModel()
        val entityRelativeModel = createEntityRelativeModel()
        return templateBaseModel + entityRelativeModel
    }

    /**
     * Builds the table-specific model:
     * - Entity name (camelCase), short name (module prefix stripped), table structure, all columns
     * - Splits columns into five groups — search/list/edit/detail/cache — according to UI selections
     * - Calls [determinePoDaoSuperClass] to decide the PO/DAO parent classes
     * - Calls [initOtherParameters] to compute import flags per column type and `serialVersionUID`
     *
     * @return Entity-related fragment of the model
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    fun createEntityRelativeModel(): MutableMap<String, Any?> {
        val templateModel = mutableMapOf<String, Any?>()
        val tableName = CodeGeneratorContext.tableName
        val config = CodeGeneratorContext.config
        val columns = CodeGeneratorContext.columns

        val entityName = tableName.underscoreToHump().capitalizeString()
        templateModel["entityName"] = entityName
        val shortEntityName = entityName.replaceFirst(config.getModuleName(), "", true)
        templateModel["shortEntityName"] = shortEntityName
        templateModel["lowerShortEntityName"] = shortEntityName.lowercase()
        // First-level package directory. Not derivable from the table name — see CodeGeneratorContext.bizModule.
        templateModel["bizModule"] = CodeGeneratorContext.bizModule
            .ifBlank { defaultBizModule(tableName, config.getModuleName()) }
        templateModel["table"] = RdbMetadataKit.getTableByName(tableName)
        val origColumns = RdbMetadataKit.getColumnsByTableName(tableName).values
        templateModel["columns"] = origColumns
        // Resolve every column before failing, so an unsupported type names the column it is on and
        // lists all of them at once. Asking KtormSqlType.getFunName per column instead would abort
        // on the first one with a message naming only the Kotlin type — and since the JDBC-to-Kotlin
        // mapping falls back to types Ktorm has no column function for (OffsetDateTime for
        // PostgreSQL `timestamptz`, Duration for `interval`, Any for the geometric types), that is a
        // realistic table, not a corner case.
        val unmappedColumns = origColumns.filter { KtormSqlType.getFunNameOrNull(it.kotlinType) == null }
        require(unmappedColumns.isEmpty()) {
            "Table [$tableName] has ${unmappedColumns.size} column(s) whose type has no Ktorm column " +
                "binding, so no table definition can be generated for it: " +
                unmappedColumns.joinToString { "${it.name} (${it.kotlinType.qualifiedName})" } +
                ". Map these columns to a supported type, or exclude the table from generation and " +
                "hand-write it with a custom SqlType."
        }
        templateModel["ktormFunNameMap"] = origColumns.associate { it.name to KtormSqlType.getFunName(it.kotlinType) }
        templateModel["pkColumn"] = origColumns.first { it.primaryKey }
        val columnConfMap = columns.associateBy { it.getColumn() }

        // Search items
        val searchItemColumns = mutableListOf<Column>()
        templateModel["searchItemColumns"] = searchItemColumns
        // List items
        val listItemColumns = mutableListOf<Column>()
        templateModel["listItemColumns"] = listItemColumns
        // Edit items
        val editItemColumns = mutableListOf<Column>()
        templateModel["editItemColumns"] = editItemColumns
        // Detail items
        val detailItemColumns = mutableListOf<Column>()
        templateModel["detailItemColumns"] = detailItemColumns
        // Cache items
        val cacheItemColumns = mutableListOf<Column>()
        templateModel["cacheItemColumns"] = cacheItemColumns

        for (origColumn in origColumns) {
            val columnName = origColumn.name.lowercase()
            val columnInfo = columnConfMap[columnName]
            if (columnInfo != null) {
                BeanKit.copyProperties(columnInfo, origColumn)
                origColumn.comment = columnInfo.getComment()

                if (columnInfo.getSearchItem()) {
                    searchItemColumns.add(origColumn)
                }
                if (columnInfo.getListItem()) {
                    listItemColumns.add(origColumn)
                }
                if (columnInfo.getEditItem()) {
                    editItemColumns.add(origColumn)
                }
                if (columnInfo.getDetailItem()) {
                    detailItemColumns.add(origColumn)
                }
                if (columnInfo.getCacheItem()) {
                    cacheItemColumns.add(origColumn)
                }
            }
        }

        determinePoDaoSuperClass(templateModel, origColumns)
        initOtherParameters(templateModel, templateModel["columns"] as Collection<Column>)
        return templateModel
    }

    /**
     * Selects parent classes for the PO and DAO (Ktorm Table) based on the primary-key Kotlin type.
     *
     * - String primary key with all maintenance fields (createTime/updateTime/active/builtIn, etc.) ->
     *   `IManagedDbEntity` + `ManagedTable`, and the maintenance fields are filtered out of the template
     *   `columns` (since the parent class already declares them).
     * - String primary key without maintenance fields -> `StringIdTable`; the id column is filtered out.
     * - Int / Long primary key -> the corresponding `IntIdTable` / `LongIdTable`; the id column is filtered out.
     * - Other types -> the plain `Table`; columns are not filtered.
     *
     * @param templateModel Upper-level template model; this method writes `poSuperClass`/`daoSuperClass` and may rewrite `columns`
     * @param origColumns Original DB column collection (including id / maintenance fields)
     * @author K
     * @since 1.0.0
     */
    open fun determinePoDaoSuperClass(templateModel: MutableMap<String, Any?>, origColumns: Collection<Column>) {
        val pkKotlinType = origColumns.first { it.primaryKey }.kotlinType
        var poSuperClass = IDbEntity::class.simpleName
        lateinit var daoSuperClass: String
        when (pkKotlinType) {
            String::class -> {
                // Every property ManagedTable already declares, mapped to its snake_case database column name.
                // The conversion has to be applied uniformly: matching some of them by the camelCase property
                // name (createUserId vs. create_user_id) would make `containsAll` fail on every real table, so
                // ManagedTable/IManagedDbEntity would never be selected. The user-name columns belong here too,
                // otherwise they are re-declared in the generated PO/table and shadow the parent's.
                val maintainColumns = listOf(
                    ManagedTable<*>::id,
                    ManagedTable<*>::createTime,
                    ManagedTable<*>::createUserId,
                    ManagedTable<*>::createUserName,
                    ManagedTable<*>::updateTime,
                    ManagedTable<*>::updateUserId,
                    ManagedTable<*>::updateUserName,
                    ManagedTable<*>::active,
                    ManagedTable<*>::builtIn,
                    ManagedTable<*>::remark,
                ).map { it.name.humpToUnderscore(false) }
                if (origColumns.map { it.name }.containsAll(maintainColumns)) {
                    // All maintenance fields are present; the PO implements IManagedDbEntity and the DAO extends ManagedTable.
                    poSuperClass = IManagedDbEntity::class.simpleName
                    daoSuperClass = requireNotNull(ManagedTable::class.simpleName) { "ManagedTable simpleName is null" }
                    // Filter out columns that the parent class already provides.
                    templateModel["columns"] = origColumns.filter { !maintainColumns.contains(it.name) }
                } else {
                    daoSuperClass = requireNotNull(StringIdTable::class.simpleName) { "StringIdTable simpleName is null" }
                    templateModel["columns"] =
                        origColumns.filter { it.name != "id" } // Filter out the id column already on the parent class
                }
            }

            Int::class -> {
                daoSuperClass = requireNotNull(IntIdTable::class.simpleName) { "IntIdTable simpleName is null" }
                templateModel["columns"] = origColumns.filter { it.name != "id" } // Filter out the id column already on the parent class
            }

            Long::class -> {
                daoSuperClass = requireNotNull(LongIdTable::class.simpleName) { "LongIdTable simpleName is null" }
                templateModel["columns"] = origColumns.filter { it.name != "id" } // Filter out the id column already on the parent class
            }

            else -> daoSuperClass = "Table"
        }
        templateModel["poSuperClass"] = poSuperClass
        templateModel["daoSuperClass"] = daoSuperClass
    }

    /**
     * Computes whether each column group contains specific Kotlin types so the template can do conditional imports
     * (e.g. `containsLocalDateTimeColumn` / `containsLocalDateTimeColumnInListItems`).
     *
     * The templates use these flags to decide whether `import java.time.LocalDateTime` is actually needed,
     * avoiding blind imports that would trigger lint warnings. Finally, a `serialVersionUID` is generated so each
     * PO class gets a stable, recognizable version number.
     *
     * @param templateModel Upper-level template model; this method only writes keys into it
     * @param origColumns Original column collection, used for the PO's own import decisions
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    open fun initOtherParameters(templateModel: MutableMap<String, Any?>, origColumns: Collection<Column>) {
        // Used to drive imports of non-Kotlin types inside the templates
        val kotlinTypeMap = mapOf(
            "containsLocalDateTimeColumn" to java.time.LocalDateTime::class,
            "containsLocalDateColumn" to java.time.LocalDate::class,
            "containsLocalTimeColumn" to java.time.LocalTime::class,
            "containsBlobColumn" to java.sql.Blob::class,
            "containsClobColumn" to java.sql.Clob::class,
            "containsBigDecimalColumn" to java.math.BigDecimal::class,
            "containsRefColumn" to java.sql.Ref::class,
            "containsRowIdColumn" to java.sql.RowId::class,
            "containsSQLXMLColumn" to java.sql.SQLXML::class
        )

        // In the PO
        for ((key, value) in kotlinTypeMap)
            templateModel[key] = origColumns.any { it.kotlinType == value }

        // In the search payload class
        val searchItemColumns = templateModel["searchItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InSearchItems"] = searchItemColumns.any { it.kotlinType == value }

        // In the list-record class
        val listItemColumns = templateModel["listItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InListItems"] = listItemColumns.any { it.kotlinType == value }

        // In the edit payload class
        val editItemColumns = templateModel["editItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InEditItems"] = editItemColumns.any { it.kotlinType == value }

        // In the detail class
        val detailItemColumns = templateModel["detailItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InDetailItems"] = detailItemColumns.any { it.kotlinType == value }

        // In the cache-item class
        val cacheItemColumns = templateModel["cacheItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InCacheItems"] = cacheItemColumns.any { it.kotlinType == value }

        // Whether the cache items contain an id
        templateModel["containsIdColumnInCacheItems"] = cacheItemColumns.any { it.name.equals("id", true) }

        // serialVersionUID
        templateModel["serialVersionUID"] = RandomStringKit.randomLong() + "L"
    }

    companion object {

        /**
         * Default business module for [tableName]: the lowercased short entity name
         * (`sys_param` + module `sys` -> `param`).
         *
         * Correct only for the *primary* table of a business module. Secondary tables
         * (`sys_dict_item` -> `dictitem`, `user_account_third` -> `accountthird`) must be
         * corrected in the wizard, because the real grouping is a human decision — `user_org_user`
         * lives under `account`, not `orguser` nor `org`.
         *
         * @param tableName database table name
         * @param moduleName atomic service name, stripped from the front of the entity name
         * @return the default first-level package directory
         * @author K
         * @since 1.0.0
         */
        fun defaultBizModule(tableName: String, moduleName: String): String =
            tableName.underscoreToHump().capitalizeString().replaceFirst(moduleName, "", true).lowercase()

    }

}