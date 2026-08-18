package ${packagePrefix}.${module}.common.${bizModule}.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
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


<@generateClassComment "Edit response VO of "+(table.comment!table.name)+"."/>
//region your codes 1
data class ${entityName}Edit (
//endregion your codes 1

    /** ${pkColumn.comment!""} */
    override val id: ${pkColumn.kotlinTypeName} = ${DEFAULT_LITERALS[pkColumn.kotlinTypeName]!"\"\""},

    //region your codes 2

    <#list editItemColumns as column>
    <#if column.name?lower_case != pkColumn.name?lower_case>
    <@responseVoProperty column/>

    </#if>
    </#list>
    //endregion your codes 2

//region your codes 3
) : IIdEntity<${pkColumn.kotlinTypeName}>
//endregion your codes 3
