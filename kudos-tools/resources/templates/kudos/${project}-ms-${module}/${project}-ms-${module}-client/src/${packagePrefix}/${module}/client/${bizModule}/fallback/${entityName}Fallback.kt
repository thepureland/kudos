package ${packagePrefix}.${module}.client.${bizModule}.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import ${packagePrefix}.${module}.client.${bizModule}.proxy.I${entityName}Proxy
import ${packagePrefix}.${module}.common.${bizModule}.vo.${entityName}CacheEntry
import org.springframework.stereotype.Component


<@generateClassComment (table.comment!table.name)+" Feign client fallback implementation."/>
@Component
//region your codes 1
open class ${entityName}Fallback : AbstractFeignFallbackSupport("${entityName}Fallback"), I${entityName}Proxy {
//endregion your codes 1

    override fun get(id: ${pkColumn.kotlinTypeName}): ${entityName}CacheEntry? {
        warnRead("get", id)
        return null
    }

    //region your codes 2

    //endregion your codes 2

}
