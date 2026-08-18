<#assign tableType = (table.type.name())!"TABLE">
<#if tableType == "VIEW">
<#assign superDao = "BaseReadOnlyDao">
<#else>
<#assign superDao = "BaseCrudDao">
</#if>
package ${packagePrefix}.${module}.core.${bizModule}.dao

import io.kudos.ability.data.rdb.ktorm.support.${superDao}
import ${packagePrefix}.${module}.core.${bizModule}.model.po.${entityName}
import ${packagePrefix}.${module}.core.${bizModule}.model.table.${entityName}s
import org.springframework.stereotype.Repository


<@generateClassComment (table.comment!table.name)+" data access object."/>
@Repository
//region your codes 1
open class ${entityName}Dao : ${superDao}<${pkColumn.kotlinTypeName}, ${entityName}, ${entityName}s>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
