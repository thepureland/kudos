package io.kudos.base.i18n

import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.io.scanner.support.Resource
import io.kudos.base.lang.string.right
import io.kudos.base.logger.LogFactory
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * i18n utility.
 *
 * Provides multilingual i18n support, organizing i18n resources by type, module, and language code.
 *
 * File path convention:
 * - Base path: i18n/
 * - Full path: i18n/<type>/<moduleName>_<two-letter lowercase language>_<two-letter uppercase country>.properties
 * - Example: i18n/common/user_zh_CN.properties
 *
 * Data structure:
 * - Four-level nested Map: locale -> type -> module -> key -> value
 * - Supports multiple i18n types (e.g. common, dicts)
 * - Supports multiple modules (e.g. user, order)
 * - Supports multiple languages (e.g. zh_CN, en_US)
 *
 * Core features:
 * 1. Initialization: supports specifying the supported languages list, type list, and default language
 * 2. Resource loading: automatically scans i18n resource files from the classpath
 * 3. Missing fallback: keys missing in other languages automatically fall back to the default language value
 * 4. Dict support: supports i18n for the dict type (DICT_I18N_KEY)
 *
 * Initialization flow:
 * 1. Scan resource files: scan properties files by type and prefix
 * 2. Group by language: group resource files by language code
 * 3. Load default language first: ensure the default language is complete
 * 4. Then load other languages: keys missing in other languages use the default language value
 *
 * Use cases:
 * - i18n display in web applications
 * - i18n of error messages
 * - i18n of dict data
 * - Multilingual system support
 *
 * Notes:
 * - i18n files must be encoded in UTF-8
 * - initI18n must be called before use
 * - Unsupported locales automatically fall back to the default locale
 * - File names must follow the convention, otherwise they cannot be parsed correctly
 *
 * @since 1.0.0
 */
object I18nKit {

    /**
     * Initialize. If not initialized, only zh_CN is supported.
     *
     * @param supportLocales supported Locales
     * @param types i18n information types — user-defined, corresponding to subdirectories of the i18n directory
     * @param defaultLocale default Locale
     * @author K
     * @since 1.0.0
     */
    fun initI18n(supportLocales: Set<String>, types: Set<String>, defaultLocale: String = "zh_CN") {
        this.supportLocales = supportLocales
        this.types = types
        if (defaultLocale !in supportLocales) {
            error("Default Locale [$defaultLocale] is not in the supported list [${supportLocales}]!")
        }
        if (defaultLocale.isNotBlank()) {
            this.defaultLocale = defaultLocale
        }
        initAll(defaultLocale)
    }

    /**
     * Incrementally load i18n resources for a single type.
     * Before calling, [initI18n] must have been called to complete the base initialization
     * (language list, type list, default language).
     *
     * Loading strategy: load the default language first, then load other languages, finally fill in
     * missing keys with the default language.
     *
     * @param args varargs: the 1st is the type (i18n subdirectory name); the 2nd (optional) is the file name prefix
     * @return always returns true on successful call; expresses "executed" rather than a validation result
     * @throws IllegalStateException when [initI18n] has not been called first
     * @author K
     * @since 1.0.0
     */
    fun initI18nByType(vararg args: String): Boolean {
        if (!::supportLocales.isInitialized || !::types.isInitialized) {
            error("Please call initI18n to initialize I18nKit first")
        }
        val type = if (args.isNotEmpty()) args[0] else ""
        val prefix = if (args.size > 1) args[1] else ""
        if (type.isNotBlank()) {
            val otherLocales = ArrayList(supportLocales)
            // remove the default language
            otherLocales.remove(defaultLocale)
            //initI18nByType(type,DEFAULT_LOCALE,Arrays.asList(DEFAULT_LOCALE),prefix);
            initI18nByType(type, defaultLocale, otherLocales, prefix)
        }
        return true
    }

    /**
     * Returns the i18n Map of all types for the given Locale.
     *
     * @param locale two-letter lowercase language code _ two-letter uppercase country code
     * @return MutableMap(type, MutableMap(module, MutableMap(i18n-key, i18n-value)))
     * @author K
     * @since 1.0.0
     */
    fun getI18nMap(locale: String = defaultLocale): Map<String, MutableMap<String, MutableMap<String, String>>> {
        val resolvedLocale = if (!isSupport(locale)) {
            log.warn("Unsupported Locale [${locale}], falling back to default Locale [${defaultLocale}]!")
            defaultLocale
        } else {
            locale
        }
        return i18nMap[resolvedLocale] ?: emptyMap()
    }

    /**
     * Returns the localized string.
     *
     * @param locale  two-letter lowercase language code _ two-letter uppercase country code
     * @param type    i18n information type
     * @param module  module name
     * @param i18nKey i18n key
     * @return the localized string; if not found, returns the value of i18nKey itself
     * @author K
     * @since 1.0.0
     */
    fun getLocalStr(i18nKey: String, module: String, type: String, locale: String = defaultLocale): String {
        val localeStr = if (!isSupport(locale)) {
            log.warn("Unsupported Locale [${locale}], falling back to default Locale [${defaultLocale}]!")
            defaultLocale
        } else locale
        return i18nMap[localeStr]
            ?.get(type)
            ?.get(module)
            ?.get(i18nKey)
            ?: i18nKey
    }

    /**
     * Whether the given Locale is supported.
     *
     * @param locale Locale
     * @return true: supported, false: unsupported
     * @author K
     * @since 1.0.0
     */
    fun isSupport(locale: String): Boolean = supportLocales.contains(locale)


    /** Logger. */
    private val log = LogFactory.getLog(I18nKit::class)
    /** Root directory of i18n resources (under classpath). */
    private const val DEFAULT_BASE_PATH = "i18n/"
    /** Fixed type key for dict i18n (corresponds to the i18n/dicts/ subdirectory). */
    const val DICT_I18N_KEY = "dicts"

    /** Main i18n container: locale -> type -> module -> i18nKey -> i18nValue. */
    private val i18nMap = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, String>>>>()

    /** Dict-specific container: locale -> module -> dictType -> dictKey -> dictValue. */
    private val i18nMapDict =
        mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, String?>>>>()

    /**
     * Default language used for initialization.
     */
    private var defaultLocale: String = "zh_CN"

    /** Set of supported locales, injected by [initI18n]. */
    private lateinit var supportLocales: Set<String>

    /** Set of supported i18n types (subdirectory names under i18n), injected by [initI18n]. */
    private lateinit var types: Set<String>

    /**
     * Entry-style initialization: currently only triggers [initI18n] to perform regular i18n loading.
     * Dict initialization ([initDictByLocale]) is retained as an optional extension point and is not enabled by default.
     *
     * @param defaultLocale default language
     * @author K
     * @since 1.0.0
     */
    private fun initAll(defaultLocale: String) {
        initI18n(defaultLocale)
//        initDictByLocale(defaultLocale)
    }

    /**
     * Iterates over all [types] and loads resources per type; when types is empty, performs a single type-less load.
     *
     * @param defaultLocale default language; other languages use it to fill in missing keys
     * @author K
     * @since 1.0.0
     */
    private fun initI18n(defaultLocale: String) {
        val otherLocales = ArrayList(supportLocales)
        // remove the default language
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
     * Binds the local runtime environment to a resource file.
     *
     * @param file resource file
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

    /**
     * Core implementation for loading all language resources under a single type.
     * First fully establishes a baseline using the default language, then loads other languages and fills in
     * missing keys from the default language.
     *
     * @param type i18n type (i18n subdirectory name)
     * @param defaultLocale default language
     * @param otherLocales list of languages to load other than the default language
     * @param prefix resource file name prefix filter
     * @author K
     * @since 1.0.0
     */
    private fun initI18nByType(type: String, defaultLocale: String, otherLocales: List<String>, prefix: String) {
        val resources = ClassPathScanner.scanForResources(DEFAULT_BASE_PATH + type, prefix, ".properties")
        val resourceGroup = resourceGroup(resources)
        // initialize the default language first
        initOneLocale(defaultLocale, type, resourceGroup[defaultLocale].orEmpty())

        // then initialize the other languages
        for (locale in otherLocales) {
            initOneLocale(locale, type, resourceGroup[locale].orEmpty())
            // fill in missing i18n entries
            compareToSetDefaultLocale(defaultLocale, type, locale)
        }
    }

    /**
     * Initializes by language and type.
     * @param locale
     * @param type
     * @param resourceGroup
     */
    private fun initOneLocale(locale: String, type: String, resourceGroup: List<Resource>) {
        for (resource in resourceGroup) {
            val typeMap = i18nMap.getOrPut(locale) { mutableMapOf() }
            val moduleMap = typeMap.getOrPut(type) { mutableMapOf() }
            val (moduleName, _) = getModuleAndLocale(resource)
            moduleMap.getOrPut(moduleName) { LinkedHashMap() }
            initLocaleByResourceType(moduleMap, resource, type)
        }
    }

    /**
     * Compares against the default language; if a key is missing or its value is blank, falls back to the default language.
     *
     * @param defaultLocale
     * @param type
     * @param locale
     */
    private fun compareToSetDefaultLocale(defaultLocale: String, type: String, locale: String) {
        val moduleMapDef = i18nMap[defaultLocale]?.get(type) ?: return
        val localeTypeMap = i18nMap.getOrPut(locale) { mutableMapOf() }
        val moduleMap = localeTypeMap.getOrPut(type) { LinkedHashMap() }
        for ((module, keyValueMapDef) in moduleMapDef) {
            val keyValueMap = moduleMap[module]
            if (keyValueMap == null) {
                moduleMap[module] = keyValueMapDef //use default locale
                continue
            }
            for ((key, value) in keyValueMapDef) {
                if (!keyValueMap.containsKey(key) || keyValueMap[key].isNullOrBlank()) {
                    keyValueMap[key] = value
                }
            }
        }
    }

    /**
     * Groups resource files by language.
     * @param resources
     * @return
     */
    private fun resourceGroup(resources: Array<Resource>): Map<String, List<Resource>> {
        return supportLocales.associateWith { locale -> resources.filter { it.filename.contains(locale) } }
    }

    /**
     * Processes by type.
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

    /**
     * Parses the module name and locale from a resource file name.
     * File name convention: `<moduleName>_<two-letter lowercase language>_<two-letter uppercase country>.properties`.
     *
     * @param resource resource on the classpath
     * @return Pair(moduleName, locale string)
     * @throws IllegalArgumentException when the file name does not follow the naming convention
     * @author K
     * @since 1.0.0
     */
    private fun getModuleAndLocale(resource: Resource): Pair<String, String> {
        val baseName = resource.filename.substringBefore(".")
        val moduleName = baseName.substring(0, baseName.length - 6)
        val locale = requireNotNull(baseName.right(5)) { "Cannot parse locale from resource name: ${resource.filename}" }
        return Pair(moduleName, locale)
    }

    /**
     * Gets or lazily creates the key-value map for the specified module.
     *
     * @param moduleMap type-level map
     * @param module module name
     * @return the key->value map corresponding to the module
     * @author K
     * @since 1.0.0
     */
    private fun createMapByModule(
        moduleMap: MutableMap<String, MutableMap<String, String>>, module: String
    ): MutableMap<String, String> = moduleMap.getOrPut(module) { mutableMapOf() }

    /**
     * Organizes the dicts inside i18nMap into the dict-specific i18nMapDict.
     * @param locale locale string
     */
    private fun initDictByLocale(locale: String) {
        synchronized(i18nMapDict) {
            val allDicts = getI18nMap(locale)[DICT_I18N_KEY] ?: return
            val localeDictMap = i18nMapDict.getOrPut(locale) { LinkedHashMap() }
            for ((module, moduleDictMap) in allDicts) {
                val moduleMap = localeDictMap.getOrPut(module) { LinkedHashMap() }
                val allDictType: Set<String> = moduleDictMap.keys
                for (oneType in allDictType) {
                    try {
                        val (dictType, realKey) = oneType.split(".", limit = 2)
                        val dictTypeMap = moduleMap.getOrPut(dictType) { LinkedHashMap() }
                        if (moduleDictMap[oneType] != null) {
                            val realValue = moduleDictMap[oneType]
                            dictTypeMap[realKey] = realValue
                        } else {
                            dictTypeMap[dictType] = "${module}_$oneType"
                            log.error("i18n: dict i18n module:{0}, type:{1}, missing Code!", module, oneType)
                        }
                    } catch (_: IndexOutOfBoundsException) {
                        log.error("i18n: dict i18n module:{0}, type:{1}, missing Code!", module, oneType)
                    } catch (_: IllegalArgumentException) {
                        log.error("i18n: dict i18n module:{0}, type:{1}, missing Code!", module, oneType)
                    }
                }
            }
        }
    }

}