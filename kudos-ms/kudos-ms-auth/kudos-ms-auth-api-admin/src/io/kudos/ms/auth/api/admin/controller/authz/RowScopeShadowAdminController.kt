package io.kudos.ms.auth.api.admin.controller.authz

import io.kudos.ability.data.rdb.ktorm.rowscope.RowScopeProperties
import io.kudos.ability.data.rdb.ktorm.rowscope.RowScopeRegistry
import io.kudos.ability.data.rdb.ktorm.rowscope.RowScopeShadowRecorder
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable


/**
 * The row-filtering migration work-list: what shadow mode has observed, and what state the feature
 * is in.
 *
 * **Why this lives under the auth admin API** rather than with the data layer that produces it: it
 * is read by the same people, on the same screen, for the same reason as the permission diagnostics
 * next door — and a diagnostics endpoint in `kudos-ability-data-rdb-ktorm` would drag a web
 * dependency into a module that deliberately has none.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/authz")
class RowScopeShadowAdminController {

    @Resource
    private lateinit var recorder: RowScopeShadowRecorder

    @Resource
    private lateinit var registry: RowScopeRegistry

    @Resource
    private lateinit var properties: RowScopeProperties

    /**
     * The findings so far, plus enough state to interpret them — an empty list means something very
     * different when filtering is switched off than when it has been running in shadow for a week.
     */
    @GetMapping("/rowScopeFindings")
    fun findings(): RowScopeStatusVo = RowScopeStatusVo(
        enabled = properties.enabled,
        shadowMode = properties.shadowMode,
        declaredEntities = registry.declaredEntities().mapNotNull { it.simpleName }.sorted(),
        droppedCount = recorder.droppedCount(),
        findings = recorder.findings().map {
            RowScopeStatusVo.FindingVo(
                kind = it.kind.name,
                entity = it.entity,
                table = it.table,
                detail = it.detail,
                count = it.count,
                firstSeen = it.firstSeen.toString(),
                lastSeen = it.lastSeen.toString(),
            )
        },
    )

    /** Clears the list, for after the findings have been acted on. */
    @DeleteMapping("/rowScopeFindings")
    fun clear(): Boolean {
        recorder.clear()
        return true
    }
}


/**
 * Shadow-mode status and findings.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class RowScopeStatusVo(

    /** Whether row filtering is switched on at all. */
    val enabled: Boolean,

    /** Whether it is observing (true) or actually filtering (false). */
    val shadowMode: Boolean,

    /** Entities that have declared themselves into filtering. */
    val declaredEntities: List<String>,

    /**
     * Distinct findings dropped because the buffer filled. Non-zero means **the list below is
     * incomplete** — surfaced rather than hidden, because a truncated work-list that looks complete
     * is worse than no list.
     */
    val droppedCount: Long,

    val findings: List<FindingVo>,

) : Serializable {

    data class FindingVo(
        /** WOULD_FILTER (verify the predicate) or WOULD_FAIL (this call needs system authority). */
        val kind: String,
        val entity: String,
        val table: String,
        val detail: String,
        val count: Long,
        val firstSeen: String,
        val lastSeen: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
