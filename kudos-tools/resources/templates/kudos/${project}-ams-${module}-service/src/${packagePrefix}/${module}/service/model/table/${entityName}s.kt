package ${packagePrefix}.${module}.service.model.table

import ${packagePrefix}.${module}.service.model.po.${entityName}
import org.ktorm.schema.*
<#if daoSuperClass == "MaintainableTable">
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable
<#elseif daoSuperClass == "StringIdTable">
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
<#elseif daoSuperClass == "IntIdTable">
import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
<#elseif daoSuperClass == "LongIdTable">
import io.kudos.ability.data.rdb.ktorm.support.LongIdTable
</#if>


<@generateClassComment table.comment+"数据库表-实体关联对象"/>
//region your codes 1
object ${entityName}s : ${daoSuperClass}<${entityName}>("${table.name}") {
//endregion your codes 1

	<#list columns as column>
    /** ${column.comment!""} */
    var ${column.columnHumpName} = ${ktormFunNameMap[column.name]}("${column.name}").bindTo { it.${column.columnHumpName} }

	</#list>

    //region your codes 2

    //endregion your codes 2

}