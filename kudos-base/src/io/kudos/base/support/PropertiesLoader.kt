package io.kudos.base.support

import io.kudos.base.logger.LogFactory
import java.io.IOException
import java.util.Properties


/**
 * Properties file loading utility class.
 *
 * Used to load and manage multiple properties files, supporting property value overrides and priority control.
 *
 * Loading rules:
 * 1. Multi-file loading: multiple properties files can be loaded
 * 2. Override mechanism: properties in later-loaded files override those of the same name in earlier-loaded files
 * 3. Priority: System Property > file property (highest priority)
 *
 * Core features:
 * 1. File loading: supports loading multiple properties files (Spring Resource path format)
 * 2. Type conversion: provides methods to retrieve properties as String, Int, Double, Boolean, etc.
 * 3. Default values: supports providing default values for properties
 * 4. Priority: automatically prefers System Property values
 *
 * Type support:
 * - String: string type, supports default values
 * - Int: integer type, supports default values; throws an exception on conversion failure
 * - Double: floating-point type, supports default values; throws an exception on conversion failure
 * - Boolean: boolean type, supports default values; returns false for non-true/false values
 *
 * Use cases:
 * - Configuration file loading and management
 * - Reading system configuration
 * - Environment variable overrides
 * - Multi-environment configuration support
 *
 * Notes:
 * - File paths use the Spring Resource format (e.g., classpath:config.properties)
 * - System Property has the highest priority, suitable for environment variable overrides
 * - File loading failures do not throw exceptions; they are only logged
 * - Type conversion failures throw exceptions; using methods with default values is recommended
 *
 * @since 1.0.0
 */
class PropertiesLoader {

    val properties: Properties

    constructor(properties: Properties) {
        this.properties = properties
    }

    constructor(vararg resourcesPaths: String?) {
        properties = loadProperties(*resourcesPaths.filterNotNull().toTypedArray())
    }

    /**
     * Retrieves a Property, with System Property taking precedence.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    private fun getValue(key: String): String? = System.getProperty(key) ?: properties.getProperty(key)

    /**
     * Retrieves a String Property, with System Property taking precedence. Throws an exception if both are null.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getProperty(key: String): String? = getValue(key)

    /**
     * Retrieves a String Property, with System Property taking precedence. Returns the default value if both are null.
     *
     * @param key Key
     * @param defaultValue Default value
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getProperty(key: String, defaultValue: String): String = getValue(key) ?: defaultValue

    /**
     * Retrieves an Integer Property, with System Property taking precedence. Throws an exception if both are null or the content is invalid.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getInt(key: String): Int? = getValue(key)?.toInt()

    /**
     * Retrieves an Integer Property, with System Property taking precedence. Returns the default value if both are null; throws an exception if the content is invalid.
     *
     * @param key Key
     * @param defaultValue Default value
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getInt(key: String, defaultValue: Int): Int = getValue(key)?.toInt() ?: defaultValue

    /**
     * Retrieves a Double Property, with System Property taking precedence. Throws an exception if both are null or the content is invalid.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getDouble(key: String): Double? = getValue(key)?.toDouble()

    /**
     * Retrieves a Double Property, with System Property taking precedence. Returns the default value if both are null; throws an exception if the content is invalid.
     *
     * @param key Key
     * @param defaultValue Default value
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getDouble(key: String, defaultValue: Double): Double = getValue(key)?.toDouble() ?: defaultValue

    /**
     * Retrieves a Boolean Property, with System Property taking precedence. Throws an exception if both are null; returns false if the content is not true/false.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getBoolean(key: String): Boolean? = getValue(key)?.toBoolean()

    /**
     * Retrieves a Boolean Property, with System Property taking precedence. Returns the default value if both are null; returns false if the content is not true/false.
     *
     * @param key Key
     * @param defaultValue Default value
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = getValue(key)?.toBoolean() ?: defaultValue

    /**
     * Loads multiple files; file paths use the Spring Resource format.
     */
    private fun loadProperties(vararg resourcesPaths: String): Properties {
        val props = Properties()
        for (location in resourcesPaths) {
            log.debug("Loading properties file from:$location")
            try {
                val normalizedPath = normalizeResourcePath(location)
                val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath)
                    ?: javaClass.getResourceAsStream(if (normalizedPath.startsWith("/")) normalizedPath else "/$normalizedPath")
                stream?.use { input ->
                    props.load(input)
                } ?: log.warn("Could not load properties from path:$location (normalized:$normalizedPath), resource not found")
            } catch (ex: IOException) {
                log.warn("Could not load properties from path:$location, ${ex.message}")
            } catch (ex: IllegalArgumentException) {
                log.warn("Could not load properties from path:$location, ${ex.message}")
            }
        }
        return props
    }

    /**
     * Normalizes an externally provided resource path into a format recognized by the classpath resource loader:
     * removes the `classpath:` prefix (Spring style) and the leading `/` (so that ClassLoader.getResource can locate it).
     *
     * @param path The original path
     * @return The normalized path
     * @author K
     * @since 1.0.0
     */
    private fun normalizeResourcePath(path: String): String =
        path.removePrefix("classpath:").removePrefix("/")

    private val log = LogFactory.getLog(this::class)

}