<#assign formColumns = editItemColumns?filter(c -> c.name?lower_case != pkColumn.name?lower_case)>
package ${packagePrefix}.${module}.common.${bizModule}.vo.request

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


<@generateClassComment "Create form request VO of "+(table.comment!table.name)+"."/>
<#-- "data" is dropped when no edit column is selected: Kotlin forbids a data class with an empty
     primary constructor. The region layout stays identical either way. -->
//region your codes 1
<#if (formColumns?size > 0)>data </#if>class ${entityName}FormCreate (
//endregion your codes 1

    //region your codes 2

    <#list formColumns as column>
    <@formVoOverrideProperty column/>

    </#list>
    //endregion your codes 2

//region your codes 3
) : I${entityName}FormBase
//endregion your codes 3
