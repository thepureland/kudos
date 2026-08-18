package io.kudos.tools.codegen.core

import io.kudos.ability.data.rdb.jdbc.metadata.Column
import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.model.vo.ColumnInfo
import kotlin.reflect.KClass
import kotlin.test.*

/**
 * test for TemplateModelCreator
 *
 * Covers:
 * - createBaseModel: all table-independent keys, including capitalized module name.
 * - determinePoDaoSuperClass: all five branches (String PK with full maintenance columns,
 *   String PK without, Int PK, Long PK, other PK type) plus the no-primary-key error path.
 * - initOtherParameters: type-presence flags for PO / search / list / edit / detail / cache groups,
 *   id-in-cache-items detection (case-insensitive) and serialVersionUID format.
 * - createEntityRelativeModel + create(): full table-driven model against an in-memory H2,
 *   including column grouping by ColumnInfo flags and custom comment override.
 *
 * @author K
 * @since 1.0.0
 */
internal class TemplateModelCreatorTest {

    private val creator = TemplateModelCreator()

    //region createBaseModel

    @Test
    fun createBaseModelExposesAllCommonKeys() {
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(
            templateName = "kudos", packagePrefix = "io.kudos.ms", moduleName = "user",
            author = "K", version = "1.0.0"
        )
        val model = creator.createBaseModel()
        assertEquals("kudos", model["project"])
        assertEquals("io.kudos.ms", model["packagePrefix"])
        assertEquals("user", model["module"])
        assertEquals("User", model["moduleCapitalize"])
        assertEquals("K", model["author"])
        assertEquals("1.0.0", model["version"])
        assertFalse(
            model.containsKey("bizModule"),
            "bizModule is table-scoped; leaking it into the base model would make non-entity templates " +
                "renderable with a stale value from a previous table"
        )
    }

    //endregion

    //region defaultBizModule

    @Test
    fun defaultBizModuleStripsTheModulePrefixAndLowercases() {
        assertEquals("param", TemplateModelCreator.defaultBizModule("sys_param", "sys"))
        assertEquals("org", TemplateModelCreator.defaultBizModule("user_org", "user"))
        // The module prefix is stripped case-insensitively, matching createEntityRelativeModel.
        assertEquals("locale", TemplateModelCreator.defaultBizModule("SYS_LOCALE", "sys"))
        // No module prefix on the table name: nothing is stripped.
        assertEquals("auditlog", TemplateModelCreator.defaultBizModule("audit_log", "sys"))
    }

    @Test
    fun defaultBizModuleIsWrongForSecondaryTablesWhichIsWhyItIsOverridable() {
        // These are exactly the cases the wizard has to correct by hand: the real directories are
        // dict / accountthird->account, and no derivation from the table name can produce them.
        assertEquals("dictitem", TemplateModelCreator.defaultBizModule("sys_dict_item", "sys"))
        assertEquals("accountthird", TemplateModelCreator.defaultBizModule("user_account_third", "user"))
    }

    @Test
    fun explicitBizModuleOverridesTheDefault() {
        CodegenTestSupport.bindNewH2(
            "tmc_biz_module",
            """CREATE TABLE IF NOT EXISTS "sys_dict_item" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "item_code" VARCHAR(64) NOT NULL
               )""",
        )
        CodeGeneratorContext.tableName = "sys_dict_item"
        CodeGeneratorContext.tableComment = "dict item"
        CodeGeneratorContext.bizModule = "dict"
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(moduleName = "sys")
        CodeGeneratorContext.columns = emptyList()
        CodeGeneratorContext.templateModelCreator = creator

        val model = creator.createEntityRelativeModel()

        assertEquals("SysDictItem", model["entityName"])
        assertEquals("dictitem", model["lowerShortEntityName"], "the derived name is still exposed")
        assertEquals("dict", model["bizModule"], "the wizard value wins over the default")

        CodeGeneratorContext.bizModule = ""
    }

    //endregion

    //region determinePoDaoSuperClass

    @Test
    fun stringPkWithAllMaintenanceColumnsUsesManagedTable() {
        // Every ManagedTable-provided column is matched by its snake_case database name, exactly as the
        // Flyway scripts declare them; all of them are filtered out of the PO/table column list.
        val columns = listOf(
            col("id", String::class, pk = true),
            col("create_time", java.time.LocalDateTime::class),
            col("create_user_id", String::class),
            col("create_user_name", String::class),
            col("update_time", java.time.LocalDateTime::class),
            col("update_user_id", String::class),
            col("update_user_name", String::class),
            col("active", Boolean::class),
            col("built_in", Boolean::class),
            col("remark", String::class),
            col("biz_field", String::class),
        )
        val model = mutableMapOf<String, Any?>()
        creator.determinePoDaoSuperClass(model, columns)

        assertEquals("IManagedDbEntity", model["poSuperClass"])
        assertEquals("ManagedTable", model["daoSuperClass"])
        @Suppress("UNCHECKED_CAST")
        val filtered = model["columns"] as List<Column>
        assertEquals(listOf("biz_field"), filtered.map { it.name })
    }

    @Test
    fun stringPkWithoutMaintenanceColumnsUsesStringIdTable() {
        val columns = listOf(col("id", String::class, pk = true), col("name", String::class))
        val model = mutableMapOf<String, Any?>()
        creator.determinePoDaoSuperClass(model, columns)

        assertEquals("IDbEntity", model["poSuperClass"])
        assertEquals("StringIdTable", model["daoSuperClass"])
        @Suppress("UNCHECKED_CAST")
        val filtered = model["columns"] as List<Column>
        assertEquals(listOf("name"), filtered.map { it.name })
    }

    @Test
    fun intPkUsesIntIdTable() {
        val columns = listOf(col("id", Int::class, pk = true), col("name", String::class))
        val model = mutableMapOf<String, Any?>()
        creator.determinePoDaoSuperClass(model, columns)

        assertEquals("IntIdTable", model["daoSuperClass"])
        @Suppress("UNCHECKED_CAST")
        val filtered = model["columns"] as List<Column>
        assertEquals(listOf("name"), filtered.map { it.name })
    }

    @Test
    fun longPkUsesLongIdTable() {
        val columns = listOf(col("id", Long::class, pk = true), col("name", String::class))
        val model = mutableMapOf<String, Any?>()
        creator.determinePoDaoSuperClass(model, columns)

        assertEquals("LongIdTable", model["daoSuperClass"])
        @Suppress("UNCHECKED_CAST")
        val filtered = model["columns"] as List<Column>
        assertEquals(listOf("name"), filtered.map { it.name })
    }

    @Test
    fun otherPkTypeUsesPlainTableAndKeepsColumnsUntouched() {
        val columns = listOf(col("id", java.util.UUID::class, pk = true), col("name", String::class))
        val model = mutableMapOf<String, Any?>("columns" to columns)
        creator.determinePoDaoSuperClass(model, columns)

        assertEquals("Table", model["daoSuperClass"])
        assertEquals("IDbEntity", model["poSuperClass"])
        assertSame(columns, model["columns"], "non-id-table branch must not rewrite the column list")
    }

    @Test
    fun missingPrimaryKeyFails() {
        assertFailsWith<NoSuchElementException> {
            creator.determinePoDaoSuperClass(mutableMapOf(), listOf(col("name", String::class)))
        }
    }

    //endregion

    //region initOtherParameters

    @Test
    fun initOtherParametersComputesTypeFlagsPerGroup() {
        val timeCol = col("birth_time", java.time.LocalDateTime::class)
        val blobCol = col("payload", java.sql.Blob::class)
        val decimalCol = col("salary", java.math.BigDecimal::class)
        val idCol = col("ID", String::class)
        val model = mutableMapOf<String, Any?>(
            "searchItemColumns" to listOf(timeCol),
            "listItemColumns" to listOf(decimalCol),
            "editItemColumns" to emptyList<Column>(),
            "detailItemColumns" to listOf(timeCol, blobCol),
            "cacheItemColumns" to listOf(idCol),
        )
        creator.initOtherParameters(model, listOf(timeCol, blobCol, decimalCol, idCol))

        // PO level
        assertEquals(true, model["containsLocalDateTimeColumn"])
        assertEquals(true, model["containsBlobColumn"])
        assertEquals(true, model["containsBigDecimalColumn"])
        assertEquals(false, model["containsClobColumn"])
        assertEquals(false, model["containsLocalDateColumn"])
        assertEquals(false, model["containsLocalTimeColumn"])
        assertEquals(false, model["containsRefColumn"])
        assertEquals(false, model["containsRowIdColumn"])
        assertEquals(false, model["containsSQLXMLColumn"])
        // group level
        assertEquals(true, model["containsLocalDateTimeColumnInSearchItems"])
        assertEquals(false, model["containsBigDecimalColumnInSearchItems"])
        assertEquals(true, model["containsBigDecimalColumnInListItems"])
        assertEquals(false, model["containsLocalDateTimeColumnInEditItems"])
        assertEquals(true, model["containsBlobColumnInDetailItems"])
        assertEquals(false, model["containsBlobColumnInCacheItems"])
        // id in cache items, case-insensitive
        assertEquals(true, model["containsIdColumnInCacheItems"])
        // serialVersionUID format
        assertTrue(Regex("-?\\d+L").matches(model["serialVersionUID"].toString()))
    }

    @Test
    fun initOtherParametersWithoutIdInCacheItems() {
        val nameCol = col("name", String::class)
        val model = mutableMapOf<String, Any?>(
            "searchItemColumns" to emptyList<Column>(),
            "listItemColumns" to emptyList<Column>(),
            "editItemColumns" to emptyList<Column>(),
            "detailItemColumns" to emptyList<Column>(),
            "cacheItemColumns" to listOf(nameCol),
        )
        creator.initOtherParameters(model, listOf(nameCol))
        assertEquals(false, model["containsIdColumnInCacheItems"])
    }

    //endregion

    //region createEntityRelativeModel / create (H2-backed)

    @Test
    fun createBuildsFullEntityModelFromDatabase() {
        CodegenTestSupport.bindNewH2(
            "tools_tmc",
            """CREATE TABLE IF NOT EXISTS "tmc_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL,
                 "age" INT,
                 "birth_time" TIMESTAMP
               )""",
            """COMMENT ON COLUMN "tmc_demo"."name" IS 'orig name comment'""",
        )
        CodeGeneratorContext.tableName = "tmc_demo"
        CodeGeneratorContext.tableComment = "demo table"
        CodeGeneratorContext.bizModule = ""  // left empty in the wizard -> falls back to the default
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(moduleName = "tmc")
        CodeGeneratorContext.columns = listOf(
            ColumnInfo().apply {
                setName("name")
                setCustomComment("custom name")
                setSearchItem(true)
                setListItem(true)
                setEditItem(true)
                setDetailItem(true)
                setCacheItem(true)
            },
            ColumnInfo().apply {
                setName("age")
                setListItem(true)
            },
        )
        CodeGeneratorContext.templateModelCreator = creator

        val model = creator.create()

        // base part merged in
        assertEquals("tmc", model["module"])
        // entity naming
        assertEquals("TmcDemo", model["entityName"])
        assertEquals("Demo", model["shortEntityName"])
        assertEquals("demo", model["lowerShortEntityName"])
        assertEquals("demo", model["bizModule"], "blank bizModule falls back to the lowercased short entity name")
        // table & columns
        assertNotNull(model["table"])
        @Suppress("UNCHECKED_CAST")
        val funNameMap = model["ktormFunNameMap"] as Map<String, String>
        assertEquals("varchar", funNameMap["name"])
        assertEquals("int", funNameMap["age"])
        assertEquals("datetime", funNameMap["birth_time"])
        assertEquals("id", (model["pkColumn"] as Column).name)
        // grouping by ColumnInfo flags; "age" has no search flag, "birth_time" has no config at all
        @Suppress("UNCHECKED_CAST")
        val searchCols = model["searchItemColumns"] as List<Column>
        assertEquals(listOf("name"), searchCols.map { it.name })
        assertEquals("custom name", searchCols[0].comment, "custom comment must override the metadata one")
        @Suppress("UNCHECKED_CAST")
        val listCols = model["listItemColumns"] as List<Column>
        assertEquals(setOf("name", "age"), listCols.map { it.name }.toSet())
        @Suppress("UNCHECKED_CAST")
        val cacheCols = model["cacheItemColumns"] as List<Column>
        assertEquals(listOf("name"), cacheCols.map { it.name })
        // super classes: string pk without maintenance columns
        assertEquals("StringIdTable", model["daoSuperClass"])
        @Suppress("UNCHECKED_CAST")
        val poColumns = model["columns"] as List<Column>
        assertEquals(setOf("name", "age", "birth_time"), poColumns.map { it.name }.toSet(), "id must be filtered out")
        // other parameters present
        assertEquals(true, model["containsLocalDateTimeColumn"])
        assertEquals(false, model["containsIdColumnInCacheItems"])
        assertNotNull(model["serialVersionUID"])
    }

    //endregion

    private fun col(name: String, type: KClass<*>, pk: Boolean = false): Column = Column().apply {
        this.name = name
        this.kotlinType = type
        this.primaryKey = pk
        this.jdbcTypeName = "TEST"
    }
}
