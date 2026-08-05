package io.kudos.ability.security.enforcement.point

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.logger.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping


/**
 * Registers this application's permission points at startup, so the registry is populated before
 * anybody tries to enforce against it.
 *
 * **Why this runs even when enforcement is switched off.** An unregistered path is refused by
 * design, so a deployment must register its points *before* turning the filter on. If registration
 * were gated on the same flag as enforcement, the only way to reach a usable state would be to
 * enable enforcement first and be denied everything — the chicken-and-egg that made the registry
 * hand-seeded in practice. Registration grants nothing; it only makes actions nameable.
 *
 * **Two sources, one merged result.**
 *  1. every HTTP endpoint Spring knows about, named by [IPermissionPointNaming] — or by the code on
 *     its `@RequiresPermission`, which always wins;
 *  2. whatever [IPermissionPointContributor] beans declare, for the guarded actions that have no
 *     request to match on.
 *
 * On a collision the **declared** definition wins over the derived one, on the principle that an
 * explicit statement outranks a convention.
 *
 * Runs on [ApplicationReadyEvent] rather than at bean-init: the handler mappings must be fully
 * built, and a registry write should never be able to hold up context startup.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class PermissionPointRegistrar(
    private val handlerMappings: List<RequestMappingHandlerMapping>,
    private val contributors: List<IPermissionPointContributor>,
    private val naming: IPermissionPointNaming,
    /**
     * Resolved at run time rather than injected, so an absent store is a logged skip instead of a
     * bean that quietly failed to be created. See the auto-configuration for why that distinction
     * decides whether every endpoint gets refused.
     */
    private val storeProvider: () -> IPermissionPointStore?,
    private val properties: PermissionPointRegistrationProperties,
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LogFactory.getLog(this::class)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        try {
            val store = storeProvider()
            if (store == null) {
                log.info(
                    "No IPermissionPointStore is available; permission points will not be registered. " +
                        "URL enforcement refuses unregistered endpoints, so do not enable it in this state.",
                )
                return
            }
            val points = collect()
            if (points.isEmpty()) {
                log.info("No permission points to register.")
                return
            }
            if (properties.dryRun) {
                log.info(
                    "Permission point registration is in dry-run: ${points.size} point(s) would be " +
                        "registered. Codes: ${points.take(DRY_RUN_SAMPLE).joinToString { it.code }}" +
                        if (points.size > DRY_RUN_SAMPLE) ", …" else "",
                )
                return
            }
            val affected = store.register(points)
            log.info("Registered ${points.size} permission point(s); ${affected} row(s) created or updated.")
        } catch (e: Exception) {
            // A registry write must never take the application down with it. Failing here leaves
            // the previous snapshot in place, which is a degraded but safe state: enforcement reads
            // the registry and refuses what it cannot find, so the failure mode is "too strict".
            log.error("Permission point registration failed; the existing registry is left untouched.", e)
        }
    }

    /** The merged, deduplicated point set. Visible for testing. */
    open fun collect(): List<PermissionPointDefinition> {
        val declared = contributors.flatMap { it.points() }
        val fromEndpoints = scanEndpoints()

        // Declared beats derived; among declared, first contributor wins and the clash is reported
        // rather than silently resolved — two modules claiming one code is a real conflict.
        val merged = LinkedHashMap<String, PermissionPointDefinition>()
        declared.forEach { point ->
            val existing = merged.put(key(point), point)
            if (existing != null && existing != point) {
                log.warn(
                    "Permission point ${point.code} in subsystem ${point.subsystem} is declared " +
                        "more than once with different definitions; keeping the first.",
                )
                merged[key(point)] = existing
            }
        }
        fromEndpoints.forEach { point -> merged.putIfAbsent(key(point), point) }
        return merged.values.toList()
    }

    private fun key(point: PermissionPointDefinition): String = "${point.subsystem}|${point.code}"

    /** One scanned endpoint before merging: the raw (code, method, path) fact. */
    private data class ScannedPoint(
        val code: String,
        val method: String?,
        val path: String,
        val source: String,
        val derived: Boolean,
    )

    private fun scanEndpoints(): List<PermissionPointDefinition> {
        val scanned = mutableListOf<ScannedPoint>()
        handlerMappings.forEach { mapping ->
            mapping.handlerMethods.forEach { (info, handler) ->
                scanned += scan(info, handler)
            }
        }

        // Merge per code *before* the store sees anything, because the store keys rows by
        // (code, subsystem): two verbs on one path derive the same code, and letting them race for
        // the single row means whichever registers last owns the url — the other verb then reads as
        // "unregistered" and gets refused. Found by the very first real traffic in shadow mode.
        return scanned.groupBy { it.code }.map { (code, group) ->
            val paths = group.map { it.path }.distinct()
            val first = group.first()
            val url: String
            if (paths.size == 1) {
                // One path, however many verbs. A single verb keeps its qualifier; several verbs
                // register unqualified — one code is one permission, whichever verb exercises it.
                val methods = group.mapNotNull { it.method }.distinct()
                url = if (group.size == 1 && methods.size == 1) "${methods.single()}:${paths.single()}"
                else paths.single()
            } else {
                // The same code from different paths (a pinned annotation on several endpoints).
                // One url column cannot express both; the first keeps URL enforcement and the rest
                // stay guarded by their @RequiresPermission aspect. Said out loud, not silently.
                url = group.first().let { if (it.method == null) it.path else "${it.method}:${it.path}" }
                log.warn(
                    "Permission code ${code} is derived from ${paths.size} different paths " +
                        "(${paths.joinToString()}); URL enforcement covers only ${url}.",
                )
            }
            PermissionPointDefinition(
                code = code,
                name = code,
                url = url,
                subsystem = properties.subsystem,
                remark = "auto-registered from ${first.source}",
                derived = group.all { it.derived },
            )
        }
    }

    private fun scan(info: RequestMappingInfo, handler: HandlerMethod): List<ScannedPoint> {
        // patternValues covers both the PathPattern and the legacy AntPathMatcher parser, so this
        // does not have to know which one the application configured.
        val paths = info.patternValues.filter { it.isNotBlank() }
        if (paths.isEmpty()) return emptyList()

        // No method condition means the endpoint answers every verb; keep it unqualified rather
        // than fanning out to seven entries that all mean the same thing.
        val methods = info.methodsCondition.methods.map { it.name }.ifEmpty { listOf(null) }
        val pinned = AnnotatedElementUtils.findMergedAnnotation(handler.method, RequiresPermission::class.java)
            ?: AnnotatedElementUtils.findMergedAnnotation(handler.beanType, RequiresPermission::class.java)

        return paths.flatMap { path ->
            if (properties.excludedPaths.any { path.startsWith(it) }) return@flatMap emptyList()
            methods.mapNotNull { method ->
                val derivedCode = naming.codeFor(method ?: ANY_METHOD, path)
                val code = pinned?.value?.trim()?.takeIf { it.isNotEmpty() } ?: derivedCode ?: return@mapNotNull null
                ScannedPoint(
                    code = code,
                    method = method,
                    path = path,
                    source = "${handler.beanType.simpleName}.${handler.method.name}",
                    derived = pinned == null,
                )
            }
        }
    }

    companion object {
        private const val ANY_METHOD = "GET"
        private const val DRY_RUN_SAMPLE = 20
    }
}
