package ${packagePrefix}.${module}.service.biz.impl

import ${packagePrefix}.${module}.service.biz.ibiz.I${entityName}Biz
import ${packagePrefix}.${module}.service.model.po.${entityName}
import ${packagePrefix}.${module}.service.dao.${entityName}Dao
<#if table.type.name() == "TABLE">
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
<#assign superBiz = "BaseCrudBiz">
</#if>
<#if table.type.name() == "VIEW">
import io.kudos.ability.data.rdb.ktorm.biz.BaseReadOnlyBiz
<#assign superBiz = "BaseReadOnlyBiz">
</#if>
import org.springframework.stereotype.Service


<@generateClassComment table.comment+"业务"/>
@Service
//region your codes 1
open class ${entityName}Biz : ${superBiz}<${pkColumn.kotlinTypeName}, ${entityName}, ${entityName}Dao>(), I${entityName}Biz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}