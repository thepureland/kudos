package io.kudos.tools.codegen.model.vo

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.SingleSelectionModel

/**
 * Configuration value object.
 *
 * Global configuration for the code generator. Each field is held by JavaFX's `SimpleStringProperty`/
 * `SimpleObjectProperty`, exposing the standard triad: `getX()` / `xProperty()` / `setX()` — the first
 * reads the value, the middle is used for UI two-way binding, and the last writes the value. This
 * convention comes from JavaBeans + JavaFX, and IDE/FXML tools detect properties via these three method
 * names by default.
 *
 * @author K
 * @since 1.0.0
 */
class Config {

    /** Database JDBC URL (used to read table/column metadata via reverse engineering) */
    private val dbUrl = SimpleStringProperty()
    /** Database connection username */
    private val dbUser = SimpleStringProperty()
    /** Database connection password */
    private val dbPassword = SimpleStringProperty()
    /** Currently selected template (name + root directory), bound to the dropdown's [SingleSelectionModel] */
    private val templateInfo = SimpleObjectProperty<SingleSelectionModel<TemplateNameAndRootDir>>()
    /** Package prefix for generated code, e.g. `io.kudos.ms.user` */
    private val packagePrefix = SimpleStringProperty()
    /** Module name to which the generated code belongs */
    private val moduleName = SimpleStringProperty()
    /** Root output directory for generated code (note: historical spelling `codeLoaction` is kept for config compatibility) */
    private val codeLoaction = SimpleStringProperty()
    /** Fill value for the `@author` placeholder in templates */
    private val author = SimpleStringProperty()
    /** Fill value for the `@since` placeholder in templates */
    private val version = SimpleStringProperty()

    fun getDbUrl(): String = dbUrl.get()

    fun dbUrlProperty(): StringProperty = dbUrl

    fun setDbUrl(dbUrl: String) = this.dbUrl.set(dbUrl)

    fun getDbUser(): String = dbUser.get()

    fun dbUserProperty(): StringProperty = dbUser

    fun setDbUser(dbUser: String) = this.dbUser.set(dbUser)

    fun getDbPassword(): String = dbPassword.get()

    fun dbPasswordProperty(): StringProperty = dbPassword

    fun setDbPassword(dbPassword: String) = this.dbPassword.set(dbPassword)

    fun getTemplateInfo(): TemplateNameAndRootDir = templateInfo.get().selectedItem

    fun templateInfoProperty(): SimpleObjectProperty<SingleSelectionModel<TemplateNameAndRootDir>> = templateInfo

    fun setTemplateInfo(templateInfo: TemplateNameAndRootDir?) = this.templateInfo.get()?.select(templateInfo)

    fun getPackagePrefix(): String = packagePrefix.get()

    fun packagePrefixProperty(): StringProperty = packagePrefix

    fun setPackagePrefix(packagePrefix: String) = this.packagePrefix.set(packagePrefix)

    fun getModuleName(): String = moduleName.get()

    fun moduleNameProperty(): StringProperty = moduleName

    fun setModuleName(moduleName: String) = this.moduleName.set(moduleName)

    fun getCodeLoaction(): String = codeLoaction.get()

    fun codeLoactionProperty(): StringProperty = codeLoaction

    fun setCodeLoaction(codeLoaction: String) = this.codeLoaction.set(codeLoaction)

    fun getAuthor(): String = author.get()

    fun authorProperty(): StringProperty = author

    fun setAuthor(author: String) = this.author.set(author)

    fun getVersion(): String = version.get()

    fun versionProperty(): StringProperty = version

    fun setVersion(version: String) = this.version.set(version)

    companion object {
        /** Keys used when persisting to the properties file; names match the field names for readability */
        const val PROP_KEY_DB_URL = "dbUrl"
        /** properties key: database username */
        const val PROP_KEY_DB_USER = "dbUser"
        /** properties key: database password (stored in plaintext, for dev tooling only; do not use in production config) */
        const val PROP_KEY_DB_PASSWORD = "dbPassword"
        /** properties key: template name */
        const val PROP_KEY_TEMPLATE_NAME = "templateName"
        /** properties key: template root directory */
        const val PROP_KEY_TEMPLATE_ROOT_DIR = "templateRootDir"
        /** properties key: code package prefix */
        const val PROP_KEY_PACKAGE_PREFIX = "packagePrefix"
        /** properties key: module name */
        const val PROP_KEY_MODULE_NAME = "module"
        /** properties key: module name suggestions (split by separator when multiple) */
        const val PROP_KEY_MODULE_SUGGESTIONS = "moduleNameSuggestions"
        /** properties key: code output root directory (historical spelling preserved) */
        const val PROP_KEY_CODE_LOACTION = "codeLoaction"
        /** properties key: author */
        const val PROP_KEY_AUTHOR = "author"
        /** properties key: version */
        const val PROP_KEY_VERSION = "version"
    }

    /**
     * Pair of template name and template root directory.
     * Overrides [toString] to return only the name so [SingleSelectionModel] displays the template name
     * in the dropdown by default.
     *
     * @property name template name (human-readable)
     * @property rootDir template root directory (filesystem path)
     * @author K
     * @since 1.0.0
     */
    data class TemplateNameAndRootDir(val name: String, val rootDir: String) {

        override fun toString(): String = name

    }

}