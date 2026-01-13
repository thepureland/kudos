package io.kudos.base.i18n

import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.io.scanner.support.Resource
import io.kudos.base.lang.string.right
import io.kudos.base.logger.LogFactory
import java.io.InputStreamReader
import java.util.*

/**
 * 国际化工具类
 * 
 * 提供多语言国际化支持，支持按类型、模块和语言代码组织国际化资源。
 * 
 * 文件路径规范：
 * - 基础路径：i18n/
 * - 完整路径：i18n/类型/模块名_两位小写语言代码_两位大写国家代码.properties
 * - 示例：i18n/common/user_zh_CN.properties
 * 
 * 数据结构：
 * - 四层嵌套Map结构：locale -> type -> module -> key -> value
 * - 支持多个国际化类型（如common、dicts等）
 * - 支持多个模块（如user、order等）
 * - 支持多个语言（如zh_CN、en_US等）
 * 
 * 核心功能：
 * 1. 初始化：支持指定支持的语言列表、类型列表和默认语言
 * 2. 资源加载：自动扫描类路径下的国际化资源文件
 * 3. 缺失补全：其他语言缺失的key会自动使用默认语言的值
 * 4. 字典支持：支持字典类型的国际化（DICT_I18N_KEY）
 * 
 * 初始化流程：
 * 1. 扫描资源文件：根据类型和前缀扫描properties文件
 * 2. 按语言分组：将资源文件按语言代码分组
 * 3. 先加载默认语言：确保默认语言完整
 * 4. 再加载其他语言：其他语言缺失的key使用默认语言的值
 * 
 * 使用场景：
 * - Web应用的国际化显示
 * - 错误消息的国际化
 * - 字典数据的国际化
 * - 多语言系统支持
 * 
 * 注意事项：
 * - 国际化文件编码必须为UTF-8
 * - 必须调用initI18n初始化后才能使用
 * - 不支持的locale会自动回退到默认locale
 * - 文件命名必须符合规范，否则无法正确解析
 * 
 * @since 1.0.0
 */
object I18nKit {

    /**
     * 初始化，如果不初始化，只支持zh_CN
     *
     * @param supportLocales 支持的Locale
     * @param types 国际化信息类型，自定义，为i18n目录的子目录
     * @param defaultLocale 默认Locale
     * @author K
     * @since 1.0.0
     */
    fun initI18n(supportLocales: Set<String>, types: Set<String>, defaultLocale: String = "zh_CN") {
        this.supportLocales = supportLocales
        this.types = types
        if (defaultLocale !in supportLocales) {
            error("默认Locale【$defaultLocale】不在支持的列表【${supportLocales}】中！")
        }
        if (defaultLocale.isNotBlank()) {
            this.defaultLocale = defaultLocale
        }
        initAll(defaultLocale)
    }

    fun initI18nByType(vararg args: String): Boolean {
        val type = if (args.isNotEmpty()) args[0] else ""
        val prefix = if (args.size > 1) args[1] else ""
        if (type.isNotBlank()) {
            val otherLocales = ArrayList(supportLocales)
            //去除默认语言
            otherLocales.remove(defaultLocale)
            //initI18nByType(type,DEFAULT_LOCALE,Arrays.asList(DEFAULT_LOCALE),prefix);
            initI18nByType(type, defaultLocale, otherLocales, prefix)
        }
        return true
    }

    /**
     * 根据Locale, 获取其所有类别的国际化Map
     *
     * @param locale 两位小写语言代码_两位大写国家代码
     * @return MutableMap(type, MutableMap(module, MutableMap(i18n-key, i18n-value)))
     * @author K
     * @since 1.0.0
     */
    fun getI18nMap(locale: String = defaultLocale): Map<String, MutableMap<String, MutableMap<String, String>>> {
        return if (!isSupport(locale)) {
            log.warn("不支持的Locale【${locale}】，按默认Locale【${defaultLocale}】处理！")
            i18nMap[defaultLocale]!!
        } else i18nMap[locale]!!
    }

    /**
     * 获取国际化后的字符串
     *
     * @param locale  两位小写语言代码_两位大写国家代码
     * @param type    国际化信息类型
     * @param module  模块名
     * @param i18nKey 国际化key
     * @return 国际化后的字符串，如果找不到会直接返回i18nKey的值
     * @author K
     * @since 1.0.0
     */
    fun getLocalStr(i18nKey: String, module: String, type: String, locale: String = defaultLocale): String {
        val localeStr = if (!isSupport(locale)) {
            log.warn("不支持的Locale【${locale}】，按默认Locale【${defaultLocale}】处理！")
            defaultLocale
        } else locale
        return if (i18nMap[localeStr] != null && i18nMap[localeStr]!![type] != null && i18nMap[localeStr]!![type]!![module] != null) {
            i18nMap[localeStr]!![type]!![module]!![i18nKey] ?: i18nKey
        } else i18nKey
    }

    /**
     * 是否支持指定的Locale
     *
     * @param locale Locale
     * @return true: 支持，false: 不支持
     * @author K
     * @since 1.0.0
     */
    fun isSupport(locale: String): Boolean = supportLocales.contains(locale)


    private val log = LogFactory.getLog(I18nKit::class)
    private const val DEFAULT_BASE_PATH = "i18n/"
    const val DICT_I18N_KEY = "dicts"

    //总的国际化容器: MutableMap<locale, MutableMap<type, MutableMap<module, MutableMap<i18n-key, i18n-value>>>>
    private val i18nMap = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, String>>>>()

    //字典国际化容器: MutableMap<locale, MutableMap<type, MutableMap<module, MutableMap<i18n-key, i18n-value>>>>
    private val i18nMapDict =
        mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, String?>>>>()

    /**
     * 初始化的默认语言
     */
    private var defaultLocale: String = "zh_CN"

    private lateinit var supportLocales: Set<String>

    private lateinit var types: Set<String>

    private fun initAll(defaultLocale: String) {
        initI18n(defaultLocale)
//        initDictByLocale(defaultLocale)
    }

    private fun initI18n(defaultLocale: String) {
        val otherLocales = ArrayList(supportLocales)
        //去除默认语言
        otherLocales.remove(defaultLocale)
        if (types.isEmpty()) {
            initI18nByType("", defaultLocale, otherLocales, "")
        } else {
            for (type in types) {
                //initI18nByType(type,defaultLocale,Arrays.asList(defaultLocale),"");
                initI18nByType(type, defaultLocale, otherLocales, "")
            }
        }

    }

    /**
     * 绑定本地运行环境和资源文件
     *
     * @param file 资源文件
     * @author K
     * @since 1.0.0
     */
    @Synchronized
    private fun bundle(file: String): ResourceBundle? {
        I18nKit::class.java.classLoader.getResourceAsStream(file).use { it ->
            if (it != null) {
                InputStreamReader(it, "UTF-8").use {
                    return PropertyResourceBundle(it)
                }
            }
        }
        return null
    }

    private fun initI18nByType(type: String, defaultLocale: String?, otherLocales: List<String?>, prefix: String) {
        val resources = ClassPathScanner.scanForResources(DEFAULT_BASE_PATH + type, prefix, ".properties")
        val resourceGroup = resourceGroup(resources)
        //先初始化:默认语言
        initOneLocale(defaultLocale, type, resourceGroup[defaultLocale]!!)

        //后初始化:其它语言
        for (locale in otherLocales) {
            initOneLocale(locale, type, resourceGroup[locale]!!)
            //补足缺失的国际化
            compareToSetDefaultLocale(defaultLocale, type, locale)
        }
    }

    /**
     * 按语言,类型,进行初始化
     * @param locale
     * @param type
     * @param resourceGroup
     */
    private fun initOneLocale(locale: String?, type: String, resourceGroup: List<Resource>) {
        val moduleSet = HashSet<String>()
        for (resource in resourceGroup) {
            var typeMap = i18nMap[locale]
            if (typeMap == null) {
                typeMap = HashMap()
                typeMap[type] = mutableMapOf()
                i18nMap[locale as String] = typeMap
            }
            val moduleAndLocale = getModuleAndLocale(resource)
            val moduleName = moduleAndLocale.first
            moduleSet.add(moduleName)
            var moduleMap = typeMap[type]
            if (moduleMap == null) {
                moduleMap = HashMap()
                typeMap[type] = moduleMap
            }
            if (!moduleMap.containsKey(moduleName)) {
                moduleMap[moduleName] = LinkedHashMap()
            }
            initLocaleByResourceType(moduleMap, resource, type)
        }
    }

    /**
     * 对比默认语言,不存在,或者值为空,即使用默认语言
     *
     * @param defaultLocale
     * @param type
     * @param locale
     */
    private fun compareToSetDefaultLocale(defaultLocale: String?, type: String, locale: String?) {
        val moduleMapDef = i18nMap[defaultLocale]!![type]
        var moduleMap = i18nMap[locale]!![type]
        if (moduleMap == null) {
            moduleMap = LinkedHashMap()
            i18nMap[locale]!![type] = moduleMap
        }
        if (moduleMapDef == null) {
            return
        }
        for ((module, keyValueMapDef) in moduleMapDef) {
            if (!moduleMap.containsKey(module)) {
                moduleMap[module] = keyValueMapDef //use default locale
                //                LOG.debug("i18n:缺失语言:{0},类型:{1},模块:{2}",locale,type,module);
                continue
            }
            val keyValueMap = moduleMap[module]
            for ((key, value) in keyValueMapDef) {
                if (!keyValueMap!!.containsKey(key) || keyValueMap[key].isNullOrBlank()) {
                    keyValueMap[key] = value
                    //                    LOG.debug("i18n:缺失语言:{0},类型:{1},模块:{2},键:{3}",locale,type,module,key);
                }
            }
        }
    }

    /**
     * 资源文件按语言分组
     * @param resources
     * @return
     */
    private fun resourceGroup(resources: Array<Resource>): Map<String, List<Resource>> {
        return supportLocales.associateWith { locale -> resources.filter { it.filename.contains(locale) } }
    }

    /**
     * 按类型
     * @param moduleMap
     * @param resource
     * @param type
     */
    private fun initLocaleByResourceType(
        moduleMap: MutableMap<String, MutableMap<String, String>>, resource: Resource, type: String
    ) {
        val moduleAndLocale = getModuleAndLocale(resource)
        val moduleName = moduleAndLocale.first
        val bundle = bundle("$DEFAULT_BASE_PATH$type/${resource.filename}")
        val map = createMapByModule(moduleMap, moduleName)
        bundle?.keySet()?.forEach { map[it] = bundle.getString(it) }
    }

    private fun getModuleAndLocale(resource: Resource): Pair<String, String> {
        val baseName = resource.filename.substringBefore(".")
        val moduleName = baseName.substring(0, baseName.length - 5 + -1)
        val locale = baseName.right(5)!!
        return Pair(moduleName, locale)
    }

    private fun createMapByModule(
        moduleMap: MutableMap<String, MutableMap<String, String>>, module: String
    ): MutableMap<String, String> {
        lateinit var map: MutableMap<String, String>
        if (moduleMap.containsKey(module)) {
            map = moduleMap[module]!!
        } else {
            map = HashMap()
            moduleMap[module] = map
        }
        return map
    }

    /**
     * 将i18nMap里的字典,组织成字典专用的i18nMapDict
     * @param locale locale string
     */
    private fun initDictByLocale(locale: String) {
        synchronized(i18nMapDict) {
            val allDicts = getI18nMap(locale)[DICT_I18N_KEY]
            if (!i18nMapDict.containsKey(locale)) {
                i18nMapDict[locale] = LinkedHashMap()
            }
            for (module in allDicts!!.keys) {
                if (!i18nMapDict[locale]!!.containsKey(module)) {
                    i18nMapDict[locale]!![module] = LinkedHashMap()
                }
                val allDictType: Set<String> = allDicts[module]!!.keys
                for (oneType in allDictType) {
                    try {
                        val dictTypeAndKey = oneType.split("\\.").toTypedArray()
                        val dictType = dictTypeAndKey[0]
                        val realKey = dictTypeAndKey[1]
                        if (!i18nMapDict[locale]!![module]!!.containsKey(dictType)) {
                            i18nMapDict[locale]!![module]!![dictType] = LinkedHashMap()
                        }
                        if (allDicts[module] != null) {
                            val realValue = allDicts[module]!![oneType]
                            i18nMapDict[locale]!![module]!![dictType]!![realKey] = realValue
                        } else {
                            i18nMapDict[locale]!![module]!![dictType]!![dictType] = module + "_" + oneType
                            log.error("i18n:字典国际化模块:{0},类型:{1},缺少Code！", module, oneType)
                        }
                    } catch (_: Exception) {
                        log.error("i18n:字典国际化模块:{0},类型:{1},缺少Code！", module, oneType)
                    }
                }
            }
        }
    }

}