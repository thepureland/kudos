package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import io.kudos.context.core.KudosContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.*
import javax.sql.DataSource

/**
 * 上下文dataSourceId数据源解析处理器
 *
 * @author damon
 */
class DsContextProcessor {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var dataSourceCreator: DefaultDataSourceCreator

    @Autowired
    private lateinit var dynamicDataSourceLoad: IDynamicDataSourceLoad

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    @Autowired(required = false)
    private val dataSourceFinder: IDataSourceFinder? = null

    @Value("\${spring.datasource.dynamic.primary:master}")
    private val primary: String? = null

    private val keyLockRegistry = KeyLockRegistry<String>()

    fun doDetermineDatasource(dsKey: String, dsKeyConfig: String?): String? {
        if (Objects.isNull(KudosContextHolder.get())) {
            return null
        }
        val context = KudosContextHolder.get()
        if (context._datasourceTenantId == DatasourceConst.CONSOLE_TENANT_ID) {
            return null
        }
        //获取域名指定的默认数据源id
        var defaultDsId = context.dataSourceId
        var mode = DatasourceConst.MODE_MASTER
        //如果是备库
        if (DatasourceKeyTool.isReadOnly(dsKey) && context.readOnlyDataSourceId != null) {
            defaultDsId = context.readOnlyDataSourceId
            mode = DatasourceConst.MODE_READONLY
        }
        keyLockRegistry.tryLock(dsKey)
        try {
            var realDsId: String? = null
            if (dataSourceFinder != null) {
                val serverCode: String? = DatasourceKeyTool.getServerCode(dsKey)
                realDsId = dataSourceFinder.findDataSourceId(context._datasourceTenantId, serverCode, mode)
            }
            if (realDsId == null) {
                realDsId = defaultDsId
            }
            return getDatasourceKey(realDsId)
        } finally {
            keyLockRegistry.unlock(dsKey)
        }
    }

    protected fun getDatasourceKey(dsId: String?): String? { //TODO
        val dataSourceKey = dsId
        val ds: DynamicRoutingDataSource = dataSource as DynamicRoutingDataSource
        if (!ds.dataSources.containsKey(dataSourceKey)) {
            // 该dsKey数据源未初始化，加载配置并初始化
            val dsProperty: DataSourceProperty? = dynamicDataSourceLoad.getPropertyById(dsId)
            if (dsProperty == null) {
                log.warn("动态数据源id未配置:{0}", dsId)
                throw RuntimeException("动态数据源id未配置!dsId=$dsId")
            }
            log.warn("開始創建並載數據源id={0}...", dsId)
            val dataSource = dataSourceCreator.createDataSource(dsProperty)
            if (dataSourceProxy != null) {
                ds.addDataSource(dataSourceKey, dataSourceProxy.proxyDatasource(dataSource))
            } else {
                ds.addDataSource(dataSourceKey, dataSource)
            }
        }
        return dataSourceKey
    }

    /**
     * 根据数据源id获取数据源key
     *
     * @param dsKey
     */
    fun getDataSource(dsKey: String?): DataSource? {
        return when (dataSource) {
            is DynamicRoutingDataSource -> (dataSource as DynamicRoutingDataSource).getDataSource(dsKey)
            else -> dataSource // 单数据源：走单库逻辑
        }
    }

    fun haveDataSource(dsKey: String?): Boolean {
        return when (dataSource) {
            is DynamicRoutingDataSource -> (dataSource as DynamicRoutingDataSource).dataSources.containsKey(dsKey)
            else -> true // 单数据源：直接认为存在即可，或走单库逻辑
        }
    }

    fun refreshDatasource(dsId: Int?) {
        log.warn("收到刷新數據源id為：{0} 的請求", dsId)
        if (dsId == null) {
            val ds = dataSource as DynamicRoutingDataSource
            val strings: MutableSet<String?> = (dataSource as DynamicRoutingDataSource).dataSources.keys
            for (dsKey in strings) {
                if (primary != dsKey) {
                    ds.removeDataSource(dsKey)
                }
            }
            ds.afterPropertiesSet()
        } else {
            val dataSourceKey: String? = dsId.toString()
            val ds: DynamicRoutingDataSource = dataSource as DynamicRoutingDataSource
            ds.removeDataSource(dataSourceKey)
        }
        DynamicDataSourceAspect.cacheDsCache()
        log.warn("數據源刷新成功...")
    }

    fun currentDataSource(): DataSource? {
        return when (dataSource) {
            is DynamicRoutingDataSource -> (dataSource as DynamicRoutingDataSource).determineDataSource()
            else -> dataSource // 单数据源：走单库逻辑
        }
    }

    private val log = LogFactory.getLog(this)

}
