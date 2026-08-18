<#assign queryColumns = searchItemColumns?filter(c -> c.name?lower_case != pkColumn.name?lower_case)>
<#assign likeQueryColumns = queryColumns?filter(c -> c.kotlinTypeName == "String")>
package ${packagePrefix}.${module}.common.${bizModule}.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import ${packagePrefix}.${module}.common.${bizModule}.vo.response.${entityName}Row
<#if (likeQueryColumns?size > 0)>
import io.kudos.base.query.enums.OperatorEnum
import kotlin.reflect.KProperty0
</#if>
<#if containsLocalDateTimeColumnInSearchItems>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumnInSearchItems>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumnInSearchItems>
import java.time.LocalTime
</#if>
<#if containsBlobColumnInSearchItems>
import java.sql.Blob
</#if>
<#if containsClobColumnInSearchItems>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumnInSearchItems>
import java.math.BigDecimal
</#if>
<#if containsRefColumnInSearchItems>
import java.sql.Ref
</#if>
<#if containsRowIdColumnInSearchItems>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumnInSearchItems>
import java.sql.SQLXML
</#if>


<@generateClassComment "List query criteria request VO of "+(table.comment!table.name)+"."/>
<#-- "data" is dropped when no search column is selected: Kotlin forbids a data class with an empty
     primary constructor. The region layout stays identical either way, so re-generating after the
     column selection changes cannot break the merge. -->
//region your codes 1
<#if (queryColumns?size > 0)>data </#if>class ${entityName}Query (
//endregion your codes 1

    //region your codes 2

    <#list queryColumns as column>
    /** ${column.comment!""} */
    val ${column.columnHumpName}: ${column.kotlinTypeName}? = null,

    </#list>
    //endregion your codes 2

//region your codes 3
) : ListSearchPayload() {
//endregion your codes 3

    override fun getReturnEntityClass() = ${entityName}Row::class

    //region your codes 4

    <#if (likeQueryColumns?size > 0)>
    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        <#list likeQueryColumns as column>
        ::${column.columnHumpName} to OperatorEnum.ILIKE,
        </#list>
    )
    </#if>

    //endregion your codes 4

}
