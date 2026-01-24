package io.kudos.test.rdb

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeEach
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

/**
 * 所有涉及SQL操作的测试用例的父类
 *
 * ## 背景与使用场景
 * - 位于测试层，为所有需要SQL测试提供统一的测试数据管理和执行控制
 * - 由具体的DAO测试类直接继承使用
 * - 提供基础的测试数据加载、串行执行、事务回滚和数据库容器配置功能
 *
 * ## 责任边界
 * - 加载并执行子类指定的测试数据SQL文件
 * - 确保测试串行执行，防止数据相互影响
 * - 通过事务回滚机制清理测试数据
 *
 * ## 核心流程
 * 1. 子类通过getTestDataSqlPath()方法指定测试数据SQL文件路径
 * 2. 在@BeforeEach中加载并执行指定的SQL文件
 * 3. 测试执行完成后，通过@Transactional自动回滚所有数据变更
 * 4. 通过@Execution(ExecutionMode.SAME_THREAD)确保测试串行执行
 *
 * ## 依赖与外部交互
 * - 依赖：DataSource（通过DsContextProcessor获取）、ResourceDatabasePopulator（Spring提供）
 * - 依赖：Spring Test Context（通过@EnableKudosTest启用）
 * - IO：读取classpath下的SQL文件并执行
 *
 * ## 资料/契约
 * - 输入：子类必须实现getTestDataSqlPath()方法，返回SQL文件路径（相对于classpath）
 * - 输出：无返回值，通过执行SQL文件准备测试数据，配置Spring测试环境属性
 * - 错误：如果SQL文件不存在或执行失败，会抛出异常
 *
 * ## 交易与一致性
 * - 事务：使用@Transactional，测试完成后自动回滚
 * - 一致性：每个测试方法在独立事务中执行，互不影响
 *
 * ## 并发与线程安全
 * - 通过@Execution(ExecutionMode.SAME_THREAD)确保测试串行执行
 * - 防止多个测试方法并行执行导致的数据冲突
 *
 * ## 性能特性
 * - 每个测试方法执行前都会执行SQL文件，有一定性能开销
 * - 但保证了测试数据的独立性和可重复性
 *
 * ## 安全与合规
 * - 仅执行测试数据SQL文件，不涉及敏感操作
 * - 仅用于测试环境，不涉及生产数据
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Transactional
//@Execution(ExecutionMode.SAME_THREAD)
open class SqlTestBase {

    /**
     * 获取测试数据SQL文件路径
     *
     * @return SQL文件路径（相对于classpath）
     */
    protected open fun getTestDataSqlPath(): String {
       return getTestDataSqlPathPrefix() + getTestDataSqlFilename()
    }

    /**
     * 获取测试数据SQL文件路径的前缀
     *
     * @return SQL文件路径前缀
     */
    protected open fun getTestDataSqlPathPrefix(): String {
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        val testClassName = getTestClassName()
        val classCate =
            if(testClassName.endsWith("DaoTest")) {
                "dao/"
            } else if(testClassName.endsWith("ServiceTest")) {
                "service/"
            } else if(testClassName.endsWith("CacheHandlerTest")) {
                "cache/"
            } else {
                ""
            }
        return "sql/${rdbType.name.lowercase()}/$classCate"
    }

    /**
     * 获取测试数据SQL文件名
     *
     * 获取实现该类的最终类的名称（即运行测试用例的类），
     * 并返回对应的SQL文件名（类名 + .sql后缀）。
     *
     * 例如：
     * - 如果测试类是 `SysParamDaoTest`，则返回 `SysParamDaoTest.sql`
     * - 如果测试类是 `MyServiceTest`，则返回 `MyServiceTest.sql`
     *
     * ## 核心流程
     * 1. 获取当前实例的实际类名（运行时类，不是声明类）
     * 2. 在继承链中，这会返回最底层的子类名称
     * 3. 添加 `.sql` 后缀返回
     *
     * @return SQL文件名（类名 + .sql后缀）
     */
    protected open fun getTestDataSqlFilename(): String {
        return "${getTestClassName()}.sql"
    }

    /**
     * 获取测试类的类名
     *
     * 通过反射获取实现该类的最终类的名称（即运行测试用例的类），
     *
     * ## 核心流程
     * 1. 使用 `this::class.java.simpleName` 获取当前实例的实际类名（运行时类，不是声明类）
     * 2. 在继承链中，这会返回最底层的子类名称
     *
     * @return 测试类的类名
     */
    protected open fun getTestClassName(): String {
        return this::class.java.simpleName
    }

    @Resource
    protected lateinit var dataSource: DataSource

    /**
     * 在每个测试方法执行前，加载并执行测试数据SQL文件
     */
    @BeforeEach
    open fun setUpTestData() {
        val sqlPath = getTestDataSqlPath()
        val resource = ClassPathResource(sqlPath)

        if (!resource.exists()) {
            throw IllegalStateException("测试数据SQL文件不存在: $sqlPath")
        }

        val populator = ResourceDatabasePopulator()
        populator.addScript(resource)
        populator.setSeparator(";")
        populator.setCommentPrefix("--")
        populator.setIgnoreFailedDrops(true)
        populator.setContinueOnError(false)

        populator.execute(dataSource)
    }

}