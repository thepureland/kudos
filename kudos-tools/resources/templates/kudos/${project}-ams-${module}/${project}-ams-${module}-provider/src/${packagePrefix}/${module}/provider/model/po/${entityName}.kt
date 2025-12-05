package ${packagePrefix}.${module}.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
<#if poSuperClass == "IDbEntity">
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
<#elseif poSuperClass == "IMaintainableDbEntity">
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity
</#if>
<#if containsLocalDateTimeColumn>
import java.time.LocalDateTime
</#if>
<#if containsLocalDateColumn>
import java.time.LocalDate
</#if>
<#if containsLocalTimeColumn>
import java.time.LocalTime
</#if>
<#if containsBlobColumn>
import java.sql.Blob
</#if>
<#if containsClobColumn>
import java.sql.Clob
</#if>
<#if containsBigDecimalColumn>
import java.math.BigDecimal
</#if>
<#if containsRefColumn>
import java.sql.Ref
</#if>
<#if containsRowIdColumn>
import java.sql.RowId
</#if>
<#if containsSQLXMLColumn>
import java.sql.SQLXML
</#if>

<@generateClassComment table.comment+"数据库实体"/>
//region your codes 1
interface ${entityName} : ${poSuperClass}<${pkColumn.kotlinTypeName}, ${entityName}> {
//endregion your codes 1

    companion object : DbEntityFactory<${entityName}>()

	<#list columns as column>
    /** ${column.comment!""} */
    var ${column.columnHumpName}: ${column.kotlinTypeName}<#if (column.nullable)>?</#if>

	</#list>

    //region your codes 2

    //endregion your codes 2

}