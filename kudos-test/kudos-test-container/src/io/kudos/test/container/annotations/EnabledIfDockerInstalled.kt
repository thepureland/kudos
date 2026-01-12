package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ExtendWith
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * 如果本机安装了 Docker，则启用测试用例
 * 
 * 与 `@EnabledIfDockerAvailable` 不同，此注解只检查 Docker 是否安装（通过 `docker --version` 命令），
 * 而不检查 Docker 是否正在运行。如果 Docker 已安装但未启动，测试仍会运行（DockerKit 会自动启动 Docker）。
 * 
 * 如果没有安装 Docker，测试用例会被跳过。
 * 
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
@Target(CLASS, FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DockerInstalledCondition::class)
annotation class EnabledIfDockerInstalled
