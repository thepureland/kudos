package io.kudos.tools.codegen

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.tools.codegen.model.vo.Config
import javafx.scene.control.SingleSelectionModel
import org.h2.jdbcx.JdbcDataSource
import org.ktorm.database.Database
import java.io.File
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Shared test support for codegen tests:
 * - creates in-memory H2 databases initialized with the module's own code_gen_* DDL and binds them
 *   to [KudosContextHolder] (the same lookup path production DAOs use), so DAO/service/core logic
 *   can be tested without a Spring container or any external infrastructure;
 * - builds [Config] instances (whose templateInfo requires a JavaFX [SingleSelectionModel]);
 * - resolves classpath resource directories to filesystem paths for template-based tests.
 *
 * @author K
 * @since 1.0.0
 */
internal object CodegenTestSupport {

    /**
     * Creates an in-memory H2 database named [dbName], runs the codegen metadata DDL plus
     * [extraDdls], and binds both the DataSource and a fresh ktorm Database to the current
     * thread context (overwriting any previously bound ones, so test classes stay independent).
     */
    fun bindNewH2(dbName: String, vararg extraDdls: String): DataSource {
        val ds = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1")
            user = "sa"
        }
        ds.connection.use { conn ->
            conn.createStatement().use { st ->
                CODEGEN_DDL_RESOURCES.forEach { resource ->
                    val sql = checkNotNull(this::class.java.getResource(resource)) {
                        "missing DDL resource: $resource"
                    }.readText()
                    st.execute(sql)
                }
                extraDdls.forEach { st.execute(it) }
            }
        }
        bindDataSourceToCurrentThread(ds)
        return ds
    }

    /**
     * Binds [ds] (and a fresh ktorm Database over it) to the *current thread's* context. Needed when the
     * code under test runs on another thread (e.g. the JavaFX application thread): the context is an
     * InheritableThreadLocal, so a thread that already existed before bindNewH2 ran would not see the
     * binding. Call this on the thread that will actually execute the DAO/metadata access.
     */
    fun bindDataSourceToCurrentThread(ds: DataSource) {
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATA_SOURCE to ds,
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect(ds, alwaysQuoteIdentifiers = true)
        )
    }

    /** Executes each SQL statement against [ds]. */
    fun executeSql(ds: DataSource, vararg sqls: String) {
        ds.connection.use { conn ->
            conn.createStatement().use { st -> sqls.forEach { st.execute(it) } }
        }
    }

    /** Runs [sql] and maps every row with [mapper]. */
    fun <T> query(ds: DataSource, sql: String, mapper: (ResultSet) -> T): List<T> =
        ds.connection.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    val result = mutableListOf<T>()
                    while (rs.next()) result.add(mapper(rs))
                    result
                }
            }
        }

    /** Builds a [Config] whose JavaFX-backed properties are filled without any UI. */
    fun newConfig(
        templateName: String = "kudos",
        templateRootDir: String = "",
        packagePrefix: String = "io.kudos.demo",
        moduleName: String = "demo",
        codeLocation: String = "",
        author: String = "K",
        version: String = "1.0.0",
    ): Config = Config().apply {
        templateInfoProperty().set(FixedSelectionModel(Config.TemplateNameAndRootDir(templateName, templateRootDir)))
        setPackagePrefix(packagePrefix)
        setModuleName(moduleName)
        setCodeLoaction(codeLocation)
        setAuthor(author)
        setVersion(version)
    }

    /** Resolves a classpath directory (e.g. "/templates/xxx") to a forward-slash filesystem path. */
    fun classpathDirPath(resourcePath: String): String {
        val url = checkNotNull(this::class.java.getResource(resourcePath)) {
            "missing classpath resource: $resourcePath"
        }
        return File(url.toURI()).absolutePath.replace('\\', '/')
    }

    /** Selection model that always holds exactly one item; enough for [Config.getTemplateInfo]. */
    private class FixedSelectionModel(item: Config.TemplateNameAndRootDir) :
        SingleSelectionModel<Config.TemplateNameAndRootDir>() {
        private val items = listOf(item)
        override fun getModelItem(index: Int): Config.TemplateNameAndRootDir? = items.getOrNull(index)
        override fun getItemCount(): Int = items.size

        init {
            select(0)
        }
    }

    private val CODEGEN_DDL_RESOURCES = listOf(
        "/sql/codegen/h2/V1.0.0.1__init_code_gen_file.sql",
        "/sql/codegen/h2/V1.0.0.2__init_code_gen_object.sql",
        "/sql/codegen/h2/V1.0.0.3__init_code_gen_column.sql",
    )
}
