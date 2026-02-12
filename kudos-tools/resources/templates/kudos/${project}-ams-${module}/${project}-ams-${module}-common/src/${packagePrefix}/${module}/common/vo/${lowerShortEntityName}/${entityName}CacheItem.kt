package ${packagePrefix}.${module}.common.vo.${lowerShortEntityName}

import java.io.Serializable
<#if containsIdColumnInCacheItems>
import io.kudos.base.support.IIdEntity
</#if>
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


<@generateClassComment table.comment+"缓存项"/>
data class ${entityName}CacheItem (

    <#if containsIdColumnInCacheItems>
    /** ${pkColumn.comment!""} */
    override var id: ${pkColumn.kotlinTypeName}? = null,
    </#if>

    //region your codes 1

    <#list cacheItemColumns as column>
    <#if column.name?lower_case != "id">
    /** ${column.comment!""} */
    var ${column.columnHumpName}: ${column.kotlinTypeName}? = null,

    </#if>
    </#list>
    //endregion your codes 1
//region your codes 2
<#if containsIdColumnInCacheItems>
) : IIdEntity<${pkColumn.kotlinTypeName}>, Serializable {
</#if>
<#if containsIdColumnInCacheItems == false>
) : Serializable {
</#if>
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = ${serialVersionUID}
    }

}