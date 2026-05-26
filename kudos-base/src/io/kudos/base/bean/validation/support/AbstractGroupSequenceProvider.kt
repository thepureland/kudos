package io.kudos.base.bean.validation.support

import io.kudos.base.lang.GenericKit
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider
import kotlin.reflect.KClass

/**
 * Abstract group sequence provider.
 * Frees subclasses from caring about the following:
 * 1. The rule that "the group sequence must include the bean class itself".
 * 2. Null-checking the bean.
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractGroupSequenceProvider<T> : DefaultGroupSequenceProvider<T> {

    override fun getValidationGroups(klass: Class<*>?, bean: T?): List<Class<*>?>? {
        val beanClass = GenericKit.getSuperClassGenricClass(this::class)
        // The bean class itself must be added; otherwise the Default group will not execute and an error will be thrown
        val defaultGroupSequence = mutableListOf<Class<*>>(beanClass.java)
        bean?.let { getGroups(it).mapTo(defaultGroupSequence) { kc -> kc.java } }
        return defaultGroupSequence
    }

    /**
     * Return the groups.
     *
     * @param bean the bean object to validate
     * @return the list of groups
     */
    abstract fun getGroups(bean: T): List<KClass<*>>

}
