<#assign formColumns = editItemColumns?filter(c -> c.name?lower_case != pkColumn.name?lower_case)>
package ${packagePrefix}.${module}.common.${bizModule}.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
<#if pkColumn.kotlinTypeName == "String" && (pkColumn.length!0) gt 0>
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
</#if>
<#if pkColumn.kotlinTypeName == "String">
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


<@generateClassComment "Update form request VO of "+(table.comment!table.name)+"."/>
//region your codes 1
data class ${entityName}FormUpdate (
//endregion your codes 1

    /** ${pkColumn.comment!""} */
    <#if pkColumn.kotlinTypeName == "String">
    @get:NotBlank
    </#if>
    <#if pkColumn.kotlinTypeName == "String" && (pkColumn.length!0) gt 0>
    @get:FixedLength(${pkColumn.length})
    </#if>
    override val id: ${pkColumn.kotlinTypeName},

    //region your codes 2

    <#list formColumns as column>
    <@formVoOverrideProperty column/>

    </#list>
    //endregion your codes 2

//region your codes 3
) : IIdEntity<${pkColumn.kotlinTypeName}>, I${entityName}FormBase
//endregion your codes 3
