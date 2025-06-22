package io.kudos.ability.data.rdb.flyway.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.logger.LogFactory
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import java.sql.Connection
import javax.sql.DataSource


/**
 * flyway工具类（without Spring）
 *
 * 方便非spring上下文中调用，如：代码生成器
 *
 * @author K
 * @since 1.0.0
 */
object FlywayKit {

    private val log = LogFactory.getLog(this)

    const val SQL_ROOT_PATH = "sql"

    /**
     * 升级数据库脚本
     *
     * @param moduleName 模块名(SQL_ROOT_PATH直接子目录)
     * @param dataSource 数据源对象
     * @param flywayProperties FlywayProperties
     */
    fun migrate(moduleName: String, dataSource: DataSource, flywayProperties: FlywayProperties) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val dbType = RdbKit.determinRdbTypeByUrl(connection.metaData.url).name.lowercase()
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .table("flyway_history_$moduleName")
                .locations("classpath:$SQL_ROOT_PATH/$moduleName/$dbType")
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