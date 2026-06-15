package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ContainerPort

/**
 * Helpers building real docker-java [Container] / [ContainerPort] instances so each container
 * object's `registerProperties(...)` can be unit-tested without a Docker daemon.
 *
 * Both classes are Jackson POJOs with final getters (not stubbable on Mockito's default mock-maker),
 * so instances are populated by reflectively setting their private fields.
 *
 * @author K
 * @since 1.0.0
 */
internal object ContainerPortMocks {

    fun port(ip: String?, publicPort: Int?, privatePort: Int? = null): ContainerPort {
        val p = ContainerPort()
        setField(p, "ip", ip)
        setField(p, "publicPort", publicPort)
        setField(p, "privatePort", privatePort)
        return p
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    fun container(id: String = "deadbeefcafebabe0000", vararg ports: ContainerPort): Container {
        val c = Container()
        setField(c, "id", id)
        setField(c, "ports", ports)
        return c
    }
}
