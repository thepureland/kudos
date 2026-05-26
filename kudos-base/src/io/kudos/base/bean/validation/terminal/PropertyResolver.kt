package io.kudos.base.bean.validation.terminal

/**
 * Property-name handling utilities.
 *
 * @author K
 * @since 1.0.0
 */
object PropertyResolver {
    /**
     * Converts to a dot-delimited property name (wrapped in single quotes).
     *
     * @param property       the original property name
     * @param propertyPrefix the property-name prefix
     * @return the dot-delimited property name (wrapped in single quotes)
     * @author K
     * @since 1.0.0
     */
    fun toPotQuote(property: String, propertyPrefix: String): String {
        if (property.isBlank()) {
            return ""
        }
        var prop = property
        // If the property already contains an underscore or dot, do not add the prefix; preserve the original format.
        val hasSpecialChars = prop.contains("_") || prop.contains(".")
        if (propertyPrefix.isNotBlank() && !prop.startsWith("$")
            && !prop.startsWith("'") && !prop.startsWith(propertyPrefix)
            && !hasSpecialChars
        ) {
            prop = "$propertyPrefix.$prop"
        }
        // If the property contains an underscore or dot, add quotes directly (if needed); do not call toPot.
        if (hasSpecialChars) {
            if (!prop.startsWith("'") && (prop.contains(".") || prop.contains("_"))) {
                prop = "'$prop'"
            }
            return prop
        }
        return toPotQuote(prop)
    }

    /**
     * Converts to a dot-delimited property name (wrapped in single quotes).
     *
     * @param property the original property name
     * @return the dot-delimited property name (wrapped in single quotes)
     * @author K
     * @since 1.0.0
     */
    fun toPotQuote(property: String): String {
        if (property.isBlank()) {
            return ""
        }
        var prop = toPot(property)
        if (!prop.startsWith("'") && (prop.contains(".") || prop.endsWith("[]"))) {
            prop = "'$prop'"
        }
        return prop
    }

    /**
     * Converts to a dot-delimited property name.
     *
     * @param property the original property name
     * @return the dot-delimited property name
     * @author K
     * @since 1.0.0
     */
    fun toPot(property: String): String {
        var prop = property
        // If it starts with $$, strip it and append [] at the end.
        if (prop.startsWith("$$")) {
            prop = prop.substring(2) + "[]"
        } else if (prop.startsWith("$")) {
            prop = prop.substring(1)
        }
        // Replace $$ with [] (handles $$ in the middle of the string).
        prop = prop.replace("\\$\\$".toRegex(), "[]") // array handling
        prop = prop.replace("_".toRegex(), ".") // underscores represent property names with "." submitted from forms
        return prop
    }

    /**
     * Converts to an underscore-delimited property name.
     *
     * @param property the original property name
     * @return the underscore-delimited property name
     * @author K
     * @since 1.0.0
     */
    fun toUnderline(property: String): String {
        var prop = property
        if (isArrayProperty(prop)) {
            // If the array property is followed by a dot (e.g. users[0].name), keep it unchanged.
            val bracketIndex = prop.indexOf("]")
            if (bracketIndex < prop.length - 1 && prop[bracketIndex + 1] == '.') {
                return prop
            }
            // Remove the array index and prepend $$.
            val index = prop.indexOf("[")
            val baseName = prop.substring(0, index)
            val rest = prop.substring(bracketIndex + 1)
            prop = "$$$baseName$rest"
        } else if (prop.contains(".")) {
            prop = prop.replace("\\.".toRegex(), "_")
        } else if (!prop.startsWith("$") && !prop.contains("_")) {
            // Add a single $ prefix only if the property does not start with $ and does not contain an underscore.
            prop = "$$prop"
        }
        return prop
    }

    /**
     * Whether this property's return value is an array.
     *
     * @param property property name
     * @return true if the property's return value is an array; false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isArrayProperty(property: String): Boolean {
        return property.matches("^.+\\[\\d+].*$".toRegex())
    }
}
