package io.kudos.base.bean.validation.support

/**
 * Bean validator interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IBeanValidator<T> {
    /**
     * Execute validation.
     *
     * @param bean the bean to validate
     * @return whether validation passes
     * @author K
     * @since 1.0.0
     */
    fun validate(bean: T): Boolean
}
