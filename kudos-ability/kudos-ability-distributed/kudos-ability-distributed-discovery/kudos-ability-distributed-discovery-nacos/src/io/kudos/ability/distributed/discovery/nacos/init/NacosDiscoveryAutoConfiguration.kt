package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration


/**
 * nacos服务发现自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosDiscoveryAutoConfiguration : IComponentInitializer {

//    @Bean
//    open fun feignContextWebFilter(): FilterRegistrationBean<FeignContextWebFilter> {
//        val registration = FilterRegistrationBean<FeignContextWebFilter>().apply {
//            //注入过滤器
//            filter = FeignContextWebFilter()
//            //拦截规则
//            addUrlPatterns("/*")
//            //过滤器名称
//            setName("feignContextWebFilter")
//            //过滤器顺序
//            order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1
//        }
//        return registration
//    }

    override fun getComponentName() = "kudos-ability-distributed-discovery-nacos"

}
