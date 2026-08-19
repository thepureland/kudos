package ${packagePrefix}.${module}.client.platform.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


<@generateClassComment "Auto-configuration class for the "+module+" atomic service client."/>
// 两个职责：
//  1. 定义 group 名，供各实体的 *ClientConfiguration 引用；
//  2. @ComponentScan 把那些 @Configuration 捞进上下文 —— 调用方只有 @EnableKudos，
//     而 EnableKudos 只导入 IComponentInitializer 实现，扫不到普通 @Configuration。
//
// 调用方必须在部署配置里给这个 group 指定目标，否则 proxy 注册成功、直到首次调用才失败
// （没有编译期或启动期校验）：
//
//   spring:
//     http:
//       serviceclient:
//         ${module}:
//           base-url: lb://${project}-ms-${module}    # 注册中心里的服务 id
//           connect-timeout: 2s
//           read-timeout: 5s
//
// 另外：契约里的 @RequestParam 若不写显式名字，调用方工程必须开启 Kotlin
// javaParameters = true，否则运行期报 "parameter name information not available via reflection"
// —— interface client 只读反射元数据，不像 Feign 自己解析注解。
@Configuration
@ComponentScan(basePackages = ["${packagePrefix}.${module}.client"])
//region your codes 1
open class ${moduleCapitalize}ClientAutoConfiguration : IComponentInitializer {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override fun getComponentName() = "${project}-ms-${module}-client"

    companion object {
        /** HTTP service group 名，须与 `spring.http.serviceclient.<group>` 配置键一致。 */
        const val GROUP: String = "${module}"
    }

}
