package ${packagePrefix}.${module}.api.internal.controller.${bizModule}

import ${packagePrefix}.${module}.common.${bizModule}.api.I${entityName}Api
import ${packagePrefix}.${module}.common.${bizModule}.vo.${entityName}CacheEntry
import ${packagePrefix}.${module}.core.${bizModule}.api.${entityName}Api
import org.springframework.web.bind.annotation.RestController


<@generateClassComment (table.comment!table.name)+" internal RPC controller. Paths and HTTP methods come from the method-level annotations on I"+entityName+"Api."/>
@RestController
//region your codes 1
class ${entityName}InternalController(
    private val ${entityName?uncap_first}Api: ${entityName}Api,
) : I${entityName}Api {
//endregion your codes 1

    override fun get(id: ${pkColumn.kotlinTypeName}): ${entityName}CacheEntry? = ${entityName?uncap_first}Api.get(id)

    //region your codes 2

    //endregion your codes 2

}
