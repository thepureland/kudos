package io.kudos.tools.codegen.model.vo

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.SingleSelectionModel

/**
 * 配置信息值对象。
 *
 * 代码生成器的全局配置；每个字段都用 JavaFX 的 `SimpleStringProperty`/`SimpleObjectProperty`
 * 持有，对应一组标准三件套：`getX()` / `xProperty()` / `setX()` —— 前者拿值、中间用于 UI 双向绑定、
 * 后者写值。该约定来自 JavaBeans + JavaFX，IDE/FXML 工具默认按这三个方法名探测属性。
 *
 * @author K
 * @since 1.0.0
 */
class Config {

    /** 数据库 JDBC URL（用于反向读取表/列元数据） */
    private val dbUrl = SimpleStringProperty()
    /** 数据库连接用户名 */
    private val dbUser = SimpleStringProperty()
    /** 数据库连接密码 */
    private val dbPassword = SimpleStringProperty()
    /** 当前选中的模板（名 + 根目录），绑定到下拉框的 [SingleSelectionModel] */
    private val templateInfo = SimpleObjectProperty<SingleSelectionModel<TemplateNameAndRootDir>>()
    /** 生成代码的包前缀，例如 `io.kudos.ms.user` */
    private val packagePrefix = SimpleStringProperty()
    /** 生成代码归属的模块名 */
    private val moduleName = SimpleStringProperty()
    /** 生成的代码输出根目录（注意：历史拼写沿用 `codeLoaction`，未修正以保持配置兼容） */
    private val codeLoaction = SimpleStringProperty()
    /** 模板内 `@author` 占位符的填充值 */
    private val author = SimpleStringProperty()
    /** 模板内 `@since` 占位符的填充值 */
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
        /** 持久化到 properties 文件时使用的 key 集合；命名与字段名保持一致，便于配置文件可读 */
        const val PROP_KEY_DB_URL = "dbUrl"
        /** properties key：数据库用户名 */
        const val PROP_KEY_DB_USER = "dbUser"
        /** properties key：数据库密码（明文落盘，仅供开发工具使用，勿写入生产配置） */
        const val PROP_KEY_DB_PASSWORD = "dbPassword"
        /** properties key：模板名 */
        const val PROP_KEY_TEMPLATE_NAME = "templateName"
        /** properties key：模板根目录 */
        const val PROP_KEY_TEMPLATE_ROOT_DIR = "templateRootDir"
        /** properties key：代码包前缀 */
        const val PROP_KEY_PACKAGE_PREFIX = "packagePrefix"
        /** properties key：模块名 */
        const val PROP_KEY_MODULE_NAME = "module"
        /** properties key：模块名候选建议（多个值时按分隔符切分） */
        const val PROP_KEY_MODULE_SUGGESTIONS = "moduleNameSuggestions"
        /** properties key：代码输出根目录（保留历史拼写） */
        const val PROP_KEY_CODE_LOACTION = "codeLoaction"
        /** properties key：作者 */
        const val PROP_KEY_AUTHOR = "author"
        /** properties key：版本 */
        const val PROP_KEY_VERSION = "version"
    }

    /**
     * 模板名 + 模板根目录的二元组。
     * 重写 [toString] 仅返回 name，让 [SingleSelectionModel] 在下拉框中默认显示模板名。
     *
     * @property name 模板名（人类可读）
     * @property rootDir 模板根目录（文件系统路径）
     * @author K
     * @since 1.0.0
     */
    data class TemplateNameAndRootDir(val name: String, val rootDir: String) {

        override fun toString(): String = name

    }

}