package io.kudos.base.support

import io.kudos.base.logger.LogFactory
import java.io.IOException
import java.util.*


/**
 * Properties文件加载工具类
 * 
 * 用于加载和管理多个properties文件，支持属性值的覆盖和优先级控制。
 * 
 * 加载规则：
 * 1. 多文件加载：可以加载多个properties文件
 * 2. 覆盖机制：后加载的文件会覆盖先加载文件中相同的属性
 * 3. 优先级：System Property > 文件中的属性（最高优先级）
 * 
 * 核心功能：
 * 1. 文件加载：支持加载多个properties文件（Spring Resource格式路径）
 * 2. 类型转换：提供String、Int、Double、Boolean等类型的属性获取方法
 * 3. 默认值：支持为属性提供默认值
 * 4. 优先级：自动优先使用System Property的值
 * 
 * 类型支持：
 * - String：字符串类型，支持默认值
 * - Int：整数类型，支持默认值，转换失败会抛出异常
 * - Double：浮点数类型，支持默认值，转换失败会抛出异常
 * - Boolean：布尔类型，支持默认值，非true/false返回false
 * 
 * 使用场景：
 * - 配置文件加载和管理
 * - 系统配置的读取
 * - 环境变量的覆盖
 * - 多环境配置支持
 * 
 * 注意事项：
 * - 文件路径使用Spring Resource格式（如classpath:config.properties）
 * - System Property优先级最高，适合用于环境变量覆盖
 * - 文件加载失败不会抛出异常，只会记录日志
 * - 类型转换失败会抛出异常，建议使用带默认值的方法
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
     * 取出Property，但以System的Property优先.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    private fun getValue(key: String): String? = System.getProperty(key) ?: properties.getProperty(key)

    /**
     * 取出String类型的Property，但以System的Property优先,如果都為Null则抛出异常.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getProperty(key: String): String? = getValue(key)

    /**
     * 取出String类型的Property，但以System的Property优先.如果都為Null則返回Default值.
     *
     * @param key Key
     * @param defaultValue 默认值
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getProperty(key: String, defaultValue: String): String = getValue(key) ?: defaultValue

    /**
     * 取出Integer类型的Property，但以System的Property优先.如果都為Null或内容错误则抛出异常.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getInt(key: String): Int? = getValue(key)?.toInt()

    /**
     * 取出Integer类型的Property，但以System的Property优先.如果都為Null則返回Default值，如果内容错误则抛出异常
     *
     * @param key Key
     * @param defaultValue 默认值
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getInt(key: String, defaultValue: Int): Int = getValue(key)?.toInt() ?: defaultValue

    /**
     * 取出Double类型的Property，但以System的Property优先.如果都為Null或内容错误则抛出异常.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getDouble(key: String): Double? = getValue(key)?.toDouble()

    /**
     * 取出Double类型的Property，但以System的Property优先.如果都為Null則返回Default值，如果内容错误则抛出异常
     *
     * @param key Key
     * @param defaultValue 默认值
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getDouble(key: String, defaultValue: Double): Double = getValue(key)?.toDouble() ?: defaultValue

    /**
     * 取出Boolean类型的Property，但以System的Property优先.如果都為Null抛出异常,如果内容不是true/false则返回false.
     *
     * @param key Key
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getBoolean(key: String): Boolean? = getValue(key)?.toBoolean()

    /**
     * 取出Boolean类型的Property，但以System的Property优先.如果都為Null則返回Default值,如果内容不为true/false则返回false.
     *
     * @param key Key
     * @param defaultValue 默认值
     * @return Value
     * @author K
     * @since 1.0.0
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = getValue(key)?.toBoolean() ?: defaultValue

    /**
     * 载入多个文件, 文件路径使用Spring Resource格式.
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

    private fun normalizeResourcePath(path: String): String =
        path.removePrefix("classpath:").removePrefix("/")

    private val log = LogFactory.getLog(this)

}