package io.kudos.ms.sys.client.init

import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy
import io.kudos.ms.sys.client.microservice.proxy.ISysSubSystemMicroServiceProxy
import io.kudos.ms.sys.common.dict.api.ISysDictApi
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip test for the sys interface clients against a controller that implements the **same**
 * `ISys*Api` contract from `kudos-ms-sys-common`.
 *
 * This is the test that justifies the migration's riskiest edit — moving the 65 mapping annotations
 * in `kudos-ms-sys-common` from Spring MVC (`@GetMapping` / `@PostMapping`) to the `@HttpExchange`
 * family. It asserts three things that a compile cannot:
 *
 * 1. **`@GetExchange` / `@PostExchange` still work as server-side request mappings.** The mock
 *    controllers below declare no mapping annotations of their own; every route they answer on is
 *    inherited from the shared API interface. Spring MVC 7 maps `@HttpExchange` via
 *    `RequestMappingHandlerMapping.createRequestMappingInfo(HttpExchange, ...)`.
 * 2. **Unnamed `@RequestParam` binds on the client side.** All 87 `@RequestParam` in the sys contract
 *    are unnamed. `HttpServiceProxyFactory` installs no `ParameterNameDiscoverer`, so the name comes
 *    from reflection alone, which needs Kotlin `javaParameters = true` on whichever module compiles
 *    the interface — here `kudos-ms-sys-common`, which sets it locally (originally for Jackson). The
 *    root build sets it repo-wide for the modules that do not.
 * 3. **`@ImportHttpServices` on [SysClientAutoConfiguration] registers the proxies as beans**, picked
 *    up automatically through `IComponentInitializer` — no `@EnableFeignClients` equivalent needed on
 *    the calling application.
 *
 * The base-url is a plain `http://localhost:<port>` rather than `lb://`: LoadBalancer resolution is
 * already covered end-to-end in `kudos-ability-distributed-client-http`, and pinning it here would
 * drag a Nacos container into a test whose subject is the contract, not discovery.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${SysClientContractTest.PORT}",
        "spring.http.serviceclient.${SysClientAutoConfiguration.GROUP}.base-url=http://localhost:${SysClientContractTest.PORT}",
    ]
)
@Import(SysClientContractTest.MockDictController::class, SysClientContractTest.MockSubSystemController::class)
open class SysClientContractTest {

    @Autowired
    private lateinit var dictProxy: ISysDictProxy

    @Autowired
    private lateinit var subSystemProxy: ISysSubSystemMicroServiceProxy

    @Test
    fun `GET with two unnamed RequestParams round-trips through the shared contract`() {
        val items = dictProxy.getActiveDictItems("GENDER", "sys")

        assertEquals(1, items.size)
        assertEquals("M", items.single().itemCode)
        // Proves the query parameters actually arrived, in the right slots.
        assertEquals("GENDER", items.single().dictType)
        assertEquals("sys", items.single().atomicServiceCode)
    }

    @Test
    fun `GET returning a map round-trips`() {
        assertEquals(linkedMapOf("M" to "Male"), dictProxy.getActiveDictItemMap("GENDER", "sys"))
    }

    @Test
    fun `GET with a single unnamed RequestParam and a Set return round-trips`() {
        assertEquals(setOf("sys", "user"), subSystemProxy.getMicroServiceCodesBySubSystemCode("kudos"))
    }

    @Test
    fun `POST mixing RequestParam and RequestBody round-trips`() {
        // The trickiest binding in the contract: one value goes to the query string, the other to the
        // JSON body, and the client has to tell them apart from the annotations alone.
        assertEquals(2, subSystemProxy.batchBind("kudos", listOf("sys", "user")))
    }

    @Test
    fun `GET returning a primitive round-trips`() {
        assertTrue(subSystemProxy.exists("kudos", "sys"))
    }

    /**
     * Mock provider for the dict contract. Deliberately carries **no** mapping annotations — the
     * routes come from `@GetExchange` on [ISysDictApi].
     */
    @RestController
    open class MockDictController : ISysDictApi {

        override fun getActiveDictItems(
            dictType: String,
            atomicServiceCode: String,
        ): List<SysDictItemCacheEntry> = listOf(
            SysDictItemCacheEntry(
                id = "1",
                itemCode = "M",
                itemName = "Male",
                dictId = "d1",
                orderNum = 1,
                parentId = null,
                remark = null,
                active = true,
                builtIn = false,
                // Echo the inputs back so the test can assert they were bound, not defaulted.
                dictType = dictType,
                dictName = "Gender",
                atomicServiceCode = atomicServiceCode,
            )
        )

        override fun getActiveDictItemMap(
            dictType: String,
            atomicServiceCode: String,
        ): LinkedHashMap<String, String> = linkedMapOf("M" to "Male")

        override fun batchGetActiveDictItems(
            dictTypeAndASCodePairs: List<Pair<String, String>>,
        ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> = emptyMap()

        override fun batchGetActiveDictItemMap(
            dictTypeAndASCodePairs: List<Pair<String, String>>,
        ): Map<Pair<String, String>, LinkedHashMap<String, String>> = emptyMap()
    }

    /** Mock provider for the subsystem-microservice contract; same "no mapping annotations" rule. */
    @RestController
    open class MockSubSystemController : ISysSubSystemMicroServiceApi {

        override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> =
            if (subSystemCode == "kudos") setOf("sys", "user") else emptySet()

        override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> = emptySet()

        override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int =
            if (subSystemCode == "kudos") microServiceCodes.size else -1

        override fun unbind(subSystemCode: String, microServiceCode: String): Boolean = true

        override fun exists(subSystemCode: String, microServiceCode: String): Boolean =
            subSystemCode == "kudos" && microServiceCode == "sys"
    }

    companion object {
        /**
         * Fixed port: the group's `base-url` is resolved when the HTTP service proxies are registered,
         * which happens before a `RANDOM_PORT` server would know its port.
         */
        const val PORT: String = "18099"
    }

}
