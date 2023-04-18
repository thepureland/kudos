package io.kudos.ability.web.springmvc.init

import com.fasterxml.jackson.databind.json.JsonMapper
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.kit.SpringKit
import org.soul.ability.web.common.consts.WebCommonConst
import org.soul.ability.web.common.init.I18nDictServlet
import org.soul.ability.web.common.session.SessionManager
import org.soul.ability.web.springmvc.CorsHandlerInterceptor
import org.soul.ability.web.springmvc.handler.BadRequestExceptionHandler
import org.soul.ability.web.springmvc.handler.GlobalExceptionHandler
import org.soul.ability.web.springmvc.handler.GlobalResponseBodyHandler
import org.soul.ability.web.springmvc.init.DefaultWebContextInitializer
import org.soul.ability.web.springmvc.init.SoulRequestContextListener
import org.soul.context.core.IContextInitializer
import org.soul.context.core.SoulPropertySourceFactory
import org.soul.context.locale.DateFormattor
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.*
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.filter.FormContentFilter
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.annotation.PostConstruct


/**
 * SpringMvc自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@EnableWebMvc
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(value = ["classpath:kudos-ability-web-springmvc.yml"], factory = SoulPropertySourceFactory::class)
@ComponentScan(
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [TomcatServletWebServerFactory::class]
    )]
)
open class SpringMvcAutoConfiguration : IComponentInitializer, WebMvcConfigurer, BeanFactoryPostProcessor {

    private val log = LoggerFactory.getLogger(this)

    /**
     * Swagger是否屏蔽生产环境
     */
    @Value("\${soul.ability.web.swagger.production:true}")
    private val swaggerProduction = false


//    @Primary
//    @Bean
//    open fun servletWebServerFactory() = ServletWebServerFactory()

//    open fun webServerFactory(): TomcatServletWebServerFactory {
//        val factory = TomcatServletWebServerFactory()
//        factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector: Connector ->
//            // 解决用tomcat时，get请求传入特殊字符报400错误的问题
//            connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}")
//            connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}")
//        })
//        return factory
//    }

//    @Primary
//    @Bean
//    open fun servletWebServerApplicationContext() = ServletWebServerApplicationContext()

    @Bean
    @ConditionalOnMissingBean
    open fun requestContextListener(): RequestContextListener = SoulRequestContextListener()

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
    open fun i18nDictService(): ServletRegistrationBean<I18nDictServlet> {
        val registrationBean = ServletRegistrationBean(
            I18nDictServlet(), WebCommonConst.DICT_URL_GET, WebCommonConst.DICT_URL_ALL
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
        val corsHandlerInterceptor = SpringKit.getBean("corsHandlerInterceptor") as HandlerInterceptor
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
        //允许去掉后缀的匹配
        configurer.isUseSuffixPatternMatch = true
    }

    override fun extendMessageConverters(converters: List<HttpMessageConverter<*>?>) {
        val mapper = JsonMapper()
        mapper.dateFormat = DateFormattor()
        for (converter in converters) {
            if (converter is StringHttpMessageConverter) {
                converter.defaultCharset = StandardCharsets.UTF_8
            } else if (converter is MappingJackson2HttpMessageConverter) {
                converter.objectMapper.dateFormat = DateFormattor()
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

    @PostConstruct
    override fun init() {
        log.info("【kudos-ability-web-springmvc】初始化完成.")
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val beanNames = beanFactory.getBeanNamesForType(ServletWebServerFactory::class.java, true, false)
        for (beanName in beanNames) {
            val beanDefinition = beanFactory.getBeanDefinition(beanName)
            if (beanName == "tomcatServletWebServerFactory") {
                // 将 Bean 设置为非自动装配候选对象，以便在解析依赖关系时不会选择它
                beanDefinition.isAutowireCandidate = false
            }
        }
    }

}