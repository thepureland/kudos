package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.io.FileKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.logger.LogFactory
import org.flywaydb.core.Flyway
import org.soul.ability.data.rdb.jdbc.datasource.DsContextProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import java.io.File
import java.sql.Connection


/**
 * Flyway多数据源脚本升级器
 *
 * 1.sql脚本按模块名和数据库类型(RdbTypeEnum::name的小写)存放于类路径的sql目录下，如：sql/sys/h2/V1.0.1__xxxx.sql
 * 2.sql脚本的文件名需符合flyway规范
 * 3.模块升级顺序为kudos.ability.flyway.datasource-config中的顺序
 * 4.升级前检测到同名模块存在，将退出升级
 * 5.升级过程中，某一模块出错，将中断后续所有模块的升级
 * 6.支持同时升级多个不同类型的关系型数据库
 *
 * @author K
 * @since 1.0.0
 */
open class FlywayMultiDatasourceMigrator {

    @Autowired
    private lateinit var flywayMultiDatasourceProperties: FlywayMultiDatasourceProperties

    @Autowired
    private lateinit var flywayProperties: FlywayProperties

    @Autowired
    private lateinit var dsContextProcessor: DsContextProcessor

    private val log = LogFactory.getLog(this)

    private val sqlRootPath = "sql"

    fun migrate() {
        val locationUrls = ClassPathScanner.getLocationUrlsForPath(sqlRootPath)

        // 检测所有合法的模块
        val moduleNames = mutableListOf<String>()
        locationUrls.forEach { url ->
            val path = url.path
            val childFolders = if (url.protocol == "jar") {
                val pathes = FileKit.listFilesOrDirsInJar(url.toString().removeSuffix(sqlRootPath), sqlRootPath)
                pathes.map { it.removePrefix("$sqlRootPath/").removeSuffix("/") }
            } else {
                File(path).listFiles().map { it.name }
            }
            childFolders.forEach { moduleName ->
                if (!moduleNames.contains(moduleName)) {
                    val datasourceKey = flywayMultiDatasourceProperties.getDataSourceKey(moduleName)
                    if (!datasourceKey.isNullOrBlank()) {
                        moduleNames.add(moduleName)
                    } else {
                        log.warn("未配置模块【$moduleName】的数据源！忽略之。模块位置：${url.path}/$moduleName")
                    }
                } else {
                    error("存在名字为【${moduleName}】的多个模块！")
                }
            }
        }

        // 排除kudos.ability.flyway.datasource-config中有定义的，但实际代码中没有的模块
        val modules = flywayMultiDatasourceProperties.datasourceConfig.sequencedKeySet()
        val diffModules = modules - moduleNames
        if (diffModules.isNotEmpty()) {
            log.warn("以下kudos.ability.flyway.datasource-config中配置的模块，实际并不存在于sql目录下：$diffModules")
        }
        val finalModuleNames = modules - diffModules

        // 升级各模块的sql脚本
        finalModuleNames.forEach {
            val datasourceKey = flywayMultiDatasourceProperties.getDataSourceKey(it)!!
            migrateByModule(it, datasourceKey)
        }

    }

    internal fun migrateByModule(moduleName: String, datasourceKey: String) {
        if (!dsContextProcessor.haveDataSource(datasourceKey)) {
            val errMsg = "模块【$moduleName】配置的数据源【$datasourceKey】不存在！后续所有数据库更新中断！"
            log.error(errMsg)
            error(errMsg)
        }

        val datasource = dsContextProcessor.getDataSource(datasourceKey)
        var connection: Connection? = null
        try {
            connection = datasource.connection
            val dbType = RdbKit.determinRdbTypeByUrl(connection.metaData.url).name.lowercase()
            val flyway = Flyway.configure()
                .dataSource(datasource)
                .table("flyway_history_$moduleName")
                .locations("classpath:$sqlRootPath/$moduleName/$dbType")
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate)
                .baselineVersion(flywayProperties.baselineVersion)
                .encoding(flywayProperties.encoding)
                .outOfOrder(flywayProperties.isOutOfOrder)
                .validateOnMigrate(flywayProperties.isValidateOnMigrate)
                .placeholderReplacement(flywayProperties.isPlaceholderReplacement)
                .load()
            log.info(">>>>>>>>>>>>>  开始升级模块【${moduleName}】的数据库...")
            val result = flyway.migrate()
            if (result.success) {
                val migrationCount = result.migrationsExecuted
                if (migrationCount == 0) {
                    log.info("<<<<<<<<<<<<<  模块【$moduleName】数据库已为最新，无更新任何sql文件。")
                } else {
                    log.info("<<<<<<<<<<<<<  模块【$moduleName】数据库升级完成，共执行了${migrationCount}个sql文件，最新版本为：${result.targetSchemaVersion}")
                }
            } else {
                log.error("flyway升级模块【${moduleName}】的数据库失败！")
            }
        } catch (e: Exception) {
            log.error(e, "flyway升级模块【${moduleName}】数据库出错！")
            throw e
        } finally {
            connection?.close()
        }
    }

}