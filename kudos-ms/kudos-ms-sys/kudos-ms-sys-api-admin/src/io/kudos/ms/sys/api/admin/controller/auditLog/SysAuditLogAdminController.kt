package io.kudos.ms.sys.api.admin.controller.auditLog

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only admin controller for `sys_audit_log` (+ `sys_audit_detail_log`). Powers the
 * `/sys/auditlog` page in the console UI.
 *
 * Audit data is append-only — write is owned by the audit aspect in `kudos-ability-log-audit`; this
 * controller is strictly the read window for forensics and admin investigations. Hence no
 * create/update/delete endpoints, and no extension of [io.kudos.ability.web.springmvc.controller.BaseCrudController].
 *
 * **Why not [io.kudos.ability.web.springmvc.controller.BaseReadOnlyController]?** That base assumes
 * the service implements [io.kudos.base.support.service.iservice.IBaseReadOnlyService] and the
 * query VO extends `ListSearchPayload`. Audit's [IAuditLogReadOnlyService] has its own
 * narrower contract (separate `pageNo`/`pageSize` args, [io.kudos.ability.log.audit.common.entity.AuditLogQuery]
 * filter) so we wrap it directly.
 *
 * **Bean wiring**: the dependency is `kudos-ability-log-audit-rdb-ktorm`; its autoconfig
 * (`LogAuditRdbKtormAutoConfiguration`) registers an `IAuditLogReadOnlyService` bean named
 * `rdbKtormAuditLogReadOnlyService`. We reference it by name so that a future deployment which
 * registers a second backend (ClickHouse, Mongo) for the same interface doesn't ambiguate the
 * injection point.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/auditLog")
class SysAuditLogAdminController {

    @Resource(name = "rdbKtormAuditLogReadOnlyService")
    private lateinit var auditLogReadOnly: IAuditLogReadOnlyService

    /**
     * Paged + filtered search. Returns the standard `{data, totalCount}` shape the console UI's
     * `BaseListPage.postSearchSuccessfully` understands, so no JS-side adapter is needed.
     *
     * Storage-side ordering is fixed at `operate_time DESC` — the request's `orders` field is
     * accepted but ignored (see [AuditLogPagingRequest.orders] for the rationale).
     */
    @PostMapping("/pagingSearch")
    fun pagingSearch(@RequestBody request: AuditLogPagingRequest): PagingSearchResult<*> {
        val page = auditLogReadOnly.pagingSearch(
            query = request.toQuery(),
            pageNo = request.pageNo ?: 1,
            // The storage layer only clamps the lower bound (coerceAtLeast(1)); cap the upper bound here
            // so an inflated pageSize cannot dump the whole audit table in one request. 100 matches
            // ListSearchPayload.getMaxPageSize() used by the other admin list endpoints.
            pageSize = (request.pageSize ?: 10).coerceAtMost(MAX_PAGE_SIZE),
        )
        // Long → Int narrowing on totalCount: the frontend uses Number anyway. Audit volume past
        // Int.MAX_VALUE in a single tenant is implausible; if it ever happens the count would
        // overflow silently — a future refactor of PagingSearchResult to a Long totalCount would
        // fix the root cause, but bigger scope than this controller.
        return PagingSearchResult(data = page.items, totalCount = page.total.toInt())
    }

    /**
     * Look up one audit log by id; the response merges the main row with the (optional) detail row
     * into a single flat DTO. Throws [ObjectNotFoundException] only when the main row is missing —
     * a missing detail row is normal for older entries written before detail capture was added.
     */
    @GetMapping("/getDetail")
    fun getDetail(@RequestParam id: String): AuditLogDetailDto {
        val main = auditLogReadOnly.findById(id)
            ?: throw ObjectNotFoundException("Audit log not found: $id")
        val detail = auditLogReadOnly.findDetailById(id)
        return AuditLogDetailDto.from(main, detail)
    }

    companion object {
        /** Upper bound for [AuditLogPagingRequest.pageSize]; aligned with `ListSearchPayload.getMaxPageSize()`. */
        private const val MAX_PAGE_SIZE = 100
    }
}
