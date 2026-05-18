package io.kudos.ability.data.rdb.flyway.multidatasource

import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.base.io.FileKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import java.io.File


/**
 * Flyway 多数据源脚本升级器。
 *
 * 约定 / 行为：
 * 1. SQL 脚本按"模块名 + 数据库类型"分目录存放在 classpath 下，路径形如
 *    `sql/<moduleName>/<dbType>/V1.0.1__xxx.sql`，其中 `<dbType>` 是
 *    `io.kudos.ability.data.rdb.jdbc.consts.RdbTypeEnum#name` 的小写
 * 2. SQL 脚本文件名遵循 Flyway 规范（V_x__xxx.sql / R__xxx.sql ...）
 * 3. 模块升级顺序由 `kudos.ability.flyway.datasource-config` 的声明顺序决定
 * 4. 启动时若发现某模块名在 classpath 多次出现（例如分布在不同 jar），会直接报错中断
 * 5. 任意一个模块迁移失败，剩余模块的升级被中断（由 [FlywayKit] 抛异常向上传播）
 * 6. 支持一次性升级多个不同类型的关系型数据库（dbType 由各模块对应数据源的元数据决定）
 *
 * 耦合点：通过 [DsContextProcessor] 拿到具体数据源实例，因此目前**依赖 baomidou
 * dynamic-datasource starter** 提供的动态路由数据源。
 *
 * @author K
 * @since 1.0.0
 */
open class FlywayMultiDataSourceMigrator {

    @Resource
    private lateinit var flywayMultiDatasourceProperties: FlywayMultiDataSourceProperties

    @Resource
    private lateinit var flywayProperties: FlywayProperties

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    private val log = LogFactory.getLog(this::class)

    /**
     * 主入口：扫描 classpath、与 properties 中声明的模块对比、按顺序逐模块升级。
     * 任何模块失败都会向上抛出（不 swallow），从而打断 Spring 启动。
     */
    fun migrate() {
        val moduleNamesOnDisk = scanModuleNamesFromClasspath()
        val configuredModules = flywayMultiDatasourceProperties.datasourceConfig.sequencedKeySet()

        val orphanModules = configuredModules - moduleNamesOnDisk
        if (orphanModules.isNotEmpty()) {
            log.warn("以下 kudos.ability.flyway.datasource-config 中配置的模块，实际并不存在于 sql 目录下：$orphanModules")
        }

        val toMigrate = configuredModules - orphanModules
        toMigrate.forEach { module ->
            val datasourceKey = checkNotNull(flywayMultiDatasourceProperties.getDataSourceKey(module)) {
                "datasource key missing for module: $module"
            }
            migrateByModule(module, datasourceKey)
        }
    }

    /**
     * 扫 classpath 上 `sql/` 直接子目录，对照配置过滤出"既在磁盘又在 properties 里"的模块名。
     *
     * 重复检测语义：若同名模块在多个 classpath URL（不同 jar / 不同 source set）都出现一次，
     * 会抛 [IllegalStateException]。Flyway 不支持同一 schema 历史表来自两套不同 source，
     * 这里把它当致命错误，避免运行期出现混乱迁移。
     */
    private fun scanModuleNamesFromClasspath(): Set<String> {
        val sqlRootPath = FlywayKit.SQL_ROOT_PATH
        val locationUrls = ClassPathScanner.getLocationUrlsForPath(sqlRootPath)
        val moduleNames = mutableSetOf<String>()
        locationUrls.forEach { url ->
            val childFolders = listChildFolders(url, sqlRootPath)
            childFolders.forEach { moduleName ->
                if (moduleNames.contains(moduleName)) {
                    error("存在名字为【$moduleName】的多个模块！请检查 classpath 上是否有重复的 sql/$moduleName 目录")
                }
                val datasourceKey = flywayMultiDatasourceProperties.getDataSourceKey(moduleName)
                if (!datasourceKey.isNullOrBlank()) {
                    moduleNames.add(moduleName)
                } else {
                    log.warn("未配置模块【$moduleName】的数据源！忽略之。模块位置：${url.path}/$moduleName")
                }
            }
        }
        return moduleNames
    }

    /**
     * 返回某个 classpath URL 下 [sqlRootPath] 直接子目录的名字列表；自动区分 jar / 文件系统两种协议。
     */
    private fun listChildFolders(url: java.net.URL, sqlRootPath: String): List<String> {
        return if (url.protocol == "jar") {
            val paths = FileKit.listFilesOrDirsInJar(url.toString().removeSuffix(sqlRootPath), sqlRootPath)
            paths.map { it.removePrefix("$sqlRootPath/").removeSuffix("/") }
        } else {
            File(url.path).listFiles()?.map { it.name }.orEmpty()
        }
    }

    /**
     * 单模块升级。先检查数据源 key 在动态路由表里真的存在，再委托给 [FlywayKit]。
     * 数据源不存在会抛出 [IllegalStateException]，由调用方决定中断/重试策略。
     */
    internal fun migrateByModule(moduleName: String, datasourceKey: String) {
        if (!dsContextProcessor.haveDataSource(datasourceKey)) {
            val errMsg = "模块【$moduleName】配置的数据源【$datasourceKey】不存在！后续所有数据库更新中断！"
            log.error(errMsg)
            error(errMsg)
        }

        val dataSource = checkNotNull(dsContextProcessor.getDataSource(datasourceKey)) {
            "数据源【$datasourceKey】解析为 null"
        }
        FlywayKit.migrate(moduleName, dataSource, flywayProperties)
    }
}
