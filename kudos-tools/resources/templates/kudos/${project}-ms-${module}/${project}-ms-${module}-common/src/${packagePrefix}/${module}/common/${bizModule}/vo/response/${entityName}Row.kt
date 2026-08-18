package ${packagePrefix}.${module}.common.${bizModule}.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
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


<@generateClassComment "List query result response VO of "+(table.comment!table.name)+"."/>
//region your codes 1
data class ${entityName}Row (
//endregion your codes 1

    /** ${pkColumn.comment!""} */
    override val id: ${pkColumn.kotlinTypeName} = ${DEFAULT_LITERALS[pkColumn.kotlinTypeName]!"\"\""},

    //region your codes 2

    <#list listItemColumns as column>
    <#if column.name?lower_case != pkColumn.name?lower_case>
    <@responseVoProperty column/>

    </#if>
    </#list>
    //endregion your codes 2

//region your codes 3
) : IIdEntity<${pkColumn.kotlinTypeName}>
//endregion your codes 3
