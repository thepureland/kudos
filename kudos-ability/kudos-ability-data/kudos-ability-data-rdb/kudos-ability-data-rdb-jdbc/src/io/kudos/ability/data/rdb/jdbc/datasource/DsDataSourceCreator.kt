package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.ds.ItemDataSource
import com.baomidou.dynamic.datasource.enums.SeataMode
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.support.ScriptRunner
import com.baomidou.dynamic.datasource.toolkit.CryptoUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import javax.sql.DataSource

/**
 * Custom baomidou [DefaultDataSourceCreator] that adds on top of baomidou:
 *  - Runs schema/data scripts at startup according to [DataSourceProperty.init] (via [runScrip]).
 *  - Wraps the created DataSource via [IDataSourceProxy] (via [wrapDataSource]).
 *  - **Forces autoCommit to true everywhere** when the proxy is in Seata mode
 *    (see the in-method comment in [createDataSource]).
 *
 * Bound as a Spring singleton (`@Component`); baomidou injects the per-type
 * downstream creators (Hikari / Druid / DBCP2 / BeeCP) via `setCreators`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
class DsDataSourceCreator : DefaultDataSourceCreator() {

    private var creators: List<DataSourceCreator>? = null

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    /** Whether to lazy-load data sources (baomidou global lazy configuration). */
    private var lazy: Boolean? = false

    /** Whether to enable p6spy SQL logging output (baomidou global p6spy configuration). */
    private var p6spy = false

    /** Global default publicKey used by baomidou to encrypt/decrypt sensitive configuration items (e.g. password). */
    private var publicKey: String? = CryptoUtils.DEFAULT_PUBLIC_KEY_STRING

    /** baomidou's DataSource initialization event hook; may be null. */
    private var dataSourceInitEvent: DataSourceInitEvent? = null

    /**
     * Creates a data source. Flow:
     *  1. Pick from the registered creators the one whose `support` matches the
     *     current [DataSourceProperty] (Hikari/Druid/...).
     *  2. **Seata compatibility**: if the proxy is in Seata mode, force autoCommit
     *     to true, overriding any yml setting. Seata AT mode relies on the
     *     ConnectionProxy interceptor chain firing when each SQL auto-commits to
     *     write the undo log / register branch; with autoCommit=false the chain
     *     does not trigger and writes get rolled back when the connection returns
     *     to the pool. See the failure investigation notes in the
     *     `kudos-ability-distributed-tx-seata` module.
     *  3. Decrypt publicKey / set lazy defaults.
     *  4. Go through [DataSourceInitEvent]'s beforeCreate -> creator.createDataSource -> afterCreate.
     *  5. Run the schema / data initialization scripts.
     *  6. Wrap the proxy and attach ItemDataSource metadata.
     */
    override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource {
        val dataSourceCreator = checkNotNull(
            checkNotNull(creators) { "creators must be set" }
                .firstOrNull { it.support(dataSourceProperty) }
        ) { "creator must not be null,please check the DataSourceCreator" }
        if (dataSourceProxy != null && dataSourceProxy.isSeata()) {
            // !! Hard constraint of Seata AT mode — do not remove !!
            // Seata's ConnectionProxy only triggers undo-log writes + BranchRegister
            // when each SQL auto-commits. autoCommit=false leaves all SQL stuck inside
            // an implicit open transaction, which Hikari rolls back when the
            // connection is returned to the pool — losing all writes.
            dataSourceProperty.druid?.defaultAutoCommit = true
            dataSourceProperty.hikari?.isAutoCommit = true
            dataSourceProperty.beecp?.defaultAutoCommit = true
            dataSourceProperty.dbcp2?.defaultAutoCommit = true
        }
        val propertyPublicKey = dataSourceProperty.publicKey
        if (propertyPublicKey.isEmpty()) {
            dataSourceProperty.publicKey = publicKey
        }
        val propertyLazy = dataSourceProperty.lazy
        if (propertyLazy == null) {
            dataSourceProperty.lazy = lazy
        }
        dataSourceInitEvent?.beforeCreate(dataSourceProperty)
        val dataSource = dataSourceCreator.createDataSource(dataSourceProperty)
        dataSourceInitEvent?.afterCreate(dataSource)
        this.runScrip(dataSource, dataSourceProperty)
        return wrapDataSource(dataSource, dataSourceProperty)
    }

    /**
     * Runs schema / data scripts for a newly created data source during startup
     * (if declared in [DataSourceProperty.init]). baomidou's [ScriptRunner] runs
     * SQL one statement at a time; schema before data.
     */
    private fun runScrip(dataSource: DataSource?, dataSourceProperty: DataSourceProperty) {
        val initProperty = dataSourceProperty.init
        val schema = initProperty.schema
        val data = initProperty.data
        if (StringUtils.hasText(schema) || StringUtils.hasText(data)) {
            val scriptRunner = ScriptRunner(initProperty.isContinueOnError, initProperty.separator)
            if (StringUtils.hasText(schema)) {
                scriptRunner.runScript(dataSource, schema)
            }
            if (StringUtils.hasText(data)) {
                scriptRunner.runScript(dataSource, data)
            }
        }
    }

    /**
     * Wraps the original DataSource through [IDataSourceProxy] (typically a Seata
     * proxy) and constructs baomidou's [ItemDataSource] — keeping references to
     * both the original and the proxy, the Seata-mode signal, and the p6spy flag.
     */
    private fun wrapDataSource(dataSource: DataSource, dataSourceProperty: DataSourceProperty): DataSource {
        val name = dataSourceProperty.poolName
        var targetDataSource = dataSource
        var isSeata = false
        var seataMode: SeataMode? = null
        if (dataSourceProxy != null) {
            targetDataSource = dataSourceProxy.proxyDatasource(dataSource)
            isSeata = dataSourceProxy.isSeata()
            seataMode = dataSourceProxy.seataMode()
        }
        val enabledP6spy = p6spy && dataSourceProperty.p6spy
        return ItemDataSource(name, dataSource, targetDataSource, enabledP6spy, isSeata, seataMode)
    }

    /** Entry point for baomidou to inject the list of downstream per-type DataSourceCreators. */
    override fun setCreators(creators: List<DataSourceCreator>) {
        this.creators = creators
    }

    /** Sets the global lazy flag. */
    override fun setLazy(lazy: Boolean?) {
        this.lazy = lazy
    }

    /** Sets the global p6spy flag. */
    override fun setP6spy(p6spy: Boolean) {
        this.p6spy = p6spy
    }

    /** Sets the global publicKey (used by baomidou for sensitive-configuration decryption). */
    override fun setPublicKey(publicKey: String?) {
        this.publicKey = publicKey
    }

    /** Sets the baomidou DataSource initialization event hook. */
    override fun setDataSourceInitEvent(dataSourceInitEvent: DataSourceInitEvent?) {
        this.dataSourceInitEvent = dataSourceInitEvent
    }
}
