<#assign tableType = (table.type.name())!"TABLE">
<#-- A view has no write path: its service only extends IBaseReadOnlyService, which BaseCrudController's
     bound rejects. So a view gets BaseReadOnlyController and none of the edit/form VOs. -->
<#if tableType == "VIEW">
<#assign superController = "BaseReadOnlyController">
<#else>
<#assign superController = "BaseCrudController">
</#if>
package ${packagePrefix}.${module}.api.admin.controller.${bizModule}

import io.kudos.ability.web.springmvc.controller.${superController}
<#if tableType != "VIEW">
import ${packagePrefix}.${module}.common.${bizModule}.vo.request.${entityName}FormCreate
import ${packagePrefix}.${module}.common.${bizModule}.vo.request.${entityName}FormUpdate
</#if>
import ${packagePrefix}.${module}.common.${bizModule}.vo.request.${entityName}Query
import ${packagePrefix}.${module}.common.${bizModule}.vo.response.${entityName}Detail
<#if tableType != "VIEW">
import ${packagePrefix}.${module}.common.${bizModule}.vo.response.${entityName}Edit
</#if>
import ${packagePrefix}.${module}.common.${bizModule}.vo.response.${entityName}Row
import ${packagePrefix}.${module}.core.${bizModule}.service.iservice.I${entityName}Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


<@generateClassComment (table.comment!table.name)+" management controller."/>
@RestController
@RequestMapping("/api/admin/${module}/${shortEntityName?uncap_first}")
//region your codes 1
<#if tableType == "VIEW">
class ${entityName}AdminController :
    BaseReadOnlyController<${pkColumn.kotlinTypeName}, I${entityName}Service, ${entityName}Query, ${entityName}Row, ${entityName}Detail>() {
<#else>
class ${entityName}AdminController :
    BaseCrudController<${pkColumn.kotlinTypeName}, I${entityName}Service, ${entityName}Query, ${entityName}Row, ${entityName}Detail, ${entityName}Edit, ${entityName}FormCreate, ${entityName}FormUpdate>() {
</#if>
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
