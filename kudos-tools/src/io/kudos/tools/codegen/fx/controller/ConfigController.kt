package io.kudos.tools.codegen.fx.controller

import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.ability.data.rdb.jdbc.kit.DataSourceKit
import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.io.FilenameKit
import io.kudos.base.io.PathKit
import io.kudos.base.lang.SystemKit
import io.kudos.base.support.PropertiesLoader
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.tools.codegen.model.vo.Config
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.HashSet
import java.util.Properties
import java.util.ResourceBundle
import javax.sql.DataSource

/**
 * 配置信息界面JavaFx控制器
 *
 * @author K
 * @since 1.0.0
 */
class ConfigController : Initializable {

    /** 数据库 JDBC URL 输入框，与 [Config.dbUrl] 双向绑定 */
    @FXML
    lateinit var urlTextField: TextField

    /** 数据库用户名输入框 */
    @FXML
    lateinit var userTextField: TextField

    /** 数据库密码输入框（掩码显示） */
    @FXML
    lateinit var passwordField: PasswordField

    /** 模板下拉框，展示 templates/ 下的所有模板目录 */
    @FXML
    lateinit var templateChoiceBox: ComboBox<Config.TemplateNameAndRootDir>

    /** 生成代码的包前缀输入框 */
    @FXML
    lateinit var packagePrefixTextField: TextField

    /** 模块名输入框 */
    @FXML
    lateinit var moduleTextField: TextField

    /** 代码输出目录显示框（由目录选择器写入） */
    @FXML
    lateinit var locationTextField: TextField

    /** "选择目录"按钮，触发 [openFileChooser] */
    @FXML
    lateinit var openButton: Button

    /** 作者输入框，对应模板内 `@author` 占位符 */
    @FXML
    lateinit var authorTextField: TextField

    /** 版本输入框，对应模板内 `@since` 占位符 */
    @FXML
    lateinit var versionTextField: TextField

    /** 与 UI 双向绑定的配置 VO */
    val config = Config()
    /** 当前用户的 home 目录，用于定位 properties 文件 */
    private val userHome = System.getProperty("user.home")
    /** 持久化配置文件路径：`~/.kudos/CodeGenerator.properties` */
    private val propertiesFile = File("$userHome/.kudos/CodeGenerator.properties")
    /** 读写 [propertiesFile] 的封装器 */
    private lateinit var propertiesLoader: PropertiesLoader
    /** 模块名候选建议集合，用于将来做自动补全 */
    private var moduleSuggestions: Set<String?>? = null

    /**
     * JavaFX 在加载 FXML 后回调；按"读配置 → 双向绑定 → 初始化下拉框 → 初始化补全"顺序完成界面准备。
     *
     * @param location FXML 文件位置（未使用）
     * @param resources i18n 资源（未使用）
     * @author K
     * @since 1.0.0
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initConfig()
        bindProperties()
        initTempleComboBox()
        initAutoCompletion()
    }

    /**
     * 读取 properties 中的"模块名候选建议"列表，按逗号切分供后续自动补全使用。
     *
     * @author K
     * @since 1.0.0
     */
    private fun initAutoCompletion() {
        val moduleSuggestionsStr = propertiesLoader.getProperty(Config.PROP_KEY_MODULE_SUGGESTIONS, "")
        moduleSuggestions = moduleSuggestionsStr.split(",").toHashSet()
    }

    /**
     * 从 [propertiesFile] 加载所有配置项填入 [config]。
     * 模板信息独立处理：[Config.TemplateNameAndRootDir] 的 name/rootDir 在这里都取自 ROOT_DIR 字段，
     * 因为旧版配置文件没有单独存模板名。
     *
     * @author K
     * @since 1.0.0
     */
    private fun initConfig() {
        val properties = properties
        propertiesLoader = PropertiesLoader(properties)
        with(config) {
            setDbUrl(propertiesLoader.getProperty(Config.PROP_KEY_DB_URL, ""))
            setDbUser(propertiesLoader.getProperty(Config.PROP_KEY_DB_USER, ""))
            setDbPassword(propertiesLoader.getProperty(Config.PROP_KEY_DB_PASSWORD, ""))
            val templateInfo = Config.TemplateNameAndRootDir(
                propertiesLoader.getProperty(Config.PROP_KEY_TEMPLATE_ROOT_DIR, ""),
                FilenameKit.normalize(propertiesLoader.getProperty(Config.PROP_KEY_TEMPLATE_ROOT_DIR, ""), true)
            )
            setTemplateInfo(templateInfo)
            setPackagePrefix(propertiesLoader.getProperty(Config.PROP_KEY_PACKAGE_PREFIX, ""))
            setModuleName(propertiesLoader.getProperty(Config.PROP_KEY_MODULE_NAME, ""))
            setCodeLoaction(propertiesLoader.getProperty(Config.PROP_KEY_CODE_LOACTION, ""))
            setAuthor(propertiesLoader.getProperty(Config.PROP_KEY_AUTHOR, SystemKit.getUser()))
            setVersion(propertiesLoader.getProperty(Config.PROP_KEY_VERSION, ""))
        }
    }

    /**
     * 把每个 FXML 输入控件的 `textProperty` 与 [config] 的对应 JavaFX 属性做双向绑定。
     * 这样 UI 修改即时反映到 [config]，反之亦然，省去手动同步代码。
     *
     * @author K
     * @since 1.0.0
     */
    private fun bindProperties() {
        with(config) {
            urlTextField.textProperty().bindBidirectional(dbUrlProperty())
            userTextField.textProperty().bindBidirectional(dbUserProperty())
            passwordField.textProperty().bindBidirectional(dbPasswordProperty())
            templateChoiceBox.selectionModelProperty().bindBidirectional(templateInfoProperty())
            moduleTextField.textProperty().bindBidirectional(moduleNameProperty())
            authorTextField.textProperty().bindBidirectional(authorProperty())
            versionTextField.textProperty().bindBidirectional(versionProperty())
            packagePrefixTextField.textProperty().bindBidirectional(packagePrefixProperty())
            locationTextField.textProperty().bindBidirectional(codeLoactionProperty())
            authorTextField.textProperty().bindBidirectional(authorProperty())
            versionTextField.textProperty().bindBidirectional(versionProperty())
        }
    }

    /**
     * 扫描 `resources/main/templates/` 下的所有子目录作为模板候选填入下拉框。
     * 默认选中第一项，避免下拉框初始为空导致用户卡在"未选模板"的校验上。
     *
     * @author K
     * @since 1.0.0
     */
    private fun initTempleComboBox() {
        val templatesPath = "${PathKit.getRuntimePath()}/../../../resources/main/templates/"
        val templateNameAndPaths = File(templatesPath).normalize().listFiles().orEmpty().map {
            Config.TemplateNameAndRootDir(it.name, FilenameKit.normalize(it.absolutePath, true))
        }
        templateChoiceBox.items = FXCollections.observableArrayList(templateNameAndPaths)
        templateChoiceBox.selectionModel = object : SingleSelectionModel<Config.TemplateNameAndRootDir>() {
            override fun getItemCount(): Int = templateNameAndPaths.size
            override fun getModelItem(index: Int): Config.TemplateNameAndRootDir = templateNameAndPaths[index]
        }
        templateChoiceBox.selectionModel.select(0)
    }

    /**
     * "下一步"按钮前置校验：连接数据库、跑 Flyway、检查所有必填字段。
     * 任一项失败都抛 [Exception]，由上层 UI 捕获后用 Alert 提示用户。
     *
     * 副作用：成功调用一次会真实建库 / 跑 migration，并把 DataSource 写入 [KudosContextHolder]，
     * 让后续步骤可以直接读元数据。
     *
     * @throws Exception 任一校验项失败时
     * @author K
     * @since 1.0.0
     */
    fun canGoOn() {
        // test connection
        val dataSource = DataSourceKit.createDataSource(
            urlTextField.text.trim(),
            userTextField.text.trim(),
            passwordField.text
        )
        val context = KudosContext()
        context.addOtherInfos(Pair(KudosContext.OTHER_INFO_KEY_DATA_SOURCE, dataSource))
        KudosContextHolder.set(context)

        _testDbConnection()

        migrateDb(dataSource)

        // test template
        if (templateChoiceBox.selectionModel.isEmpty) {
            throw Exception("请选择模板方案！")
        }

        // package prefix
        if (packagePrefixTextField.text.isNullOrBlank()) {
            throw Exception("请填写包名前缀！")
        }

        // test module
        if (moduleTextField.text.isNullOrBlank()) {
            throw Exception("请填写模块名！")
        }

        // test location
        if (locationTextField.text.isNullOrBlank()) {
            throw Exception("代码生成目录不存在！")
        }

        // author location
        if (authorTextField.text.isNullOrBlank()) {
            throw Exception("请填写作者！")
        }

        // version location
        if (versionTextField.text.isNullOrBlank()) {
            throw Exception("请填写版本号！")
        }
    }

    /**
     * 执行 Flyway 脚本升级（codegen 自己的元数据表）。
     *
     * - `isBaselineOnMigrate = true`：新库自动 baseline，避免空库报错
     * - `isValidateOnMigrate = false`：校验阶段宽容，允许历史脚本在本地被改动
     * - `isPlaceholderReplacement = false`：本工具的脚本不含占位符，关闭以避免 `${}` 误解析
     *
     * @param dataSource 上一步建立好的数据源
     * @author K
     * @since 1.0.0
     */
    private fun migrateDb(dataSource: DataSource) {
        val flywayProperties = FlywayProperties().apply {
            isBaselineOnMigrate = true
            baselineVersion = "0"
            encoding = Charsets.UTF_8
            isOutOfOrder = false
            isValidateOnMigrate = false
            isPlaceholderReplacement = false
        }
        FlywayKit.migrate("codegen", dataSource, flywayProperties)
    }

    /**
     * 内部数据库连接测试：成功立即关闭连接；失败弹一次 Alert 并抛 [Exception]。
     * 私有版本不弹"连接成功"，避免在 [canGoOn] 流程里多余的弹窗。
     *
     * @throws Exception 当 JDBC 连接抛错时
     * @author K
     * @since 1.0.0
     */
    private fun _testDbConnection() {
        try {
            RdbKit.newConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword()).use {
                RdbKit.testConnection(it)
            }
        } catch (_: Exception) {
            Alert(Alert.AlertType.ERROR, "连接失败！").show()
            throw Exception("数据库连接不上！")
        }
    }

    /**
     * "测试连接"按钮回调；与 [_testDbConnection] 的区别是连接成功也弹 Alert 反馈用户。
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    private fun testDbConnection() {
        _testDbConnection()
        Alert(Alert.AlertType.INFORMATION, "连接成功！").show()
    }

    /**
     * "选择目录"按钮回调：用 [DirectoryChooser] 选一个目录，写回 [locationTextField]。
     * 若上次保存的目录仍存在，作为对话框的默认起点。
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    private fun openFileChooser() {
        val directoryChooser = DirectoryChooser().apply {
            title = "选择生成目录"
            config.getCodeLoaction().takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.exists() && it.isDirectory }
                ?.let { initialDirectory = it }
        }
        directoryChooser.showDialog(openButton.scene.window)?.let {
            locationTextField.text = it.absolutePath
        }
    }

    /**
     * 把当前 [config] 的内容回写到 [propertiesFile]，供下次启动时恢复。
     * 写文件失败仅打印堆栈，不抛异常——配置写不进去不应阻塞代码生成流程。
     *
     * @author K
     * @since 1.0.0
     */
    fun storeConfig() {
        val properties = propertiesLoader.properties
        with(properties) {
            setProperty(Config.PROP_KEY_DB_URL, config.getDbUrl())
            setProperty(Config.PROP_KEY_DB_USER, config.getDbUser())
            setProperty(Config.PROP_KEY_DB_PASSWORD, config.getDbPassword())
            setProperty(Config.PROP_KEY_TEMPLATE_NAME, config.getTemplateInfo().name)
            setProperty(Config.PROP_KEY_TEMPLATE_ROOT_DIR, config.getTemplateInfo().rootDir)
            setProperty(Config.PROP_KEY_PACKAGE_PREFIX, config.getPackagePrefix())
            setProperty(Config.PROP_KEY_MODULE_NAME, config.getModuleName())
            setProperty(Config.PROP_KEY_CODE_LOACTION, config.getCodeLoaction())
            setProperty(Config.PROP_KEY_MODULE_SUGGESTIONS, moduleSuggestions?.joinToString())
            setProperty(Config.PROP_KEY_AUTHOR, config.getAuthor())
            setProperty(Config.PROP_KEY_VERSION, config.getVersion())
        }
        try {
            FileOutputStream(propertiesFile).use { os -> properties.store(os, null) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 读取或初始化 [propertiesFile]。
     * 文件不存在时创建父目录并塞入一组合理默认值（默认指向本地 H2、当前用户、版本 1.0.0），
     * 让"首次使用"无需先编辑配置文件就能跑通主流程。
     */
    private val properties: Properties
        get() {
            val properties = Properties()
            if (!propertiesFile.exists()) { // 第一次使用，预设组件默认值
                val parentFile = propertiesFile.parentFile
                if (!parentFile.exists()) {
                    if (!parentFile.mkdir()) {
                        throw Exception(parentFile.toString() + "目录创建失败！")
                    }
                }
                with(properties) {
                    setProperty(Config.PROP_KEY_DB_URL, "jdbc:h2:tcp://localhost:9092/./h2;DATABASE_TO_UPPER=false")
                    setProperty(Config.PROP_KEY_DB_USER, "sa")
                    setProperty(Config.PROP_KEY_DB_PASSWORD, "")
                    setProperty(Config.PROP_KEY_PACKAGE_PREFIX, "")
                    setProperty(Config.PROP_KEY_MODULE_NAME, "")
                    setProperty(Config.PROP_KEY_CODE_LOACTION, userHome)
                    setProperty(Config.PROP_KEY_MODULE_SUGGESTIONS, "")
                    setProperty(Config.PROP_KEY_AUTHOR, SystemKit.getUser())
                    setProperty(Config.PROP_KEY_VERSION, "1.0.0")
                }
            } else {
                try {
                    FileInputStream(propertiesFile).use { `is` -> properties.load(`is`) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return properties
        }

}