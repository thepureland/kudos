package io.kudos.ability.security.enforcement.point

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.support.StaticWebApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for [PermissionPointRegistrar].
 *
 * Drives a real [RequestMappingHandlerMapping] over throwaway controllers rather than stubbing the
 * scan: the whole value of the registrar is that it agrees with what Spring actually routes, and a
 * fake mapping would agree with itself instead.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class PermissionPointRegistrarTest {

    @RestController
    @RequestMapping("/api/admin/demo/widget")
    class DemoWidgetController {

        @GetMapping("/search")
        fun search(): String = "ok"

        @PostMapping("/save")
        fun save(): String = "ok"

        /** Pinned: the code must survive the path being renamed, so it is stated rather than derived. */
        @RequiresPermission("demo:widget:purge")
        @DeleteMapping("/deleteEverythingNamedLikeThis")
        fun purge(): String = "ok"
    }

    @RestController
    @RequestMapping("/actuator/custom")
    class ExcludedController {
        @GetMapping("/probe")
        fun probe(): String = "ok"
    }

    /** The shape that found the bug: GET and DELETE on one path, deriving one code. */
    @RestController
    @RequestMapping("/api/admin/demo/findings")
    class TwoVerbController {
        @GetMapping("/report")
        fun read(): String = "ok"

        @DeleteMapping("/report")
        fun clear(): String = "ok"
    }

    private class RecordingStore : IPermissionPointStore {
        var received: Collection<PermissionPointDefinition> = emptyList()
        override fun register(points: Collection<PermissionPointDefinition>): Int {
            received = points
            return points.size
        }
    }

    private fun registrar(
        contributors: List<IPermissionPointContributor> = emptyList(),
        properties: PermissionPointRegistrationProperties = PermissionPointRegistrationProperties(),
    ): Pair<PermissionPointRegistrar, RecordingStore> {
        val context = StaticWebApplicationContext()
        context.registerSingleton("demoWidgetController", DemoWidgetController::class.java)
        context.registerSingleton("excludedController", ExcludedController::class.java)
        context.registerSingleton("twoVerbController", TwoVerbController::class.java)
        context.refresh()
        val mapping = RequestMappingHandlerMapping()
        mapping.applicationContext = context
        mapping.afterPropertiesSet()

        val store = RecordingStore()
        return PermissionPointRegistrar(
            handlerMappings = listOf(mapping),
            contributors = contributors,
            naming = ConventionalPermissionPointNaming(),
            storeProvider = { store },
            properties = properties,
        ) to store
    }

    private fun collect(
        contributors: List<IPermissionPointContributor> = emptyList(),
        properties: PermissionPointRegistrationProperties = PermissionPointRegistrationProperties(),
    ) = registrar(contributors, properties).first.collect()

    @Test
    fun derivesAPointPerEndpointFromTheRealHandlerMapping() {
        val points = collect().associateBy { it.code }

        val search = assertNotNull(points["demo:widget:search"])
        assertEquals("GET:/api/admin/demo/widget/search", search.url)
        assertTrue(search.derived, "a code read off the path is marked derived")

        assertEquals("POST:/api/admin/demo/widget/save", points["demo:widget:save"]?.url)
    }

    /**
     * `@RequiresPermission` pins the code. That is what makes an endpoint safe to rename once its
     * permission has been granted in anger — the url follows the move, the code does not.
     */
    @Test
    fun anAnnotatedCodeWinsOverTheDerivedOne() {
        val points = collect().associateBy { it.code }

        val pinned = assertNotNull(points["demo:widget:purge"])
        assertEquals("DELETE:/api/admin/demo/widget/deleteEverythingNamedLikeThis", pinned.url)
        assertTrue(!pinned.derived, "an explicitly stated code is not derived")
        assertNull(
            points["demo:widget:deleteEverythingNamedLikeThis"],
            "the derived name must not also be registered, or one action would have two codes",
        )
    }

    /**
     * Two verbs on one path derive one code, and the store keys rows by (code, subsystem) — so
     * letting them register separately means whichever lands last owns the url, and the other verb
     * reads as "unregistered" and gets refused. Found by the first real traffic in shadow mode:
     * GET /rowScopeFindings was being flagged unregistered because DELETE had claimed the row.
     */
    @Test
    fun severalVerbsOnOnePathMergeIntoOneUnqualifiedRegistration() {
        val points = collect().associateBy { it.code }

        val merged = assertNotNull(points["demo:findings:report"])
        assertEquals(
            "/api/admin/demo/findings/report",
            merged.url,
            "one code is one permission, whichever verb exercises it — so the url drops the qualifier",
        )
    }

    @Test
    fun infrastructurePathsAreNotPermissionPoints() {
        assertTrue(collect().none { it.url?.contains("/actuator/") == true })
    }

    /** An explicit declaration outranks a convention, on the same principle as the annotation. */
    @Test
    fun aContributedPointWinsOverTheDerivedOneWithTheSameCode() {
        val contributed = PermissionPointDefinition(
            code = "demo:widget:search",
            name = "Search widgets (curated)",
            url = null,
            remark = "declared by the owning module",
        )
        val points = collect(listOf(object : IPermissionPointContributor {
            override fun points() = listOf(contributed)
        })).associateBy { it.code }

        assertEquals(contributed, points["demo:widget:search"])
    }

    /**
     * Points with no HTTP surface at all — a scheduled job, a message consumer — are reachable only
     * through the contributor SPI, and must survive the merge with nothing to collide against.
     */
    @Test
    fun contributedPointsWithNoUrlAreKept() {
        val job = PermissionPointDefinition(code = "demo:report:nightlyRebuild", name = "Nightly rebuild")
        val points = collect(listOf(object : IPermissionPointContributor {
            override fun points() = listOf(job)
        })).associateBy { it.code }

        assertEquals(job, points["demo:report:nightlyRebuild"])
    }

    /** Dry run must be genuinely dry — the whole point is inspecting before creating rows. */
    @Test
    fun dryRunWritesNothing() {
        val (registrar, store) = registrar(properties = PermissionPointRegistrationProperties().apply { dryRun = true })
        registrar.onApplicationEvent(readyEvent())
        assertTrue(store.received.isEmpty())
    }

    @Test
    fun aFailingStoreDoesNotEscapeToTheCaller() {
        val context = StaticWebApplicationContext()
        context.registerSingleton("demoWidgetController", DemoWidgetController::class.java)
        context.refresh()
        val mapping = RequestMappingHandlerMapping()
        mapping.applicationContext = context
        mapping.afterPropertiesSet()

        val exploding = object : IPermissionPointStore {
            override fun register(points: Collection<PermissionPointDefinition>): Int =
                throw IllegalStateException("registry unavailable")
        }
        val registrar = PermissionPointRegistrar(
            handlerMappings = listOf(mapping),
            contributors = emptyList(),
            naming = ConventionalPermissionPointNaming(),
            storeProvider = { exploding },
            properties = PermissionPointRegistrationProperties(),
        )
        // A registry write must never take the application down: the resulting state is "too
        // strict" (unregistered endpoints are refused), never "too permissive".
        registrar.onApplicationEvent(readyEvent())
    }

    /**
     * No store on the classpath must be a logged skip, not an exception — and specifically not a
     * silently absent bean, which is what the old `@ConditionalOnBean` gate risked.
     */
    @Test
    fun anAbsentStoreIsSkippedRatherThanFailing() {
        val context = StaticWebApplicationContext()
        context.registerSingleton("demoWidgetController", DemoWidgetController::class.java)
        context.refresh()
        val mapping = RequestMappingHandlerMapping()
        mapping.applicationContext = context
        mapping.afterPropertiesSet()

        PermissionPointRegistrar(
            handlerMappings = listOf(mapping),
            contributors = emptyList(),
            naming = ConventionalPermissionPointNaming(),
            storeProvider = { null },
            properties = PermissionPointRegistrationProperties(),
        ).onApplicationEvent(readyEvent())
    }

    private fun readyEvent() = org.springframework.boot.context.event.ApplicationReadyEvent(
        org.springframework.boot.SpringApplication(),
        arrayOf(),
        StaticWebApplicationContext(),
        java.time.Duration.ZERO,
    )
}
