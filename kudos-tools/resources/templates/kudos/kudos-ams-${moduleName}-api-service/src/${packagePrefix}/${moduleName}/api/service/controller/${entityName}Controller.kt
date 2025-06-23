package ${packagePrefix}.${moduleName}.api.service.controller

import ${packagePrefix}.${moduleName}.common.api.I${entityName}Api
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired


<@generateClassComment table.comment+"服务间控制器"/>
@RestController
//region your codes 1
@RequestMapping("/api-service/${entityName}")
open class ${entityName}Controller {
//endregion your codes 1

    @Autowired
    private lateinit var ${lowerShortEntityName}Api: I${entityName}Api

    //region your codes 2

    //endregion your codes 2

}