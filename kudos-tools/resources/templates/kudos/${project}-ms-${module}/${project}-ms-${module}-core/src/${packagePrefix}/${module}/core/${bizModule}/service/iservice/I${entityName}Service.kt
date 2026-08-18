<#assign tableType = (table.type.name())!"TABLE">
<#if tableType == "VIEW">
<#assign superService = "IBaseReadOnlyService">
<#else>
<#assign superService = "IBaseCrudService">
</#if>
package ${packagePrefix}.${module}.core.${bizModule}.service.iservice

import io.kudos.base.support.service.iservice.${superService}
import ${packagePrefix}.${module}.core.${bizModule}.model.po.${entityName}


<@generateClassComment (table.comment!table.name)+" business interface."/>
//region your codes 1
interface I${entityName}Service : ${superService}<${pkColumn.kotlinTypeName}, ${entityName}> {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
