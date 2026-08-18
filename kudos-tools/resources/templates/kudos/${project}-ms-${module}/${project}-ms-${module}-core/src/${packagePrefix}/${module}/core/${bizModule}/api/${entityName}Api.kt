package ${packagePrefix}.${module}.core.${bizModule}.api

import ${packagePrefix}.${module}.common.${bizModule}.api.I${entityName}Api
import ${packagePrefix}.${module}.common.${bizModule}.vo.${entityName}CacheEntry
import ${packagePrefix}.${module}.core.${bizModule}.service.iservice.I${entityName}Service
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


<@generateClassComment "Local implementation of the "+(table.comment!table.name)+" API."/>
@Primary
@Component
//region your codes 1
open class ${entityName}Api(
    private val ${entityName?uncap_first}Service: I${entityName}Service,
) : I${entityName}Api {
//endregion your codes 1

    override fun get(id: ${pkColumn.kotlinTypeName}): ${entityName}CacheEntry? =
        ${entityName?uncap_first}Service.get(id, ${entityName}CacheEntry::class)

    //region your codes 2

    //endregion your codes 2

}
