package ${packagePrefix}.${module}.consumer.fallback

import ${packagePrefix}.${module}.client.proxy.I${entityName}Proxy
import org.springframework.stereotype.Component


<@generateClassComment table.comment+" fallback handler"/>
@Component
//region your codes 1
interface ${entityName}Fallback : I${entityName}Proxy {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}