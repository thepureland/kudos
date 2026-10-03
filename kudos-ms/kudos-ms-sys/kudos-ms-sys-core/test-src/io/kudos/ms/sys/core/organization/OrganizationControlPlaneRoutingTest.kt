package io.kudos.ms.sys.core.organization.init

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.mockito.Mockito.mock
import org.springframework.beans.factory.support.DefaultListableBeanFactory

import org.springframework.mock.env.MockEnvironment
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource
import kotlin.test.*

internal class OrganizationControlPlaneRoutingTest {
    private val controlPlane = mock(DataSource::class.java)
    private val tenant = mock(DataSource::class.java)
    private val routing = DynamicRoutingDataSource(emptyList()).apply {
        setPrimary("control-plane")
        addDataSource("control-plane", controlPlane)
        addDataSource("tenant-a", tenant)
    }
    private val environment = MockEnvironment().withProperty("spring.datasource.dynamic.primary", "control-plane")
    private val properties = OrganizationModeProperties().apply { enabled = true }
    private val factory = DefaultListableBeanFactory().apply { registerSingleton("dataSource", routing) }
    private val interceptor = OrganizationControlPlaneRoutingConfiguration().organizationControlPlaneAdvisor(factory, properties, environment).advice as MethodInterceptor

    @AfterTest
    fun clear() {
        DynamicDataSourceContextHolder.clear()
        TransactionSynchronizationManager.clear()
    }

    @Test
    fun tenantRoutingCannotRedirectIdentityAndOriginalRouteIsRestored() {
        DynamicDataSourceContextHolder.push("tenant-a")
        assertSame(controlPlane, invoke { routing.determineDataSource() })
        assertSame(tenant, routing.determineDataSource())
    }

    @Test
    fun existingTenantTransactionIsRefusedInsteadOfWritingMetadataToItsConnection() {
        DynamicDataSourceContextHolder.push("tenant-a")
        TransactionSynchronizationManager.setActualTransactionActive(true)
        assertFailsWith<IllegalStateException> { invoke { fail("must not join tenant transaction") } }
        assertSame(tenant, routing.determineDataSource())
    }

    @Test
    fun missingFixedDatasourceFailsClosedAndDoesNotFallbackToTenant() {
        properties.controlPlaneDatasource = "missing"
        DynamicDataSourceContextHolder.push("tenant-a")
        assertFailsWith<IllegalArgumentException> { invoke { fail("must not use fallback") } }
    }

    private fun invoke(body: () -> Any?): Any? = interceptor.invoke(object : MethodInvocation {
        override fun getMethod() = Any::class.java.getMethod("toString")
        override fun getArguments(): Array<Any> = emptyArray()
        override fun proceed(): Any? = body()
        override fun getThis(): Any = this@OrganizationControlPlaneRoutingTest
        override fun getStaticPart() = method
    })
}
