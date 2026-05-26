package io.kudos.test.container.kit

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer

/**
 * Extensions for the GenericContainer class.
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Map ports.
 *
 * @param ports Pair(host port, container port)
 * @return the GenericContainer instance
 */
fun GenericContainer<*>.bindingPort(vararg ports: Pair<Int, Int>): GenericContainer<*> {
    this.withCreateContainerCmdModifier { cmd ->
        requireNotNull(cmd.hostConfig) { "hostConfig is null" }.withPortBindings(
            ports.map { PortBinding(Ports.Binding.bindPort(it.first), ExposedPort(it.second)) }
        )
    }
    return this
}