package io.kudos.ability.web.springmvc.init

import com.fasterxml.jackson.databind.SerializationFeature
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.web.common.consts.WebCommonConst
import org.soul.ability.web.common.init.DictI18nServlet
import org.soul.ability.web.common.session.SessionManager
import org.soul.ability.web.springmvc.CorsHandlerInterceptor
import org.soul.ability.web.springmvc.handler.*
import org.soul.ability.web.springmvc.init.DefaultWebContextInitializer
import org.soul.ability.web.springmvc.init.ServletWebServerFactory
import org.soul.ability.web.springmvc.init.SoulRequestContextListener
import org.soul.ability.web.springmvc.starter.properties.GlobalRequestProperties
import org.soul.ability.web.springmvc.starter.properties.GlobalResponseProperties
import org.soul.context.core.IContextInitializer
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.filter.FormContentFilter
import org.springframework.web.servlet.config.annotation.*
import org.springframework.web.util.pattern.PathPatternParser
import java.nio.charset.StandardCharsets
import java.util.*


/**
 * SpringMvc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableWebMvc
@PropertySource(
    value = ["classpath:kudos-ability-web-springmvc.yml", "classpath:kudos-ability-web-springmvc-global.yml"],
    factory = SoulPropertySourceFactory::class
)
open class SpringMvcAutoConfiguration : IComponentInitializer, WebMvcConfigurer {

    /**
     * Swagger是否屏蔽生产环境
     */
    @Value("\${kudos.ability.web.swagger.production:true}")
    private val swaggerProduction = true


    @Bean
    @ConditionalOnMissingBean
    open fun webServerFactory() = ServletWebServerFactory()

    @Bean
    @ConditionalOnMissingBean
    open fun requestContextListener(): RequestContextListener = SoulRequestContextListener()

    @Bean(IGlobalExceptionProcess.BEAN_NAME)
    @ConditionalOnMissingBean
    open fun globalExceptionProcess(): IGlobalExceptionProcess = DefaultGlobalExceptionProcess()

    @Bean(IGlobalErrorI18nService.BEAN_NAME)
    @ConditionalOnMissingBean
    open fun globalErrorI18nService(): IGlobalErrorI18nService = DefaultGlobalErrorI18nService()

    @Bean(IGlobalResponseBodyProcess.BEAN_NAME)
    @ConditionalOnMissingBean
    open fun globalResponseBodyProcess(): IGlobalResponseBodyProcess = DefaultGlobalResponseBodyProcess()


    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.web.springmvc.global.request")
    open fun globalRequestProperties() = GlobalRequestProperties()

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.web.springmvc.global.response")
    open fun globalResponseProperties() = GlobalResponseProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun servletListenerRegistration(requestContextListener: RequestContextListener): ServletListenerRegistrationBean<EventListener> {
        val registrationBean = ServletListenerRegistrationBean<EventListener>()
        registrationBean.listener = requestContextListener
        registrationBean.order = 1
        return registrationBean
    }

    @Bean
    @ConditionalOnBean
    open fun i18nDictService(): ServletRegistrationBean<DictI18nServlet> {
        val registrationBean = ServletRegistrationBean(
            DictI18nServlet(), WebCommonConst.DICT_URL_GET, WebCommonConst.DICT_URL_ALL
        )
        registrationBean.setLoadOnStartup(1)
        return registrationBean
    }

    @Bean
    @ConditionalOnMissingBean
    open fun webContextInitializer(): IContextInitializer = DefaultWebContextInitializer()

    @Bean
    @ConditionalOnMissingBean
    open fun sessionManager() = SessionManager()

    /**
     * 支持put，delete
     */
    @Bean
    @ConditionalOnMissingBean
    open fun formContentFilter() = FormContentFilter()

    @Bean
    @ConditionalOnMissingBean
    open fun corsHandlerInterceptor() = CorsHandlerInterceptor()

    @Bean
    @ConditionalOnMissingBean
    open fun badRequestExceptionHandler() = BadRequestExceptionHandler()

    @Bean
    @ConditionalOnMissingBean
    open fun globalExceptionHandler() = GlobalExceptionHandler()

    @Bean
    @ConditionalOnMissingBean
    open fun globalResponseBodyHandler() = GlobalResponseBodyHandler()


    override fun addInterceptors(registry: InterceptorRegistry) {
        val corsHandlerInterceptor = corsHandlerInterceptor()
        registry.addInterceptor(corsHandlerInterceptor).addPathPatterns("/**")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowCredentials(true)
            .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")
            .maxAge((3600 * 24).toLong())
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.patternParser = PathPatternParser()
    }

    override fun extendMessageConverters(converters: List<HttpMessageConverter<*>?>) {
        for (converter in converters) {
            if (converter is StringHttpMessageConverter) {
                converter.defaultCharset = StandardCharsets.UTF_8
            }
            if (converter is MappingJackson2HttpMessageConverter) {
                converter.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            }
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        //启用swagger时, 需映射对应的resource
        if (!swaggerProduction) {
            registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/")
            registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/")
        }
    }

    override fun getComponentName() = "kudos-ability-web-springmvc"

}