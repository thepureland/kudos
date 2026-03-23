package ${packagePrefix}.${module}.provider.service.impl

import ${packagePrefix}.${module}.provider.service.iservice.I${entityName}Service
import ${packagePrefix}.${module}.provider.model.po.${entityName}
import ${packagePrefix}.${module}.provider.dao.${entityName}Dao
<#if table.type.name() == "TABLE">
import io.kudos.base.support.service.BaseCrudService
<#assign superService = "BaseCrudService">
</#if>
<#if table.type.name() == "VIEW">
import io.kudos.base.support.service.BaseReadOnlyService
<#assign superService = "BaseReadOnlyService">
</#if>
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


<@generateClassComment table.comment+"业务"/>
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