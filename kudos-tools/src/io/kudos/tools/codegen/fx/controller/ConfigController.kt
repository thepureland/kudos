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
 * JavaFX controller for the configuration screen.
 *
 * @author K
 * @since 1.0.0
 */
class ConfigController : Initializable {

    /** Database JDBC URL text field, bidirectionally bound to [Config.dbUrl] */
    @FXML
    lateinit var urlTextField: TextField

    /** Database username text field */
    @FXML
    lateinit var userTextField: TextField

    /** Database password text field (masked) */
    @FXML
    lateinit var passwordField: PasswordField

    /** Template dropdown that lists all template directories under templates/ */
    @FXML
    lateinit var templateChoiceBox: ComboBox<Config.TemplateNameAndRootDir>

    /** Package prefix text field for generated code */
    @FXML
    lateinit var packagePrefixTextField: TextField

    /** Module name text field */
    @FXML
    lateinit var moduleTextField: TextField

    /** Code output directory text field (written by the directory chooser) */
    @FXML
    lateinit var locationTextField: TextField

    /** "Choose directory" button; triggers [openFileChooser] */
    @FXML
    lateinit var openButton: Button

    /** Author text field, corresponding to the `@author` placeholder in templates */
    @FXML
    lateinit var authorTextField: TextField

    /** Version text field, corresponding to the `@since` placeholder in templates */
    @FXML
    lateinit var versionTextField: TextField

    /** Configuration VO bidirectionally bound to the UI */
    val config = Config()
    /** Current user's home directory, used to locate the properties file */
    private val userHome = System.getProperty("user.home")
    /** Persisted config file path: `~/.kudos/CodeGenerator.properties` */
    private val propertiesFile = File("$userHome/.kudos/CodeGenerator.properties")
    /** Wrapper for reading/writing [propertiesFile] */
    private lateinit var propertiesLoader: PropertiesLoader
    /** Module name suggestion set, for future autocompletion */
    private var moduleSuggestions: Set<String?>? = null

    /**
     * Callback invoked by JavaFX after the FXML is loaded; prepares the UI in the order:
     * "load config -> bind properties -> init dropdown -> init autocompletion".
     *
     * @param location FXML file location (unused)
     * @param resources i18n resources (unused)
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
     * Reads the "module-name suggestions" list from the properties, splitting on comma for later autocompletion.
     *
     * @author K
     * @since 1.0.0
     */
    private fun initAutoCompletion() {
        val moduleSuggestionsStr = propertiesLoader.getProperty(Config.PROP_KEY_MODULE_SUGGESTIONS, "")
        moduleSuggestions = moduleSuggestionsStr.split(",").toHashSet()
    }

    /**
     * Loads every configuration entry from [propertiesFile] into [config].
     * Template info is handled separately: both name and rootDir of [Config.TemplateNameAndRootDir] are taken from
     * the ROOT_DIR field, because older config files did not store the template name independently.
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
     * Bidirectionally binds each FXML input control's `textProperty` to the corresponding JavaFX property on [config].
     * UI edits are immediately reflected in [config] and vice versa, eliminating manual synchronization code.
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
     * Scans subdirectories under `resources/main/templates/` and fills them into the dropdown as template candidates.
     * The first entry is selected by default so the dropdown is never initially empty, which would otherwise trip the
     * "no template selected" validation.
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
     * Pre-check for the "Next" button: connect to the database, run Flyway, and verify all required fields.
     * Any failure throws [Exception], which the upper-level UI catches and shows via an Alert.
     *
     * Side effects: a successful call actually creates the database / runs migrations and writes the DataSource into
     * [KudosContextHolder], so subsequent steps can read metadata directly.
     *
     * @throws Exception When any validation step fails
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
            throw Exception("Please select a template scheme!")
        }

        // package prefix
        if (packagePrefixTextField.text.isNullOrBlank()) {
            throw Exception("Please fill in the package prefix!")
        }

        // test module
        if (moduleTextField.text.isNullOrBlank()) {
            throw Exception("Please fill in the module name!")
        }

        // test location
        if (locationTextField.text.isNullOrBlank()) {
            throw Exception("The code generation directory does not exist!")
        }

        // author location
        if (authorTextField.text.isNullOrBlank()) {
            throw Exception("Please fill in the author!")
        }

        // version location
        if (versionTextField.text.isNullOrBlank()) {
            throw Exception("Please fill in the version!")
        }
    }

    /**
     * Runs Flyway migrations (for codegen's own metadata tables).
     *
     * - `isBaselineOnMigrate = true`: new databases are baselined automatically to avoid empty-DB errors
     * - `isValidateOnMigrate = false`: validation is permissive so historical scripts can be tweaked locally
     * - `isPlaceholderReplacement = false`: this tool's scripts contain no placeholders, so disable to avoid `${}` misinterpretation
     *
     * @param dataSource Data source established by the previous step
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
     * Internal DB connection test: closes the connection immediately on success; pops an Alert and throws
     * [Exception] on failure. The private version does not show a "connection succeeded" alert, avoiding a
     * redundant popup in the [canGoOn] flow.
     *
     * @throws Exception When JDBC connection throws
     * @author K
     * @since 1.0.0
     */
    private fun _testDbConnection() {
        try {
            RdbKit.newConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword()).use {
                RdbKit.testConnection(it)
            }
        } catch (_: Exception) {
            Alert(Alert.AlertType.ERROR, "Connection failed!").show()
            throw Exception("Cannot connect to the database!")
        }
    }

    /**
     * "Test connection" button callback; unlike [_testDbConnection], a success alert is also shown to the user.
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    private fun testDbConnection() {
        _testDbConnection()
        Alert(Alert.AlertType.INFORMATION, "Connection succeeded!").show()
    }

    /**
     * "Choose directory" button callback: uses [DirectoryChooser] to pick a directory and writes it back to
     * [locationTextField]. If the previously saved directory still exists, it is used as the dialog's starting point.
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    private fun openFileChooser() {
        val directoryChooser = DirectoryChooser().apply {
            title = "Select the generation directory"
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
     * Writes the current [config] back to [propertiesFile] so it can be restored on the next launch.
     * Write failures only print the stack trace, never throw — failing to persist the config should not block
     * the code generation flow.
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
     * Loads or initializes [propertiesFile].
     * When the file does not exist, creates the parent directory and seeds reasonable defaults (a local H2,
     * the current user and version 1.0.0) so the main flow works out of the box without editing the config first.
     */
    private val properties: Properties
        get() {
            val properties = Properties()
            if (!propertiesFile.exists()) { // First use: preset component defaults
                val parentFile = propertiesFile.parentFile
                if (!parentFile.exists()) {
                    if (!parentFile.mkdir()) {
                        throw Exception(parentFile.toString() + " directory creation failed!")
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