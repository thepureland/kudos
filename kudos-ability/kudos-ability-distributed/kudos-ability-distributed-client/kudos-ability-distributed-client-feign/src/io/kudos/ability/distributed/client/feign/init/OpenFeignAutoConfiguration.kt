package io.kudos.ability.distributed.client.feign.init

import feign.RequestInterceptor
import io.kudos.ability.distributed.client.feign.fallback.GlobalFeignFallBackFactory
import io.kudos.ability.distributed.client.feign.interceptor.GlobalHeaderRequestInterceptor
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * OpenFeign自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class OpenFeignAutoConfiguration: IComponentInitializer {

//    @Autowired
//    lateinit var objectMapper: ObjectMapper

    @Bean("globalHeaderRequestInterceptor")
    open fun feignCacheRequestInterceptor(): RequestInterceptor = GlobalHeaderRequestInterceptor()

    @Bean
    @ConditionalOnMissingBean
    open fun globalFeignFallBackFactory() = GlobalFeignFallBackFactory()

//    @Bean
//    open fun mappingJsonpHttpMessageConverter(): MappingJackson2HttpMessageConverter {
//        val mapper = jackson2ObjectMapperBuilder.build<ObjectMapper?>()
//        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
//        //mapper.setDateFormat(new DateFormattor());
//        val mappingJsonpHttpMessageConverter = MappingJackson2HttpMessageConverter(mapper)
//        return mappingJsonpHttpMessageConverter
//    }

    override fun getComponentName() = "kudos-ability-distributed-client-feign"

}