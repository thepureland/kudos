package ${packagePrefix}.${module}.common.${bizModule}.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
<#if containsLocalDateTimeColumnInCacheItems>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumnInCacheItems>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumnInCacheItems>
import java.time.LocalTime
</#if>
<#if containsBlobColumnInCacheItems>
import java.sql.Blob
</#if>
<#if containsClobColumnInCacheItems>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumnInCacheItems>
import java.math.BigDecimal
</#if>
<#if containsRefColumnInCacheItems>
import java.sql.Ref
</#if>
<#if containsRowIdColumnInCacheItems>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumnInCacheItems>
import java.sql.SQLXML
</#if>


<@generateClassComment "Cache entry for "+(table.comment!table.name)+"."/>
data class ${entityName}CacheEntry (

    /** ${pkColumn.comment!""} */
    override val id: ${pkColumn.kotlinTypeName},

    //region your codes 1

    <#list cacheItemColumns as column>
    <#if column.name?lower_case != pkColumn.name?lower_case>
    /** ${column.comment!""} */
    val ${column.columnHumpName}: ${column.kotlinTypeName}<#if column.nullable>?</#if>,

    </#if>
    </#list>
    //endregion your codes 1
//region your codes 2
) : IIdEntity<${pkColumn.kotlinTypeName}>, Serializable {
//endregion your codes 2

    //region your codes 3

    //endregion your codes 3

    companion object {
        // Deliberately a fixed 1L rather than the generated ${r"${serialVersionUID}"}: this line sits outside the
        // "your codes" regions, so a random value would change on every re-generation of the same table and
        // break deserialization of every cache entry already in Redis -- the exact opposite of what
        // serialVersionUID is for. Bump it by hand when a change to this class is genuinely incompatible.
        private const val serialVersionUID = 1L
    }

}
