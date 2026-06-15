package io.kudos.test.container.kit

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.testcontainers.containers.GenericContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers the [bindingPort] extension on [GenericContainer]:
 * - returns the same container (fluent API)
 * - registers a create-container-cmd modifier whose body maps each host/container pair to a
 *   [PortBinding] and applies it to the cmd's host config
 * - the null-hostConfig guard throws [IllegalArgumentException] with the expected message
 *
 * Building a real container needs no Docker daemon; only `start()` would. The modifier body is driven
 * directly against a mocked [CreateContainerCmd].
 *
 * @author K
 * @since 1.0.0
 */
internal class XGenericContainerTest {

    private fun newContainer(): GenericContainer<*> = GenericContainer("alpine:3.20")

    @Test
    fun `returns the same container instance`() {
        val container = newContainer()
        val returned = container.bindingPort(8080 to 80)
        assertSame(container, returned)
    }

    @Test
    fun `registers exactly one create-container-cmd modifier`() {
        val container = newContainer()
        val before = container.createContainerCmdModifiers.size
        container.bindingPort(8080 to 80)
        assertEquals(before + 1, container.createContainerCmdModifiers.size)
    }

    @Test
    fun `modifier applies port bindings to host config`() {
        val container = newContainer()
        container.bindingPort(5001 to 5432, 6001 to 6379)

        val modifier = container.createContainerCmdModifiers.last()
        val hostConfig = Mockito.mock(HostConfig::class.java)
        Mockito.`when`(hostConfig.withPortBindings(anyList<PortBinding>())).thenReturn(hostConfig)
        val cmd = Mockito.mock(CreateContainerCmd::class.java)
        Mockito.`when`(cmd.hostConfig).thenReturn(hostConfig)

        modifier.modify(cmd)

        @Suppress("UNCHECKED_CAST")
        val captor = org.mockito.ArgumentCaptor.forClass(List::class.java) as org.mockito.ArgumentCaptor<List<PortBinding>>
        Mockito.verify(hostConfig).withPortBindings(captor.capture())
        val bindings = captor.value
        assertEquals(2, bindings.size)
        // each binding's exposed (container) port must match the supplied container ports
        val exposedPorts = bindings.map { it.exposedPort.port }.toSet()
        assertTrue(exposedPorts.containsAll(listOf(5432, 6379)))
    }

    @Test
    fun `modifier throws when host config is null`() {
        val container = newContainer()
        container.bindingPort(1234 to 5678)
        val modifier = container.createContainerCmdModifiers.last()

        val cmd = Mockito.mock(CreateContainerCmd::class.java)
        Mockito.`when`(cmd.hostConfig).thenReturn(null)

        val ex = assertFailsWith<IllegalArgumentException> { modifier.modify(cmd) }
        assertEquals("hostConfig is null", ex.message)
    }

    @Test
    fun `empty ports registers a modifier that binds nothing`() {
        val container = newContainer()
        container.bindingPort()
        val modifier = container.createContainerCmdModifiers.last()

        val hostConfig = Mockito.mock(HostConfig::class.java)
        Mockito.`when`(hostConfig.withPortBindings(anyList<PortBinding>())).thenReturn(hostConfig)
        val cmd = Mockito.mock(CreateContainerCmd::class.java)
        Mockito.`when`(cmd.hostConfig).thenReturn(hostConfig)

        modifier.modify(cmd)

        @Suppress("UNCHECKED_CAST")
        val captor = org.mockito.ArgumentCaptor.forClass(List::class.java) as org.mockito.ArgumentCaptor<List<PortBinding>>
        Mockito.verify(hostConfig).withPortBindings(captor.capture())
        assertEquals(0, captor.value.size)
    }
}
