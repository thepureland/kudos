package io.kudos.ability.data.rdb.ktorm.datasource

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.ktorm.database.Database
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.support.StaticApplicationContext
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Unit tests for the [Database] cache behind [currentDatabase].
 *
 * The cache exists because building a ktorm `Database` borrows a pooled connection and reads about
 * fifteen `DatabaseMetaData` values — not something to repeat per request. What these tests pin is
 * the part that is easy to get wrong while still "working": one `Database` per *physical*
 * datasource, never one shared globally, because that metadata describes the database the
 * connection actually reached.
 *
 * Runs against in-memory H2 with the datasource injected into the kudos context, so it needs
 * neither Docker nor a Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KtormDatabaseCacheTest {

    private fun dataSource(name: String): DataSource =
        DriverManagerDataSource("jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1")

    private fun bind(dataSource: DataSource) {
        KudosContextHolder.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to dataSource)
    }

    @AfterTest
    fun tearDown() {
        clearKtormDatabaseCache()
        // The context is thread-local and this thread is reused by the rest of the suite; leaving a
        // datasource behind in it would silently become some other test's database.
        KudosContextHolder.clear()
    }

    @Test
    fun theSameDataSourceYieldsTheSameDatabase() {
        bind(dataSource("ktorm_db_cache_a"))
        assertSame(
            KudosContextHolder.currentDatabase(), KudosContextHolder.currentDatabase(),
            "rebuilding per call would re-read DatabaseMetaData on every request",
        )
    }

    @Test
    fun differentDataSourcesGetDifferentDatabases() {
        // The reason this is a cache and not a singleton: the metadata ktorm reads describes the
        // database the connection reached, so two physical databases cannot share one Database.
        bind(dataSource("ktorm_db_cache_b"))
        val first = KudosContextHolder.currentDatabase()

        KudosContextHolder.clear()
        bind(dataSource("ktorm_db_cache_c"))
        assertNotSame(first, KudosContextHolder.currentDatabase())
    }

    @Test
    fun clearingForcesTheNextCallToRebuild() {
        bind(dataSource("ktorm_db_cache_d"))
        val first = KudosContextHolder.currentDatabase()
        clearKtormDatabaseCache()
        assertNotSame(first, KudosContextHolder.currentDatabase())
    }

    @Test
    fun aClosingContextDropsEntriesThatWouldOutliveTheirPool() {
        // The failure this prevents: an entry survives the context whose pool its Database was built over,
        // and the next caller gets connections from a pool that has been shut down — which shows up much
        // later as ProxyConnection.close() failing with a null delegate, inside an unrelated query.
        // Weak keys do not prevent it, because a WeakHashMap holds values strongly and this value can
        // reference its own key.
        bind(dataSource("ktorm_db_cache_f"))
        val beforeClose = KudosContextHolder.currentDatabase()

        val context = StaticApplicationContext()
        context.refresh()
        KtormDatabaseCacheEvictor().onApplicationEvent(ContextClosedEvent(context))
        context.close()

        assertNotSame(beforeClose, KudosContextHolder.currentDatabase())
    }

    @Test
    fun aDatabasePinnedInTheContextWins() {
        // How the code generator and tests target a specific database; the cache must not shadow it.
        bind(dataSource("ktorm_db_cache_e"))
        val pinned = Database.connect("jdbc:h2:mem:ktorm_db_cache_pinned;DB_CLOSE_DELAY=-1")
        KudosContextHolder.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATABASE to pinned)
        assertSame(pinned, KudosContextHolder.currentDatabase())
    }
}
