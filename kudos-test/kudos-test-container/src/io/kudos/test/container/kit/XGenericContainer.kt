package io.kudos.test.container.kit

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer

/**
 * 扩展GenericContainer类
 *
 * @author K
 * @since 1.0.0
 */


/**
 * 映射端口
 *
 * @param ports Pair(宿主机端口，容器端口)
 * @return GenericContainer对象
 */
fun GenericContainer<*>.bindingPort(vararg ports: Pair<Int, Int>): GenericContainer<*> {
    this.withCreateContainerCmdModifier { cmd ->
        requireNotNull(cmd.hostConfig) { "hostConfig is null" }.withPortBindings(
            ports.map { PortBinding(Ports.Binding.bindPort(it.first), ExposedPort(it.second)) }
        )
    }
    return this
}