package ${packagePrefix}.${module}.service.biz.ibiz

import io.kudos.base.support.biz.IBaseCrudBiz
import ${packagePrefix}.${module}.service.model.po.${entityName}


<@generateClassComment table.comment+"业务接口"/>
//region your codes 1
interface I${entityName}Biz : IBaseCrudBiz<${pkColumn.kotlinTypeName}, ${entityName}> {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}