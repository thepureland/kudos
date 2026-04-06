package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * postgres测试容器
 *
 * 若需将库文件持久到宿主机，请设置系统属性 [SYS_PROP_HOST_DATA_DIR] 或环境变量 [ENV_HOST_DATA_DIR]，
 * 该路径将绑定到容器内 `/var/lib/postgresql/data`（读写）；未设置时数据仅存于容器层，与原先行为一致。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object PostgresTestContainer {

    /** 宿主机数据目录；优先于 [ENV_HOST_DATA_DIR] */
    const val SYS_PROP_HOST_DATA_DIR = "kudos.test.postgres.data.dir"

    /** 宿主机数据目录（系统属性未设置时使用） */
    const val ENV_HOST_DATA_DIR = "KUDOS_TEST_POSTGRES_DATA_DIR"

    /** 官方镜像默认 PGDATA */
    private const val CONTAINER_PGDATA = "/var/lib/postgresql/data"

    private const val LOCALHOST = "127.0.0.1"

    private const val DATABASE_READY_MAX_RETRIES = 20

    private const val DATABASE_READY_RETRY_INTERVAL_MILLIS = 500L

    private const val IMAGE_NAME = "postgres:18.0-alpine3.22"

    const val DATABASE = "test"

    const val PORT = 25432

    const val CONTAINER_PORT = 5432

    const val USERNAME = "pg"

    const val PASSWORD = "postgres"

    const val LABEL = "PostgreSql"

    private var container: GenericContainer<*>? = null

    /**
     * 使用默认库名启动 postgres 容器。
     *
     * @param registry Spring 动态属性注册器，可用于注册或覆盖已注册属性，也可为 {@code null}
     * @return 当前运行中的 postgres 容器信息
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return startIfNeeded(registry, DATABASE)
    }

    /**
     * 启动 postgres 容器，并确保指定 database 已存在。
     * <p>
     * 如果容器尚未启动，则先启动容器；如果容器已存在，则直接在现有容器内补建 database。
     *
     * @param registry Spring 动态属性注册器，可用于注册或覆盖已注册属性，也可为 {@code null}
     * @param database 需要确保存在的数据库名
     * @return 当前运行中的 postgres 容器信息
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?, database: String): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, getOrCreateContainer())
            ensureDatabaseExists(runningContainer, database)
            if (registry != null) {
                registerProperties(registry, runningContainer, database)
            }
            return runningContainer
        }
    }

    /**
     * 延迟创建容器定义，避免在对象初始化阶段就固定容器状态。
     *
     * @return postgres 容器定义
     */
    private fun getOrCreateContainer(): GenericContainer<*> {
        if (container == null) {
            container = GenericContainer(IMAGE_NAME).apply {
                withExposedPorts(CONTAINER_PORT)
                bindingPort(Pair(PORT, CONTAINER_PORT))
                withEnv("POSTGRES_DB", DATABASE)
                withEnv("POSTGRES_USER", USERNAME)
                withEnv("POSTGRES_PASSWORD", PASSWORD)
                withCommand("postgres -c max_prepared_transactions=10")
                withLabel(TestContainerKit.LABEL_KEY, LABEL)
                resolveHostDataDir()?.let { hostPath ->
                    Files.createDirectories(Paths.get(hostPath))
                    withFileSystemBind(hostPath, CONTAINER_PGDATA, BindMode.READ_WRITE)
                }
            }
        }
        return requireNotNull(container)
    }

    /**
     * 解析宿主机上用于持久化 PGDATA 的目录。
     *
     * @return 非空绝对或规范路径时返回该路径；未配置时返回 `null`（不挂载卷）
     */
    private fun resolveHostDataDir(): String? {
        val fromProp = System.getProperty(SYS_PROP_HOST_DATA_DIR)?.trim().orEmpty()
        if (fromProp.isNotEmpty()) {
            return fromProp
        }
        val fromEnv = System.getenv(ENV_HOST_DATA_DIR)?.trim().orEmpty()
        return fromEnv.takeIf { it.isNotEmpty() }
    }

    /**
     * 连接管理库 {@code postgres}，按需创建目标 database。
     */
    private fun ensureDatabaseExists(runningContainer: Container, database: String) {
        validateDatabaseName(database)
        val url = buildAdminJdbcUrl(runningContainer)
        var lastException: SQLException? = null
        repeat(DATABASE_READY_MAX_RETRIES) { attempt ->
            try {
                DriverManager.getConnection(url, USERNAME, PASSWORD).use { connection ->
                    if (databaseExists(connection, database)) {
                        return
                    }
                    connection.createStatement().use { statement ->
                        statement.execute("""create database "${database.replace("\"", "\"\"")}"""")
                    }
                    return
                }
            } catch (e: SQLException) {
                lastException = e
                if (attempt == DATABASE_READY_MAX_RETRIES - 1) {
                    return@repeat
                }
                sleepBeforeRetry()
            }
        }
        throw RuntimeException("failed to ensure postgres database exists: $database", lastException)
    }

    /**
     * 构造连接管理库使用的 JDBC URL。
     *
     * @param runningContainer 当前运行中的容器
     * @return 指向 postgres 管理库的 JDBC URL
     */
    private fun buildAdminJdbcUrl(runningContainer: Container): String {
        val port = runningContainer.ports.firstOrNull()?.publicPort ?: PORT
        return "jdbc:postgresql://$LOCALHOST:$port/postgres?sslmode=disable"
    }

    /**
     * 检查目标 database 是否已经存在。
     */
    private fun databaseExists(connection: Connection, database: String): Boolean {
        val sql = "select 1 from pg_database where datname = ?"
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setString(1, database)
            statement.executeQuery().use { resultSet: ResultSet ->
                return resultSet.next()
            }
        }
    }

    /**
     * 限制 database 名称字符集，避免空值和非法标识符带来建库风险。
     */
    private fun validateDatabaseName(database: String) {
        require(database.isNotBlank()) { "database must not be blank" }
        require(database.matches(Regex("[A-Za-z0-9_]+"))) {
            "database contains unsupported characters: $database"
        }
    }

    /**
     * 在数据库尚未就绪时短暂等待后重试。
     */
    private fun sleepBeforeRetry() {
        try {
            Thread.sleep(DATABASE_READY_RETRY_INTERVAL_MILLIS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("interrupted while waiting for postgres to become ready", e)
        }
    }

    /**
     * 注册指定 database 对应的数据源属性。
     */
    private fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container,
        database: String
    ) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:postgresql://$host:$port/$database"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { PASSWORD }
    }

    /**
     * 返回运行中的容器对象。
     *
     * @return 容器对象，如果没有则返回 {@code null}
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}
