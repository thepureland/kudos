package io.kudos.base.bean.validation.teminal

/**
 * 属性名处理工具
 *
 * @author K
 * @since 1.0.0
 */
object PropertyResolver {
    /**
     * 转成以点分隔的属性名(以单引号括起来)
     *
     * @param property       原属性名
     * @param propertyPrefix 属性名前缀
     * @return 以点分隔的属性名(以单引号括起来)
     * @author K
     * @since 1.0.0
     */
    fun toPotQuote(property: String, propertyPrefix: String): String {
        if (property.isBlank()) {
            return ""
        }
        var prop = property
        // 如果属性已经包含下划线或点，不添加前缀，保持原格式
        val hasSpecialChars = prop.contains("_") || prop.contains(".")
        if (propertyPrefix.isNotBlank() && !prop.startsWith("$")
            && !prop.startsWith("'") && !prop.startsWith(propertyPrefix)
            && !hasSpecialChars
        ) {
            prop = "$propertyPrefix.$prop"
        }
        // 如果属性包含下划线或点，直接添加引号（如果需要），不调用 toPot
        if (hasSpecialChars) {
            if (!prop.startsWith("'") && (prop.contains(".") || prop.contains("_"))) {
                prop = "'$prop'"
            }
            return prop
        }
        return toPotQuote(prop)
    }

    /**
     * 转成以点分隔的属性名(以单引号括起来)
     *
     * @param property 原属性名
     * @return 以点分隔的属性名(以单引号括起来)
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
     * 转成以点分隔的属性名
     *
     * @param property 原属性名
     * @return 以点分隔的属性名
     * @author K
     * @since 1.0.0
     */
    fun toPot(property: String): String {
        var prop = property
        // 如果开头是 $$，移除它并在末尾添加 []
        if (prop.startsWith("$$")) {
            prop = prop.substring(2) + "[]"
        } else if (prop.startsWith("$")) {
            prop = prop.substring(1)
        }
        // 将 $$ 替换为 []（处理中间位置的 $$）
        prop = prop.replace("\\$\\$".toRegex(), "[]") // 数组处理
        prop = prop.replace("_".toRegex(), ".") // 有带"_"的为表单提交时属性名带"."的
        return prop
    }

    /**
     * 转成以下划线分隔的属性名
     *
     * @param property 原属性名
     * @return 以下划线分隔的属性名
     * @author K
     * @since 1.0.0
     */
    fun toUnderline(property: String): String {
        var prop = property
        if (isArrayProperty(prop)) {
            // 如果数组属性后面还有点（如 users[0].name），保持原样
            val bracketIndex = prop.indexOf("]")
            if (bracketIndex < prop.length - 1 && prop[bracketIndex + 1] == '.') {
                return prop
            }
            // 移除数组索引，并在开头添加 $$
            val index = prop.indexOf("[")
            val baseName = prop.substring(0, index)
            val rest = prop.substring(bracketIndex + 1)
            prop = "$$$baseName$rest"
        } else if (prop.contains(".")) {
            prop = prop.replace("\\.".toRegex(), "_")
        } else if (!prop.startsWith("$") && !prop.contains("_")) {
            // 只有当属性名不是以 $ 开头且不包含下划线时，才添加单个 $ 前缀
            prop = "$$prop"
        }
        return prop
    }

    /**
     * 是否返回值为数组的属性
     *
     * @param property 属性名
     * @return true: 是否返回值为数组的属性，反之为false
     * @author K
     * @since 1.0.0
     */
    fun isArrayProperty(property: String): Boolean {
        return property.matches("^.+\\[\\d+].*$".toRegex())
    }
}
