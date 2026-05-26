package io.kudos.context.init

import io.kudos.base.logger.LogFactory


/**
 * Component initializer interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IComponentInitializer {

    /**
     * Get the component name.
     *
     * @return component name
     * @author K
     * @since 1.0.0
     */
    fun getComponentName(): String

    /**
     * Called before component initialization starts.
     *
     * @author K
     * @since 1.0.0
     */
    fun beforeInit() {
        LogFactory.getLog(this::class).info(">>>>>>>>>>>>>>>>>>>>  Component [${getComponentName()}] initialization starting...")
    }

    /**
     * Called after component initialization completes.
     *
     * @author K
     * @since 1.0.0
     */
    fun afterInit() {
        LogFactory.getLog(this::class).info("<<<<<<<<<<<<<<<<<<<<  Component [${getComponentName()}] initialization complete.")
    }

}