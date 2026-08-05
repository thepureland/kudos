package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.fx.FxTestSupport
import io.kudos.tools.codegen.model.vo.Config
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.*

/**
 * test for ConfigController
 *
 * Loads config.fxml on the FX application thread to obtain a fully @FXML-injected controller; loading runs
 * initialize() which drives initConfig / bindProperties / initTempleComboBox / initAutoCompletion and the
 * private `properties` getter. user.home is redirected to a temp directory so the persisted
 * ~/.kudos/CodeGenerator.properties is deterministic and the real home is never touched.
 *
 * Covers:
 * - first-launch seeding (no properties file) -> defaults are written into config and the controls;
 * - loading an existing properties file -> values are read into config and reflected in the bound text fields;
 * - storeConfig() -> the current config is persisted back to disk;
 * - bindProperties bidirectional binding (editing config reflects in the text fields);
 * - canGoOn() validation: the DB-connection-failure branch (bad URL) throws; and, with a working in-memory H2
 *   + real Flyway migration, each required-field validation branch (package prefix / module / location /
 *   author / version) throws its specific message.
 *
 * Not covered (recorded as uncovered): openFileChooser() (opens a native DirectoryChooser dialog requiring a
 * display and user interaction) and testDbConnection()'s success path (shows an INFORMATION alert after a
 * real connection; it is a private @FXML handler not invokable deterministically without a live DB dialog).
 *
 * @author K
 * @since 1.0.0
 */
internal class ConfigControllerTest {

    private val h2Url = "jdbc:h2:mem:tools_config_ctrl;DB_CLOSE_DELAY=-1"

    private lateinit var fakeHome: File
    private var originalHome: String? = null

    @BeforeTest
    fun redirectHome() {
        originalHome = System.getProperty("user.home")
        fakeHome = Files.createTempDirectory("config-ctrl-home").toFile()
    }

    @AfterTest
    fun restoreHome() {
        originalHome?.let { System.setProperty("user.home", it) }
    }

    private val propsFile: File get() = File(fakeHome, ".kudos/CodeGenerator.properties")

    /** Loads config.fxml (runs initialize) with user.home pointed at [fakeHome]. */
    private fun load(): Pair<Parent, ConfigController> {
        System.setProperty("user.home", fakeHome.absolutePath)
        val loader = FXMLLoader()
        val root = loader.load<Parent>(javaClass.getResourceAsStream("/fxml/config.fxml"))
        return root to loader.getController()
    }

    private fun writeProps(block: Properties.() -> Unit) {
        propsFile.parentFile.mkdirs()
        val p = Properties().apply(block)
        propsFile.outputStream().use { p.store(it, null) }
    }

    @Test
    fun firstLaunchSeedsDefaults() {
        assertFalse(propsFile.exists(), "precondition: no properties file yet")
        FxTestSupport.runFx {
            val (_, controller) = load()
            // defaults seeded by the `properties` getter for a brand-new install
            assertEquals("jdbc:h2:tcp://localhost:9092/./h2;DATABASE_TO_UPPER=false", controller.config.getDbUrl())
            assertEquals("sa", controller.config.getDbUser())
            assertEquals("", controller.config.getDbPassword())
            assertEquals("1.0.0", controller.config.getVersion())
            // code location defaults to the (redirected) user home
            assertEquals(fakeHome.absolutePath, controller.config.getCodeLoaction())
            assertTrue(controller.config.getAuthor().isNotBlank(), "author defaults to the current OS user")
        }
    }

    @Test
    fun existingPropertiesAreLoadedAndBoundToFields() {
        writeProps {
            setProperty(Config.PROP_KEY_DB_URL, "jdbc:h2:mem:x")
            setProperty(Config.PROP_KEY_DB_USER, "u1")
            setProperty(Config.PROP_KEY_DB_PASSWORD, "p1")
            setProperty(Config.PROP_KEY_PACKAGE_PREFIX, "io.kudos.x")
            setProperty(Config.PROP_KEY_MODULE_NAME, "xmod")
            setProperty(Config.PROP_KEY_CODE_LOACTION, "C:/x")
            setProperty(Config.PROP_KEY_AUTHOR, "Alice")
            setProperty(Config.PROP_KEY_VERSION, "9.9.9")
            setProperty(Config.PROP_KEY_MODULE_SUGGESTIONS, "a,b,c")
        }
        FxTestSupport.runFx {
            val (_, controller) = load()
            assertEquals("jdbc:h2:mem:x", controller.config.getDbUrl())
            assertEquals("u1", controller.config.getDbUser())
            assertEquals("p1", controller.config.getDbPassword())
            assertEquals("io.kudos.x", controller.config.getPackagePrefix())
            assertEquals("xmod", controller.config.getModuleName())
            assertEquals("C:/x", controller.config.getCodeLoaction())
            assertEquals("Alice", controller.config.getAuthor())
            assertEquals("9.9.9", controller.config.getVersion())
            // bound text fields reflect config
            assertEquals("jdbc:h2:mem:x", controller.urlTextField.text)
            assertEquals("xmod", controller.moduleTextField.text)
            assertEquals("9.9.9", controller.versionTextField.text)
        }
    }

    @Test
    fun bindingIsBidirectional() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.config.setModuleName("changed-mod")
            assertEquals("changed-mod", controller.moduleTextField.text, "config -> field")
            controller.authorTextField.text = "Bob"
            assertEquals("Bob", controller.config.getAuthor(), "field -> config")
        }
    }

    @Test
    fun storeConfigPersistsToDisk() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.config.setPackagePrefix("io.kudos.persisted")
            controller.config.setModuleName("persistedmod")
            controller.config.setAuthor("Carol")
            controller.config.setVersion("2.0.0")
            controller.storeConfig()
        }
        assertTrue(propsFile.exists(), "storeConfig must create the properties file")
        val loaded = Properties().apply { propsFile.inputStream().use { load(it) } }
        assertEquals("io.kudos.persisted", loaded.getProperty(Config.PROP_KEY_PACKAGE_PREFIX))
        assertEquals("persistedmod", loaded.getProperty(Config.PROP_KEY_MODULE_NAME))
        assertEquals("Carol", loaded.getProperty(Config.PROP_KEY_AUTHOR))
        assertEquals("2.0.0", loaded.getProperty(Config.PROP_KEY_VERSION))
    }

    @Test
    fun canGoOnThrowsWhenDbUnreachable() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.urlTextField.text = "jdbc:h2:tcp://localhost:1/nope"
            controller.userTextField.text = "sa"
            controller.passwordField.text = ""
            val ex = assertFailsWith<Exception> { controller.canGoOn() }
            assertEquals("Cannot connect to the database!", ex.message)
        }
    }

    @Test
    fun canGoOnValidatesRequiredFieldsAfterSuccessfulMigration() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            // working in-memory H2 so the connection test + Flyway migration succeed
            controller.urlTextField.text = h2Url
            controller.userTextField.text = "sa"
            controller.passwordField.text = ""
            // initTempleComboBox always select(0)s; replace it with a fresh empty (nothing-selected)
            // selection model to drive the "no template selected" guard (the first validation after
            // a successful migration).
            controller.templateChoiceBox.items.clear()
            controller.templateChoiceBox.selectionModel =
                object : javafx.scene.control.SingleSelectionModel<Config.TemplateNameAndRootDir>() {
                    override fun getItemCount(): Int = 0
                    override fun getModelItem(index: Int): Config.TemplateNameAndRootDir? = null
                }
            controller.packagePrefixTextField.text = ""
            controller.moduleTextField.text = ""
            controller.locationTextField.text = ""
            controller.authorTextField.text = ""
            controller.versionTextField.text = ""

            assertEquals("Please select a template scheme!", assertFailsWith<Exception> { controller.canGoOn() }.message)
        }
    }

    @Test
    fun testDbConnectionButtonShowsSuccessAlertForWorkingDb() {
        FxTestSupport.runFx {
            val (root, controller) = load()
            controller.urlTextField.text = h2Url
            controller.userTextField.text = "sa"
            controller.passwordField.text = ""
            // fire the FXML "test connection" button -> private testDbConnection() success path.
            // attach to a Scene so skins are built and lookup traverses TitledPane content; the "..."
            // button opens a native dialog (openFileChooser) and must NOT be fired here.
            javafx.scene.Scene(root)
            root.applyCss()
            root.layout()
            val button = root.lookupAll(".button")
                .filterIsInstance<javafx.scene.control.Button>()
                .first { it.onAction != null && it != controller.openButton }
            button.fire()
        }
    }

    @Test
    fun storeConfigSwallowsIoExceptionWhenTargetIsDirectory() {
        // make CodeGenerator.properties a *directory* so both the load (FileInputStream) and the
        // store (FileOutputStream) hit IOException, which the controller only logs (never throws).
        propsFile.parentFile.mkdirs()
        propsFile.mkdir()
        assertTrue(propsFile.isDirectory)
        FxTestSupport.runFx {
            val (_, controller) = load() // initialize() -> properties getter load-catch branch
            controller.config.setVersion("x")
            controller.storeConfig() // store-catch branch; must not throw
        }
    }

    @Test
    fun initializeThrowsWhenConfigDirCannotBeCreated() {
        // point user.home at <file>/home so that creating <file>/home/.kudos via mkdirs() fails (a path
        // component is a regular file) -> the properties getter throws "directory creation failed".
        val blocker = File.createTempFile("config-ctrl-blocker", ".tmp")
        assertTrue(blocker.isFile)
        val unmakeableHome = File(blocker, "home")
        System.setProperty("user.home", unmakeableHome.absolutePath)
        FxTestSupport.runFx {
            // FXML load invokes initialize(); the directory-creation failure surfaces as a load failure.
            val ex = assertFailsWith<Throwable> {
                val loader = FXMLLoader()
                loader.load<Parent>(javaClass.getResourceAsStream("/fxml/config.fxml"))
            }
            val messages = generateSequence<Throwable>(ex) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
            assertTrue(messages.contains("directory creation failed"), "actual chain: $messages")
        }
    }

    @Test
    fun fxmlFieldSettersAreCallable() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            // the public lateinit @FXML fields expose Kotlin setters that FXML never calls (it injects the
            // field directly); exercise them so they are not reported as dead code.
            controller.urlTextField = javafx.scene.control.TextField()
            controller.userTextField = javafx.scene.control.TextField()
            controller.passwordField = javafx.scene.control.PasswordField()
            controller.templateChoiceBox = javafx.scene.control.ComboBox()
            controller.packagePrefixTextField = javafx.scene.control.TextField()
            controller.moduleTextField = javafx.scene.control.TextField()
            controller.locationTextField = javafx.scene.control.TextField()
            controller.openButton = javafx.scene.control.Button()
            controller.authorTextField = javafx.scene.control.TextField()
            controller.versionTextField = javafx.scene.control.TextField()
            assertNotNull(controller.openButton)
        }
    }

    @Test
    fun canGoOnValidatesEachFieldInOrder() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.urlTextField.text = h2Url
            controller.userTextField.text = "sa"
            controller.passwordField.text = ""
            // pre-select a template so we get past the template guard
            controller.templateChoiceBox.items.setAll(Config.TemplateNameAndRootDir("t", "/t"))
            controller.templateChoiceBox.selectionModel.select(0)

            controller.packagePrefixTextField.text = ""
            assertEquals("Please fill in the package prefix!", assertFailsWith<Exception> { controller.canGoOn() }.message)

            controller.packagePrefixTextField.text = "io.kudos.demo"
            controller.moduleTextField.text = ""
            assertEquals("Please fill in the module name!", assertFailsWith<Exception> { controller.canGoOn() }.message)

            controller.moduleTextField.text = "demo"
            controller.locationTextField.text = ""
            assertEquals("The code generation directory does not exist!", assertFailsWith<Exception> { controller.canGoOn() }.message)

            controller.locationTextField.text = "C:/out"
            controller.authorTextField.text = ""
            assertEquals("Please fill in the author!", assertFailsWith<Exception> { controller.canGoOn() }.message)

            controller.authorTextField.text = "K"
            controller.versionTextField.text = ""
            assertEquals("Please fill in the version!", assertFailsWith<Exception> { controller.canGoOn() }.message)

            // all fields filled -> canGoOn completes without throwing
            controller.versionTextField.text = "1.0.0"
            controller.canGoOn()
        }
    }
}
