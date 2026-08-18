<#assign hasNotBlankEditColumn = false>
<#assign hasMaxLengthEditColumn = false>
<#list editItemColumns as column>
<#if column.name?lower_case != pkColumn.name?lower_case && column.kotlinTypeName == "String">
<#if !column.nullable><#assign hasNotBlankEditColumn = true></#if>
<#if (column.length!0) gt 0><#assign hasMaxLengthEditColumn = true></#if>
</#if>
</#list>
package ${packagePrefix}.${module}.common.${bizModule}.vo.request

<#if hasMaxLengthEditColumn>
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
</#if>
<#if hasNotBlankEditColumn>
import jakarta.validation.constraints.NotBlank
</#if>
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


<@generateClassComment "Base fields shared by the create / update forms of "+(table.comment!table.name)+"."/>
//region your codes 1
interface I${entityName}FormBase {
//endregion your codes 1

    //region your codes 2

    <#list editItemColumns as column>
    <#if column.name?lower_case != pkColumn.name?lower_case>
    <@formVoProperty column/>

    </#if>
    </#list>
    //endregion your codes 2

    //region your codes 3

    //endregion your codes 3

}
