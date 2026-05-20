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
 * 模板数据模型创建者，用户可继承此类自定义要填充模板的数据
 *
 * @author K
 * @since 1.0.0
 */
open class TemplateModelCreator {

    /**
     * 构造与具体表无关的"基础模型"：包前缀、模块名、作者、版本等模板通用占位符。
     * 这部分内容在批量生成多张表时可以复用。
     *
     * @return 可继续追加 key 的模型 Map
     * @author K
     * @since 1.0.0
     */
    fun createBaseModel(): MutableMap<String, Any?> {
        val templateModel = mutableMapOf<String, Any?>()
        val config = CodeGeneratorContext.config
        templateModel["project"] = config.getTemplateInfo().name
        templateModel[Config.PROP_KEY_PACKAGE_PREFIX] = config.getPackagePrefix()
        templateModel[Config.PROP_KEY_MODULE_NAME] = config.getModuleName()
        templateModel["moduleCapitalize"] = config.getModuleName().capitalizeString()
        templateModel[Config.PROP_KEY_AUTHOR] = config.getAuthor()
        templateModel[Config.PROP_KEY_VERSION] = config.getVersion()
        return templateModel
    }

    /**
     * 模板填充模型的对外入口：基础模型 + 表相关模型合并。
     *
     * @return 渲染模板需要的完整 Map
     * @author K
     * @since 1.0.0
     */
    fun create(): Map<String, Any?> {
        val templateBaseModel = createBaseModel()
        val entityRelativeModel = createEntityRelativeModel()
        return templateBaseModel + entityRelativeModel
    }

    /**
     * 构造与具体表相关的模型：
     * - 实体名（驼峰）、短名（去掉模块前缀）、表结构、所有列
     * - 按 UI 勾选项把列拆分到 search/list/edit/detail/cache 五组
     * - 调 [determinePoDaoSuperClass] 决定 PO/DAO 的父类
     * - 调 [initOtherParameters] 计算各列类型的 import 标志位与 `serialVersionUID`
     *
     * @return 实体相关的模型片段
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
        templateModel["table"] = RdbMetadataKit.getTableByName(tableName)
        val origColumns = RdbMetadataKit.getColumnsByTableName(tableName).values
        templateModel["columns"] = origColumns
        templateModel["ktormFunNameMap"] = origColumns.associate { it.name to KtormSqlType.getFunName(it.kotlinType) }
        templateModel["pkColumn"] = origColumns.first { it.primaryKey }
        val columnConfMap = columns.associateBy { it.getColumn() }

        // 查询项
        val searchItemColumns = mutableListOf<Column>()
        templateModel["searchItemColumns"] = searchItemColumns
        // 列表项
        val listItemColumns = mutableListOf<Column>()
        templateModel["listItemColumns"] = listItemColumns
        // 编辑项
        val editItemColumns = mutableListOf<Column>()
        templateModel["editItemColumns"] = editItemColumns
        // 详情项
        val detailItemColumns = mutableListOf<Column>()
        templateModel["detailItemColumns"] = detailItemColumns
        // 缓存项
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
     * 按主键 Kotlin 类型选择 PO 与 DAO（Ktorm Table）的父类。
     *
     * - 主键 String 且含全部维护字段（createTime/updateTime/active/builtIn 等）→ `IManagedDbEntity` + `ManagedTable`，并把维护字段从模板 columns 中过滤掉（父类已声明）。
     * - 主键 String 不带维护字段 → `StringIdTable`，过滤 id 列。
     * - 主键 Int / Long → 对应的 `IntIdTable` / `LongIdTable`，过滤 id 列。
     * - 其它类型 → 朴素 `Table`，不做列过滤。
     *
     * @param templateModel 上层模板模型；本方法会向其中写 `poSuperClass`/`daoSuperClass`，并可能改写 `columns`
     * @param origColumns 原始数据库列集合（含 id/维护字段）
     * @author K
     * @since 1.0.0
     */
    open fun determinePoDaoSuperClass(templateModel: MutableMap<String, Any?>, origColumns: Collection<Column>) {
        val pkKotlinType = origColumns.first { it.primaryKey }.kotlinType
        var poSuperClass = IDbEntity::class.simpleName
        lateinit var daoSuperClass: String
        when (pkKotlinType) {
            String::class -> {
                val maintainColumns = listOf(
                    ManagedTable<*>::id.name,
                    ManagedTable<*>::createTime.name.humpToUnderscore(false),
                    ManagedTable<*>::createUserId.name,
                    ManagedTable<*>::updateTime.name.humpToUnderscore(false),
                    ManagedTable<*>::updateUserId.name,
                    ManagedTable<*>::active.name,
                    ManagedTable<*>::builtIn.name.humpToUnderscore(false),
                    ManagedTable<*>::remark.name,
                )
                if (origColumns.map { it.name }.containsAll(maintainColumns)) {
                    // 包括所有维护字段，po实现IMaintainableDbEntity，dao实现MaintainableTable
                    poSuperClass = IManagedDbEntity::class.simpleName
                    daoSuperClass = requireNotNull(ManagedTable::class.simpleName) { "MaintainableTable simpleName is null" }
                    // 过滤掉父类中已有的列
                    templateModel["columns"] = origColumns.filter { !maintainColumns.contains(it.name) }
                } else {
                    daoSuperClass = requireNotNull(StringIdTable::class.simpleName) { "StringIdTable simpleName is null" }
                    templateModel["columns"] =
                        origColumns.filter { it.name != "id" } // 过滤掉父类中已有的id列
                }
            }

            Int::class -> {
                daoSuperClass = requireNotNull(IntIdTable::class.simpleName) { "IntIdTable simpleName is null" }
                templateModel["columns"] = origColumns.filter { it.name != "id" } // 过滤掉父类中已有的id列
            }

            Long::class -> {
                daoSuperClass = requireNotNull(LongIdTable::class.simpleName) { "LongIdTable simpleName is null" }
                templateModel["columns"] = origColumns.filter { it.name != "id" } // 过滤掉父类中已有的id列
            }

            else -> daoSuperClass = "Table"
        }
        templateModel["poSuperClass"] = poSuperClass
        templateModel["daoSuperClass"] = daoSuperClass
    }

    /**
     * 计算各组列中是否包含特定 Kotlin 类型，供模板做条件式 import：
     * 例如 `containsLocalDateTimeColumn` / `containsLocalDateTimeColumnInListItems` 等。
     *
     * 模板里据此决定 `import java.time.LocalDateTime` 是否真的需要写出，避免无脑 import 导致 lint 警告。
     * 末尾再生成一个 `serialVersionUID`，让 PO 类拿到稳定可识别的版本号。
     *
     * @param templateModel 上层模板模型，本方法只向其中写 key
     * @param origColumns 原始列集合，用于 PO 自身的 import 判定
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    open fun initOtherParameters(templateModel: MutableMap<String, Any?>, origColumns: Collection<Column>) {
        // 为了模板中，非kotlin类型的import
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

        // po中
        for ((key, value) in kotlinTypeMap)
            templateModel[key] = origColumns.any { it.kotlinType == value }

        // 查询载体类中
        val searchItemColumns = templateModel["searchItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InSearchItems"] = searchItemColumns.any { it.kotlinType == value }

        // 列表记录类中
        val listItemColumns = templateModel["listItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InListItems"] = listItemColumns.any { it.kotlinType == value }

        // 编辑载体类中
        val editItemColumns = templateModel["editItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InEditItems"] = editItemColumns.any { it.kotlinType == value }

        // 详情类中
        val detailItemColumns = templateModel["detailItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InDetailItems"] = detailItemColumns.any { it.kotlinType == value }

        // 缓存项类中
        val cacheItemColumns = templateModel["cacheItemColumns"] as List<Column>
        for ((key, value) in kotlinTypeMap)
            templateModel["${key}InCacheItems"] = cacheItemColumns.any { it.kotlinType == value }

        // 缓存项中是否包含id
        templateModel["containsIdColumnInCacheItems"] = cacheItemColumns.any { it.name.equals("id", true) }

        // serialVersionUID
        templateModel["serialVersionUID"] = RandomStringKit.randomLong() + "L"
    }

}