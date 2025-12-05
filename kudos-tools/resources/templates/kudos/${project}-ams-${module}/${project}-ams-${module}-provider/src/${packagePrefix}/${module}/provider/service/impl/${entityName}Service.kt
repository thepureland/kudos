package ${packagePrefix}.${module}.provider.service.impl

import ${packagePrefix}.${module}.provider.service.iservice.I${entityName}Service
import ${packagePrefix}.${module}.provider.model.po.${entityName}
import ${packagePrefix}.${module}.provider.dao.${entityName}Dao
<#if table.type.name() == "TABLE">
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
<#assign superService = "BaseCrudService">
</#if>
<#if table.type.name() == "VIEW">
import io.kudos.ability.data.rdb.ktorm.service.BaseReadOnlyService
<#assign superService = "BaseReadOnlyService">
</#if>
import org.springframework.stereotype.Service


<@generateClassComment table.comment+"业务"/>
@Service
//region your codes 1
open class ${entityName}Service : ${superService}<${pkColumn.kotlinTypeName}, ${entityName}, ${entityName}Dao>(), I${entityName}Service {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}