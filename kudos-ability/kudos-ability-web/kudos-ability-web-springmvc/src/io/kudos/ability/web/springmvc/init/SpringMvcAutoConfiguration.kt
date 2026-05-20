package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.IWebContextInitFilter
import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import io.kudos.ability.web.springmvc.handler.BadRequestExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalResponseBodyHandler
import io.kudos.ability.web.springmvc.handler.MutableListSearchPayloadGuardAdvice
import io.kudos.ability.web.springmvc.interceptor.CorsHandlerInterceptor
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper
import java.util.EventListener


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

    /**
     * 用项目级 Validator（来自 kudos-context 的 [io.kudos.context.init.ValidatorAutoConfiguration]）
     * 替换 Spring MVC 默认 mvcValidator。required = false 是为了万一上层裁掉了 ValidatorAutoConfiguration
     * 时仍能启动，回落到 Spring 默认 validator。
     */
    @Autowired(required = false)
    private var kudosValidator: LocalValidatorFactoryBean? = null

    /**
     * 覆盖 [WebMvcConfigurer.getValidator]，把 MVC 默认 validator 替换为 [kudosValidator]。
     * 返回 null 等价于"沿用 Spring 默认"，因此与 [kudosValidator] 的 `required = false` 配合得当。
     *
     * @return 项目级 validator；未配置时为 null（Spring MVC 自行兜底）
     * @author K
     * @since 1.0.0
     */
    override fun getValidator(): Validator? = kudosValidator

    /** 内嵌容器工厂——按 `kudos.ability.web.springmvc.server` 决定走 Tomcat / Jetty。 */
    @Bean
    @ConditionalOnMissingBean
    open fun webServerFactory(env: Environment): ServletWebServerFactory = SwitchingServletWebServerFactory(env)

    /** 提供 `RequestContextHolder` 在 ServletRequestListener 触发时所需的 listener。 */
    @Bean
    open fun requestContextListener(): RequestContextListener = RequestContextListener()

    /** 把 [RequestContextListener] 显式注册到 servlet 容器（顺序 1，紧跟容器初始化之后）。 */
    @Bean
    open fun servletListenerRegistration(requestContextListener: RequestContextListener): ServletListenerRegistrationBean<EventListener> {
        val registrationBean = ServletListenerRegistrationBean<EventListener>()
        registrationBean.setListener(requestContextListener)
        registrationBean.order = 1
        return registrationBean
    }

    /** kudos 上下文初始化过滤器（业务方可通过自定义 [IWebContextInitFilter] bean 覆盖）。 */
    @Bean
    @ConditionalOnMissingBean
    open fun webContextInitFilter(): IWebContextInitFilter = WebContextInitFilter()

    /**
     * 注册 [IWebContextInitFilter]：order 比 [SessionRepositoryFilter.DEFAULT_ORDER] 大 1，
     * 即 session 已加载后才让 kudos 上下文 filter 抓 sessionAttributes。
     */
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

    /** CORS 头注入拦截器（与下方 [addCorsMappings] 提供的 Spring CORS 支持互补）。 */
    @Bean
    @ConditionalOnMissingBean
    open fun corsHandlerInterceptor(): HandlerInterceptor = CorsHandlerInterceptor()

    /** Jackson 3 默认 [ObjectMapper]，业务可自定义 bean 覆盖以接入自定义 module / 时区策略。 */
    @Bean
    @ConditionalOnMissingBean
    open fun objectMapper(): ObjectMapper = ObjectMapper()

    /** Bad request（参数 / 绑定 / Bean Validation 错误）统一响应处理器。 */
    @Bean
    @ConditionalOnMissingBean
    open fun badRequestExceptionHandler(): BadRequestExceptionHandler = BadRequestExceptionHandler()

    /** 业务异常 / 未捕获异常统一响应处理器（与 [badRequestExceptionHandler] 互补，覆盖余下场景）。 */
    @Bean
    @ConditionalOnMissingBean
    open fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()

    /** 把控制器返回值统一包装为 `ApiResponse` 的 ResponseBodyAdvice。 */
    @Bean
    @ConditionalOnMissingBean
    open fun globalResponseBodyHandler(objectMapper: ObjectMapper): GlobalResponseBodyHandler =
        GlobalResponseBodyHandler(objectMapper)

    /** 拒绝以 [io.kudos.base.model.payload.MutableListSearchPayload] 作为请求体的安全护栏。 */
    @Bean
    @ConditionalOnMissingBean
    open fun mutableListSearchPayloadGuardAdvice(): MutableListSearchPayloadGuardAdvice =
        MutableListSearchPayloadGuardAdvice()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(corsHandlerInterceptor()).addPathPatterns("/**")
    }

    /**
     * 默认开放 CORS：所有 origin pattern + 凭证。
     *
     * **生产部署应通过自定义 [WebMvcConfigurer] 覆盖**——`allowedOriginPatterns("*") +
     * allowCredentials(true)` 在 Spring 中虽合法，但等同于"反射任意来源 + 携带 Cookie"，
     * 实际上是个开放门户。框架默认放宽是为了让开发期不被 CORS 卡住；生产收紧为具体域名。
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowCredentials(true)
            .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")
            .maxAge(3600 * 24)
    }

    override fun getComponentName() = "kudos-ability-web-springmvc"

}
