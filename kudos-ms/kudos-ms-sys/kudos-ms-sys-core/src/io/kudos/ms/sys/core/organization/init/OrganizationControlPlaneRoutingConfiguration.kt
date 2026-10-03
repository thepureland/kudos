package io.kudos.ms.sys.core.organization.init

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import org.aopalliance.intercept.MethodInterceptor
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.StaticMethodMatcherPointcut
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.lang.reflect.Method

/**
 * In organization mode, identity, directory and authorization metadata must never follow a tenant's
 * data routing: one organization account is shared by all of its tenants, so it lives in one place.
 *
 * Every sys/user/auth core service, DAO and cache call is pinned to the control-plane data source.
 * Joining an already open transaction on a different (tenant) data source is refused rather than
 * silently writing metadata into a tenant database. Off by default; legacy mode routing is untouched.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "kudos.ms.organization", name = ["enabled"], havingValue = "true")
open class OrganizationControlPlaneRoutingConfiguration {

    @Bean
    open fun organizationControlPlaneAdvisor(
        beanFactory: BeanFactory,
        properties: OrganizationModeProperties,
        environment: Environment,
    ): DefaultPointcutAdvisor {
        val pointcut = object : StaticMethodMatcherPointcut() {
            override fun matches(method: Method, targetClass: Class<*>): Boolean {
                val name = targetClass.name
                return PREFIXES.any(name::startsWith) && !name.contains(".init.") &&
                    LAYERS.any(name::contains)
            }
        }
        val routing by lazy { beanFactory.getBean("dataSource") }
        val interceptor = MethodInterceptor { invocation ->
            val dataSource = routing
            if (dataSource !is DynamicRoutingDataSource) return@MethodInterceptor invocation.proceed()
            val key = properties.controlPlaneDatasource
                ?: environment.getProperty("spring.datasource.dynamic.primary", "master")
            require(!key.isNullOrBlank() && !key.startsWith("_context") && dataSource.dataSources.containsKey(key)) {
                "Organization control-plane datasource must name a fixed configured datasource"
            }
            val target = dataSource.getDataSource(key)
            check(!TransactionSynchronizationManager.isActualTransactionActive() || dataSource.determineDataSource() === target) {
                "Organization metadata cannot join an already open tenant-database transaction"
            }
            DynamicDataSourceContextHolder.push(key)
            try {
                invocation.proceed()
            } finally {
                DynamicDataSourceContextHolder.poll()
            }
        }
        // Runs inside generic dynamic routing (-99) and before Spring transactions.
        return DefaultPointcutAdvisor(pointcut, interceptor).apply { order = -98 }
    }

    private companion object {
        val PREFIXES = listOf("io.kudos.ms.sys.core.", "io.kudos.ms.user.core.", "io.kudos.ms.auth.core.")
        val LAYERS = listOf(".service.", ".dao.", ".cache.", ".organization.", ".security.", ".platform.authz.")
    }
}
