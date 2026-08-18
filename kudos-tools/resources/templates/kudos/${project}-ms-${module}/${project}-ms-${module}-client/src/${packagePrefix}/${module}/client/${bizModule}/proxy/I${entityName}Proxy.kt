package ${packagePrefix}.${module}.client.${bizModule}.proxy

import ${packagePrefix}.${module}.client.${bizModule}.fallback.${entityName}Fallback
import ${packagePrefix}.${module}.common.${bizModule}.api.I${entityName}Api
import org.springframework.cloud.openfeign.FeignClient


<@generateClassComment (table.comment!table.name)+" client proxy interface."/>
//region your codes 1
@FeignClient(name = "${module}-${lowerShortEntityName}", fallback = ${entityName}Fallback::class)
interface I${entityName}Proxy : I${entityName}Api {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
