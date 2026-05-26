package ${packagePrefix}.${module}.api.provider.route

import io.ktor.server.routing.Routing
import io.kudos.ability.web.ktor.core.IKtorRouteRegistrar
import org.springframework.stereotype.Component

<@generateClassComment table.comment+" service route registrar"/>
@Component
//region your codes 1
class ${entityName}ServiceRouteRegistrar : IKtorRouteRegistrar {
//endregion your codes 1
    override fun register(routing: Routing) {
        //region your codes 2

        //endregion your codes 2
    }

}