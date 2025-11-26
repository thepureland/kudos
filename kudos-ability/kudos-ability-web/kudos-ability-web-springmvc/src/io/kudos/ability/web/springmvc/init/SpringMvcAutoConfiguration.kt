package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.IWebContextInitFilter
import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import io.kudos.ability.web.springmvc.interceptor.CorsHandlerInterceptor
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import jakarta.servlet.Filter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*


/**
 * SpringMvc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-web-springmvc.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableWebMvc
open class SpringMvcAutoConfiguration : WebMvcConfigurer, IComponentInitializer {

//    @Bean
//    open fun mutipartResolvet(): CommonsMultipartResolver {
//        return CommonsMultipartResolver()
//    }

    @Bean
    @ConditionalOnMissingBean
    open fun webServerFactory(env: Environment): ServletWebServerFactory = SwitchingServletWebServerFactory(env)

    @Bean
    open fun requestContextListener(): RequestContextListener = RequestContextListener()

    @Bean
    open fun servletListenerRegistration(requestContextListener: RequestContextListener): ServletListenerRegistrationBean<EventListener> {
        val registrationBean = ServletListenerRegistrationBean<EventListener>()
        registrationBean.setListener(requestContextListener())
        registrationBean.order = 1
        return registrationBean
    }

    @Bean
    @ConditionalOnMissingBean
    open fun webContextInitFilter(): IWebContextInitFilter = WebContextInitFilter()

    @Bean
    @ConditionalOnMissingBean
    open fun registerAuthFilter(contextInitFilter: IWebContextInitFilter): FilterRegistrationBean<*> {
        val registration = FilterRegistrationBean<Filter>()
        registration.setFilter(contextInitFilter)
        registration.addUrlPatterns("/*")
        registration.setName("contextInitFilter")
        registration.order = SessionRepositoryFilter.DEFAULT_ORDER + 1
        return registration
    }

    @Bean
    @ConditionalOnMissingBean
    open fun corsHandlerInterceptor(): HandlerInterceptor = CorsHandlerInterceptor()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(corsHandlerInterceptor()).addPathPatterns("/**");
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowCredentials(true)
            .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")
            .maxAge(3600 * 24)
    }

    override fun getComponentName() = "kudos-ability-web-springmvc"

}