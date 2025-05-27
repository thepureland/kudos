package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration


/**
 * nacos服务发现自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class NacosDiscoveryAutoConfiguration: IComponentInitializer {

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

    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-distributed-discovery-nacos】初始化完成.")
    }

}
