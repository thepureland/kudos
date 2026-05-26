package io.kudos.context.config

import org.springframework.core.env.PropertySource

/**
 * Component configuration content finder.
 * Used to fix the problem that specifying configuration files via both configuration files and the configuration
 * center at the same time does not take effect.
 *
 * @author hanson
 * @since 1.0.0
 */
interface IConfigDataFinder {
    /**
     * @param name Configuration file name
     * @return The property source
     */
    fun findConfigData(name: String?): PropertySource<*>?
}
