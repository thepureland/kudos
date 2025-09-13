package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceCreator
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import com.baomidou.dynamic.datasource.ds.ItemDataSource
import com.baomidou.dynamic.datasource.enums.SeataMode
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.support.ScriptRunner
import com.baomidou.dynamic.datasource.toolkit.CryptoUtils
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StringUtils
import javax.sql.DataSource

class DsDataSourceCreator : DefaultDataSourceCreator() {

    private val log = LogFactory.getLog(this)
    private var creators: List<DataSourceCreator>? = null

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    /**
     * 是否懒加载数据源
     */
    private var lazy: Boolean? = false

    /**
     * / **
     * 是否使用p6spy输出，默认不输出
     */
    private var p6spy = false

    /**
     * 全局默认publicKey
     */
    private var publicKey: String? = CryptoUtils.DEFAULT_PUBLIC_KEY_STRING

    private var dataSourceInitEvent: DataSourceInitEvent? = null

    /**
     * 创建数据源
     *
     * @param dataSourceProperty 数据源参数
     * @return 数据源
     */
    override fun createDataSource(dataSourceProperty: DataSourceProperty): DataSource {
        var dataSourceCreator: DataSourceCreator? = null
        for (creator in this.creators!!) {
            if (creator.support(dataSourceProperty)) {
                dataSourceCreator = creator
                break
            }
        }
        checkNotNull(dataSourceCreator) { "creator must not be null,please check the DataSourceCreator" }
        if (dataSourceProxy != null && dataSourceProxy.isSeata) {
            if (dataSourceProperty.getDruid() != null) {
                dataSourceProperty.getDruid().setDefaultAutoCommit(true)
            }
            if (dataSourceProperty.getHikari() != null) {
                dataSourceProperty.getHikari().setIsAutoCommit(true)
            }
            if (dataSourceProperty.getBeecp() != null) {
                dataSourceProperty.getBeecp().setDefaultAutoCommit(true)
            }
            if (dataSourceProperty.getDbcp2() != null) {
                dataSourceProperty.getDbcp2().setDefaultAutoCommit(true)
            }
        }
        val propertyPublicKey = dataSourceProperty.getPublicKey()
        if (StringUtils.isEmpty(propertyPublicKey)) {
            dataSourceProperty.setPublicKey(publicKey)
        }
        val propertyLazy = dataSourceProperty.getLazy()
        if (propertyLazy == null) {
            dataSourceProperty.setLazy(lazy)
        }
        if (dataSourceInitEvent != null) {
            dataSourceInitEvent!!.beforeCreate(dataSourceProperty)
        }
        val dataSource = dataSourceCreator.createDataSource(dataSourceProperty)
        if (dataSourceInitEvent != null) {
            dataSourceInitEvent!!.afterCreate(dataSource)
        }
        this.runScrip(dataSource, dataSourceProperty)
        return wrapDataSource(dataSource, dataSourceProperty)
    }

    /**
     * 执行初始化脚本
     *
     * @param dataSource         数据源
     * @param dataSourceProperty 数据源参数
     */
    private fun runScrip(dataSource: DataSource?, dataSourceProperty: DataSourceProperty) {
        val initProperty = dataSourceProperty.getInit()
        val schema = initProperty.getSchema()
        val data = initProperty.getData()
        if (StringUtils.hasText(schema) || StringUtils.hasText(data)) {
            val scriptRunner = ScriptRunner(initProperty.isContinueOnError(), initProperty.getSeparator())
            if (StringUtils.hasText(schema)) {
                scriptRunner.runScript(dataSource, schema)
            }
            if (StringUtils.hasText(data)) {
                scriptRunner.runScript(dataSource, data)
            }
        }
    }

    /**
     * 包装数据源
     *
     * @param dataSource         数据源
     * @param dataSourceProperty 数据源参数
     */
    private fun wrapDataSource(dataSource: DataSource?, dataSourceProperty: DataSourceProperty): DataSource {
        val name = dataSourceProperty.getPoolName()
        var targetDataSource = dataSource
        var isSeata = false
        var seataMode: SeataMode? = null
        if (dataSourceProxy != null) {
            targetDataSource = dataSourceProxy.proxyDatasource(dataSource)
            isSeata = dataSourceProxy.isSeata
            seataMode = dataSourceProxy.seataMode()
        }
        val enabledP6spy = p6spy && dataSourceProperty.getP6spy()
        return ItemDataSource(name, dataSource, targetDataSource, enabledP6spy, isSeata, seataMode)
    }

    override fun setCreators(creators: List<DataSourceCreator>) {
        this.creators = creators
    }

    override fun setLazy(lazy: Boolean?) {
        this.lazy = lazy
    }

    override fun setP6spy(p6spy: Boolean) {
        this.p6spy = p6spy
    }

    override fun setPublicKey(publicKey: String?) {
        this.publicKey = publicKey
    }

    override fun setDataSourceInitEvent(dataSourceInitEvent: DataSourceInitEvent?) {
        this.dataSourceInitEvent = dataSourceInitEvent
    }
}
