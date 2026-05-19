package io.kudos.test.rdb

import io.kudos.test.container.containers.H2TestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 所有需要关系型数据库环境的测试用例的父类
 *
 * ## 背景与使用场景
 * - 位于测试层，继承自[SqlTestBase]，为需要完整数据库的测试提供容器配置
 * - 由需要关系型数据库的测试类继承使用
 * - 继承[SqlTestBase]的脚本文件配置功能
 *
 * ## 责任边界
 * - 启动并配置H2TestContainer
 * - 继承[SqlTestBase]的所有功能（测试数据加载、串行执行、事务回滚）
 *
 * ## 核心流程
 * 1. 在@DynamicPropertySource中启动H2TestContainer
 * 2. 继承[SqlTestBase]的测试数据加载和事务回滚功能
 *
 * ## 依赖与外部交互
 * - 依赖：[SqlTestBase]（提供测试数据加载、事务管理）
 * - 依赖：H2TestContainer
 * - IO：启动Docker容器（如果未运行）
 *
 * ## 资料/契约
 * - 输入：无
 * - 输出：配置Spring测试环境属性（H2）
 * - 错误：如果Docker未安装或容器启动失败，会抛出异常
 *
 * ## 交易与一致性
 * - 事务：继承[SqlTestBase]的@Transactional；fixture 数据由 @BeforeTransaction 在事务外提交（见 [SqlTestBase] 说明）
 *
 * ## 并发与线程安全
 * - 容器启动使用同步机制，确保只启动一次
 * - 继承[SqlTestBase]的串行执行保证
 *
 * ## 性能特性
 * - 容器启动有一定开销，但会复用已运行的容器
 *
 * ## 安全与合规
 * - 仅用于测试环境，不涉及生产数据
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
open class RdbTestBase : SqlTestBase() {

    companion object Companion {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "false" }
            // 默认 H2；要换 Postgres/MySQL 时业务子类自己写 companion + @DynamicPropertySource 覆盖
            H2TestContainer.startIfNeeded(registry)
        }
    }

}