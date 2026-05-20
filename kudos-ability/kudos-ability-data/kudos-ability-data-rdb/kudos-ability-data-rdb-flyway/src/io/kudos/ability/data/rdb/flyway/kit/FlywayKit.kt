package io.kudos.ability.data.rdb.flyway.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.logger.LogFactory
import org.flywaydb.core.Flyway
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import javax.sql.DataSource


/**
 * Flyway 工具类（脱离 Spring 容器也能用）。
 *
 * 主要给"非 Spring 上下文"调用 —— 例如代码生成器、独立运维脚本 —— 直接拿到一个 [DataSource]
 * 就能用同一套规则跑 Flyway，不用引一套 Spring 上下文起来。Spring 容器内的常规迁移通过
 * [io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator] 调用本工具完成。
 *
 * 约定：SQL 脚本按 `classpath:sql/<moduleName>/<dbType>/V*.sql` 组织（[SQL_ROOT_PATH] 是根
 * 目录名），其中 dbType 取自 `RdbTypeEnum::name` 的小写形式（h2 / postgresql / mysql 等）。
 *
 * @author K
 * @since 1.0.0
 */
object FlywayKit {

    /** 日志器；失败一律向上抛，确保启动期发现 schema 错位 */
    private val log = LogFactory.getLog(this::class)

    /** SQL 脚本根目录的 classpath 名称（约定为 "sql"）。 */
    const val SQL_ROOT_PATH = "sql"

    /**
     * 升级一个模块的数据库 schema。
     *
     * 行为：
     * - 探测 [dataSource] 的数据库类型并据此选择 `sql/<moduleName>/<dbType>` 子目录
     * - 每个模块使用独立的 `flyway_history_<moduleName>` 元数据表，互不污染
     * - [flywayProperties] 控制 baseline / encoding / outOfOrder 等 Flyway 通用行为
     *
     * 失败处理：**只要 Flyway 报告 success=false 或抛出任何异常，本方法都会向上抛出，调用方
     * 据此应当中断整体启动流程**。把"失败但继续"作为默认行为是危险的（应用启动后跑在错位的
     * schema 上），所以这里没有 swallow exception。
     *
     * @param moduleName 模块名（对应 SQL_ROOT_PATH 下的直接子目录）
     * @param dataSource 数据源
     * @param flywayProperties 复用 Spring Boot 的 [FlywayProperties] 暴露给用户调
     */
    fun migrate(moduleName: String, dataSource: DataSource, flywayProperties: FlywayProperties) {
        val dbType = dataSource.connection.use { conn ->
            RdbKit.determinRdbTypeByUrl(conn.metaData.url).name.lowercase()
        }
        try {
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
            log.info(">>>>>>>>>>>>>  开始升级模块【$moduleName】的数据库...")
            val result = flyway.migrate()
            if (!result.success) {
                // 不再吞掉 —— Flyway 报失败时必须打断启动，否则会跑在错位 schema 上
                error("flyway 升级模块【$moduleName】的数据库失败！warnings=${result.warnings}")
            }
            val migrationCount = result.migrationsExecuted
            if (migrationCount == 0) {
                log.info("<<<<<<<<<<<<<  模块【$moduleName】数据库已为最新，此次无更新任何 sql 文件。")
            } else {
                log.info("<<<<<<<<<<<<<  模块【$moduleName】数据库升级完成，共执行了 ${migrationCount} 个 sql 文件，最新版本为：${result.targetSchemaVersion}")
            }
        } catch (e: Exception) {
            log.error(e, "flyway 升级模块【$moduleName】数据库出错！")
            throw e
        }
    }
}
