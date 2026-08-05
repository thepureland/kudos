package io.kudos.tools.codegen.model.vo

import javafx.scene.control.SingleSelectionModel
import kotlin.test.*

/**
 * test for Config
 *
 * Covers every string getter/setter/property accessor round-trip, the property-object identity, the
 * SingleSelectionModel-backed templateInfo triad (getter reads the selected item; setter selects on the
 * underlying model; setTemplateInfo is a safe no-op when no model is bound yet), the persistence-key
 * companion constants, and the TemplateNameAndRootDir data class incl. its name-only toString().
 *
 * Config holds plain JavaFX property beans, so no FX toolkit is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConfigTest {

    /** Minimal in-memory selection model so templateInfo can be exercised without any UI control. */
    private class FixedSelectionModel(private val items: List<Config.TemplateNameAndRootDir>) :
        SingleSelectionModel<Config.TemplateNameAndRootDir>() {
        override fun getModelItem(index: Int): Config.TemplateNameAndRootDir? = items.getOrNull(index)
        override fun getItemCount(): Int = items.size
    }

    @Test
    fun stringPropertiesRoundTrip() {
        val config = Config().apply {
            setDbUrl("jdbc:h2:mem:test")
            setDbUser("sa")
            setDbPassword("secret")
            setPackagePrefix("io.kudos.demo")
            setModuleName("demo")
            setCodeLoaction("C:/out")
            setAuthor("K")
            setVersion("1.0.0")
        }
        assertEquals("jdbc:h2:mem:test", config.getDbUrl())
        assertEquals("sa", config.getDbUser())
        assertEquals("secret", config.getDbPassword())
        assertEquals("io.kudos.demo", config.getPackagePrefix())
        assertEquals("demo", config.getModuleName())
        assertEquals("C:/out", config.getCodeLoaction())
        assertEquals("K", config.getAuthor())
        assertEquals("1.0.0", config.getVersion())
    }

    @Test
    fun propertyObjectsReflectAndWriteBack() {
        val config = Config()
        config.dbUrlProperty().set("u")
        config.dbUserProperty().set("user")
        config.dbPasswordProperty().set("pwd")
        config.packagePrefixProperty().set("pkg")
        config.moduleNameProperty().set("mod")
        config.codeLoactionProperty().set("loc")
        config.authorProperty().set("auth")
        config.versionProperty().set("ver")

        assertEquals("u", config.getDbUrl())
        assertEquals("user", config.getDbUser())
        assertEquals("pwd", config.getDbPassword())
        assertEquals("pkg", config.getPackagePrefix())
        assertEquals("mod", config.getModuleName())
        assertEquals("loc", config.getCodeLoaction())
        assertEquals("auth", config.getAuthor())
        assertEquals("ver", config.getVersion())

        // property identity is stable
        assertSame(config.dbUrlProperty(), config.dbUrlProperty())
    }

    @Test
    fun templateInfoSelectsAndReadsThroughSelectionModel() {
        val a = Config.TemplateNameAndRootDir("a", "/root/a")
        val b = Config.TemplateNameAndRootDir("b", "/root/b")
        val config = Config()
        config.templateInfoProperty().set(FixedSelectionModel(listOf(a, b)))

        config.setTemplateInfo(b)
        assertEquals(b, config.getTemplateInfo())

        config.setTemplateInfo(a)
        assertEquals(a, config.getTemplateInfo())
    }

    @Test
    fun setTemplateInfoIsNoOpWhenNoModelBound() {
        val config = Config()
        // model property holds null -> set must not throw (safe-call in production code)
        config.setTemplateInfo(Config.TemplateNameAndRootDir("x", "/x"))
        assertNull(config.templateInfoProperty().get())
    }

    @Test
    fun getTemplateInfoReturnsNullWhenNoModelBound() {
        // freshly constructed Config: templateInfo property holds null (no SelectionModel bound yet).
        // getTemplateInfo() must return null instead of throwing NPE on templateInfo.get().selectedItem.
        val config = Config()
        assertNull(config.templateInfoProperty().get())
        assertNull(config.getTemplateInfo())
    }

    @Test
    fun templateNameAndRootDirToStringReturnsNameOnly() {
        val item = Config.TemplateNameAndRootDir("My Template", "/some/root/dir")
        assertEquals("My Template", item.toString(), "toString must return name for combo box display")
        assertEquals("My Template", item.name)
        assertEquals("/some/root/dir", item.rootDir)
    }

    @Test
    fun templateNameAndRootDirDataClassEqualityAndCopy() {
        val a = Config.TemplateNameAndRootDir("n", "r")
        val b = Config.TemplateNameAndRootDir("n", "r")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(Config.TemplateNameAndRootDir("n2", "r"), a.copy(name = "n2"))
        assertNotEquals(a, Config.TemplateNameAndRootDir("n", "r2"))
    }

    @Test
    fun persistenceKeyConstantsAreStable() {
        // these strings are written to the on-disk properties file; changing them silently breaks
        // backward compatibility, so pin them down.
        assertEquals("dbUrl", Config.PROP_KEY_DB_URL)
        assertEquals("dbUser", Config.PROP_KEY_DB_USER)
        assertEquals("dbPassword", Config.PROP_KEY_DB_PASSWORD)
        assertEquals("templateName", Config.PROP_KEY_TEMPLATE_NAME)
        assertEquals("templateRootDir", Config.PROP_KEY_TEMPLATE_ROOT_DIR)
        assertEquals("packagePrefix", Config.PROP_KEY_PACKAGE_PREFIX)
        assertEquals("module", Config.PROP_KEY_MODULE_NAME)
        assertEquals("moduleNameSuggestions", Config.PROP_KEY_MODULE_SUGGESTIONS)
        assertEquals("codeLoaction", Config.PROP_KEY_CODE_LOACTION)
        assertEquals("author", Config.PROP_KEY_AUTHOR)
        assertEquals("version", Config.PROP_KEY_VERSION)
    }
}
