package ${packagePrefix}.${module}.common.vo.${lowerShortEntityName}

import io.kudos.base.support.payload.FormPayload
<#if containsLocalDateTimeColumnInEditItems>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumnInEditItems>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumnInEditItems>
import java.time.LocalTime
</#if>
<#if containsBlobColumnInEditItems>
import java.sql.Blob
</#if>
<#if containsClobColumnInEditItems>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumnInEditItems>
import java.math.BigDecimal
</#if>
<#if containsRefColumnInEditItems>
import java.sql.Ref
</#if>
<#if containsRowIdColumnInEditItems>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumnInEditItems>
import java.sql.SQLXML
</#if>


<@generateClassComment table.comment+"表单载体"/>
data class ${entityName}Payload (

    /** ${pkColumn.comment!""} */
    override var id: ${pkColumn.kotlinTypeName}? = null,

    //region your codes 1

    <#list editItemColumns as column>
    <#if column.name != pkColumn.name>
    /** ${column.comment!""} */
    var ${column.columnHumpName}: ${column.kotlinTypeName}? = null,

    </#if>
    </#list>
    //endregion your codes 1
//region your codes 2
) : FormPayload<${pkColumn.kotlinTypeName}>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}