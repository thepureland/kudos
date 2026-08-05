package io.kudos.ability.data.rdb.ktorm.rowscope

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * Configuration for row-level data-scope filtering.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.data.rdb.row-scope")
open class RowScopeProperties {

    /**
     * Whether row filtering is applied at all. **Off by default** — and this switch is more
     * dangerous than the URL one, because it changes what queries *return* rather than merely
     * refusing a request. Adding the dependency must not change any behaviour.
     */
    var enabled: Boolean = false

    /**
     * Log the predicate that would have been appended, but do not append it.
     *
     * The migration path: switch on, stay in shadow, drive real traffic, read the WARN lines to see
     * which entities are about to be filtered and for whom, then turn shadow off.
     */
    var shadowMode: Boolean = true

    /**
     * Report, at startup, every entity that declares **some** dimensions but not all the ones the
     * deployment has registered. A partial declaration is the failure this catches: the entity looks
     * protected, and silently is not, on the dimension nobody remembered.
     */
    var strictDimensions: Boolean = true
}
