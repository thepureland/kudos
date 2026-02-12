package ${packagePrefix}.${module}.common.vo.${lowerShortEntityName}

import io.kudos.base.support.result.IdJsonResult
<#if containsLocalDateTimeColumnInDetailItems>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumnInDetailItems>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumnInDetailItems>
import java.time.LocalTime
</#if>
<#if containsBlobColumnInDetailItems>
import java.sql.Blob
</#if>
<#if containsClobColumnInDetailItems>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumnInDetailItems>
import java.math.BigDecimal
</#if>
<#if containsRefColumnInDetailItems>
import java.sql.Ref
</#if>
<#if containsRowIdColumnInDetailItems>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumnInDetailItems>
import java.sql.SQLXML
</#if>


<@generateClassComment table.comment+"查询记录"/>
data class ${entityName}Detail (

    //region your codes 1

    <#if containsIdColumnInCacheItems>
    /** ${pkColumn.comment!""} */
    override var id: ${pkColumn.kotlinTypeName}? = null,
    </#if>

    <#list detailItemColumns as column>
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

    //endregion your codes 3

}