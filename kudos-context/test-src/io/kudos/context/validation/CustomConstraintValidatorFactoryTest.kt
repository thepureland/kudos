package io.kudos.context.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.hibernate.validator.HibernateValidator
import org.springframework.context.support.StaticApplicationContext
import java.time.ZoneId
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for CustomConstraintValidatorFactory
 *
 * Coverage:
 * - getClockProvider returns a system-default-zone clock
 * - setApplicationContext caches the context (and still feeds the parent class)
 * - postProcessConfiguration registers "constraint annotation -> validator" mappings collected from
 *   all IConstraintValidatorProviderBean beans in the context: validated end-to-end by running a real
 *   HibernateValidator validation against a constraint with an empty validatedBy=[] that ONLY works
 *   if the programmatic mapping was applied
 * - The no-provider-bean path (empty bean map, loop body skipped) still yields a working validator
 *
 * @author K
 * @since 1.0.0
 */
internal class CustomConstraintValidatorFactoryTest {

    // ============================================================
    // Fixtures: a custom constraint wired ONLY via the provider bean
    // ============================================================

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    @Constraint(validatedBy = [])
    annotation class NeverValid(
        val message: String = "never valid",
        val groups: Array<KClass<*>> = [],
        val payload: Array<KClass<out Payload>> = []
    )

    class NeverValidValidator : ConstraintValidator<NeverValid, Any?> {
        override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean = false
    }

    class NeverValidProvider : IConstraintValidatorProviderBean {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Annotation, V : ConstraintValidator<T, *>> provide(): Map<KClass<T>, KClass<V>> =
            mapOf(NeverValid::class as KClass<T> to NeverValidValidator::class as KClass<V>)
    }

    class Subject {
        @field:NeverValid
        val name: String = "anything"
    }

    private val openContexts = mutableListOf<StaticApplicationContext>()
    private val openFactories = mutableListOf<CustomConstraintValidatorFactory>()

    @AfterTest
    fun closeResources() {
        openFactories.forEach { runCatching { it.destroy() } }
        openContexts.forEach { runCatching { it.close() } }
    }

    private fun newFactory(vararg providers: IConstraintValidatorProviderBean): CustomConstraintValidatorFactory {
        val ctx = StaticApplicationContext().also { it.refresh(); openContexts += it }
        providers.forEachIndexed { i, p -> ctx.beanFactory.registerSingleton("provider$i", p) }
        return CustomConstraintValidatorFactory().also {
            it.setProviderClass(HibernateValidator::class.java)
            it.setApplicationContext(ctx)
            it.afterPropertiesSet() // triggers postProcessConfiguration
            openFactories += it
        }
    }

    @Test
    fun clockProviderUsesSystemDefaultZone() {
        val clock = CustomConstraintValidatorFactory().clockProvider.clock
        assertEquals(ZoneId.systemDefault(), clock.zone)
    }

    @Test
    fun providerDeclaredValidatorIsRegisteredAndApplied() {
        val factory = newFactory(NeverValidProvider())
        val violations = factory.validator.validate(Subject())
        assertEquals(1, violations.size, "the programmatically mapped validator must reject the value")
        val violation = violations.first()
        assertEquals("never valid", violation.message)
        assertEquals("name", violation.propertyPath.toString())
    }

    @Test
    fun factoryWorksWithoutAnyProviderBeans() {
        val factory = newFactory() // empty provider list -> the mapping loop body is skipped
        // A bean without custom constraints validates cleanly
        val violations = factory.validator.validate(object {
            @Suppress("unused")
            val anything: String = "ok"
        })
        assertTrue(violations.isEmpty())
    }
}
