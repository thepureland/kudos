package ${packagePrefix}.${module}.api.web.route

import io.ktor.server.routing.Routing
import io.kudos.ability.web.ktor.core.IKtorRouteRegistrar
import org.springframework.stereotype.Component

<@generateClassComment table.comment+"前端路由注册器"/>
@Component
//region your codes 1
class ${entityName}WebRouteRegistrar : IKtorRouteRegistrar {
//endregion your codes 1
    override fun register(routing: Routing) {
        //region your codes 2

        //endregion your codes 2
    }

}