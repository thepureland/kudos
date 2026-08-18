<#assign tableType = (table.type.name())!"TABLE">
<#if tableType == "VIEW">
<#assign superService = "BaseReadOnlyService">
<#else>
<#assign superService = "BaseCrudService">
</#if>
package ${packagePrefix}.${module}.core.${bizModule}.service.impl

import io.kudos.base.support.service.impl.${superService}
import ${packagePrefix}.${module}.core.${bizModule}.dao.${entityName}Dao
import ${packagePrefix}.${module}.core.${bizModule}.model.po.${entityName}
import ${packagePrefix}.${module}.core.${bizModule}.service.iservice.I${entityName}Service
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


<@generateClassComment (table.comment!table.name)+" business service."/>
@Service
@Transactional
//region your codes 1
open class ${entityName}Service(
    dao: ${entityName}Dao
) : ${superService}<${pkColumn.kotlinTypeName}, ${entityName}, ${entityName}Dao>(dao), I${entityName}Service {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
