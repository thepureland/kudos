package ${packagePrefix}.${module}.common.vo.${lowerShortEntityName}

import io.kudos.base.support.result.IdJsonResult
<#if containsLocalDateTimeColumnInListItems>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumnInListItems>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumnInListItems>
import java.time.LocalTime
</#if>
<#if containsBlobColumnInListItems>
import java.sql.Blob
</#if>
<#if containsClobColumnInListItems>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumnInListItems>
import java.math.BigDecimal
</#if>
<#if containsRefColumnInListItems>
import java.sql.Ref
</#if>
<#if containsRowIdColumnInListItems>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumnInListItems>
import java.sql.SQLXML
</#if>


<@generateClassComment table.comment+"查询记录"/>
data class ${entityName}Record (

    //region your codes 1

    <#if containsIdColumnInCacheItems>
    /** ${pkColumn.comment!""} */
    override var id: ${pkColumn.kotlinTypeName}? = null,
    </#if>

    <#list listItemColumns as column>
    <#if column.name?lower_case != "id">
    /** ${column.comment!""} */
    var ${column.columnHumpName}: ${column.kotlinTypeName}? = null,

    </#if>
    </#list>
    //endregion your codes 1
//region your codes 2
) : IdJsonResult<${pkColumn.kotlinTypeName}>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}