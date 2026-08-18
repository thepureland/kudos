package ${packagePrefix}.${module}.common.${bizModule}.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
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


<@generateClassComment "Detail response VO of "+(table.comment!table.name)+"."/>
//region your codes 1
data class ${entityName}Detail (
//endregion your codes 1

    /** ${pkColumn.comment!""} */
    override val id: ${pkColumn.kotlinTypeName} = ${DEFAULT_LITERALS[pkColumn.kotlinTypeName]!"\"\""},

    //region your codes 2

    <#list detailItemColumns as column>
    <#if column.name?lower_case != pkColumn.name?lower_case>
    <@responseVoProperty column/>

    </#if>
    </#list>
    //endregion your codes 2

//region your codes 3
) : IIdEntity<${pkColumn.kotlinTypeName}>
//endregion your codes 3
