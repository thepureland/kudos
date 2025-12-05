package ${packagePrefix}.${module}.provider.service.iservice

import io.kudos.base.support.service.IBaseCrudService
import ${packagePrefix}.${module}.provider.model.po.${entityName}


<@generateClassComment table.comment+"业务接口"/>
//region your codes 1
interface I${entityName}Service : IBaseCrudService<${pkColumn.kotlinTypeName}, ${entityName}> {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}