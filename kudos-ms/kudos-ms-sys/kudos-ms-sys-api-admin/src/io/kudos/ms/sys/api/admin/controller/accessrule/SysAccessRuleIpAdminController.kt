package io.kudos.ms.sys.api.admin.controller.accessrule

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.sys.common.accessrule.vo.request.VSysAccessRuleWithIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormUpdate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpDetail
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpEdit
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.service.impl.VSysAccessRuleIpService
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleIpService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Admin "IP access rule" APIs: maintain IP range whitelist/blacklist entries for a tenant and a system, and support combined "access rule + IP range" list and detail views.
 *
 * - APIs inherited from [BaseCrudController]: standard create, edit, delete and paging management for individual IP rules.
 * - Additional read-only APIs in this class: simultaneously display **access rule attributes** (tenant, system, rule type, etc.) and **IP range** (start/end, type, enabled status, etc.) in lists/details, allowing the front end to assemble a screen with fewer requests.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/accessRuleIp")
class SysAccessRuleIpAdminController :
    BaseCrudController<
        String,
        ISysAccessRuleIpService,
        SysAccessRuleIpQuery,
        SysAccessRuleIpRow,
        SysAccessRuleIpDetail,
        SysAccessRuleIpEdit,
        SysAccessRuleIpFormCreate,
        SysAccessRuleIpFormUpdate>() {

    /** Read-only: used for combined "rule + IP range" list/detail query and paging. */
    @Resource
    private lateinit var vSysAccessRuleIpService: VSysAccessRuleIpService

    /** Same as the IP rule service injected by the base class; used by extended APIs in this class such as fetching the IP list by rule. */
    @Resource
    private lateinit var sysAccessRuleIpService: ISysAccessRuleIpService

    /**
     * Query all IP ranges configured under a given "access rule" (only IP-side fields; suitable for displaying a sub-table or sidebar list after a rule is selected).
     *
     * @param ruleId access rule primary key (same as the rule id on the rule maintenance page and rule detail)
     * @return one row per IP rule under this rule; empty list if none
     */
    @GetMapping("/getIpsByRuleId")
    fun getIpsByRuleId(@RequestParam ruleId: String): List<SysAccessRuleIpRow> =
        sysAccessRuleIpService.getIpsByRuleId(ruleId)

    /**
     * Fetch **one** combined "access rule + IP range" record by the row id from a list/search result, for a detail drawer or read-only view.
     * If a rule has no IP ranges configured, the list may still contain a placeholder row with only rule information and empty IP information; in that case the row id equals the rule id.
     *
     * @param id the `id` field returned by the list or search API
     * @return one row if found; `null` otherwise
     */
    @GetMapping("/getAccessRuleWithIp")
    fun getAccessRuleWithIp(@RequestParam id: String): VSysAccessRuleWithIpRow? =
        vSysAccessRuleIpService.get(id, VSysAccessRuleWithIpRow::class)

    /**
     * Paged query of combined "access rule + IP range" data, for filtering, sorting and paging in the admin grid (see the request body for filter and paging fields).
     *
     * @param query filter conditions (tenant, system, rule type, enabled status, etc.) and paging parameters
     * @return current page data, total count and other paging information
     */
    @PostMapping("/pagingSearchAccessRuleWithIp")
    @Suppress("UNCHECKED_CAST")
    fun pagingSearchAccessRuleWithIp(
        @RequestBody query: VSysAccessRuleWithIpQuery,
    ): PagingSearchResult<VSysAccessRuleWithIpRow> =
        vSysAccessRuleIpService.pagingSearch(query) as PagingSearchResult<VSysAccessRuleWithIpRow>

    /**
     * List all "rule + IP" rows for a given access rule, used to render all IP ranges under the rule in one go after entering rule details (including a single header-only row when no IPs have been added).
     *
     * @param parentId access rule primary key
     * @return multiple rows: one row per IP range; when no IPs exist, typically a single rule-only row
     */
    @GetMapping("/searchByParentId")
    fun searchByParentId(@RequestParam parentId: String): List<VSysAccessRuleWithIpRow> =
        vSysAccessRuleIpService.searchByParentId(parentId)

    /**
     * List currently visible "rule + IP" data by the two dimensions **business system** and **tenant**, commonly used for the overview after switching tenant/system or for integration troubleshooting.
     * Omitting `tenantId` or passing an empty string means querying only rule data that is **not bound to a specific tenant** (platform level).
     *
     * @param systemCode business system code (same as in the tenant/sub-system selector)
     * @param tenantId tenant id; omitted or empty means the platform tenant scenario
     * @return matching rows; empty list if none
     */
    @GetMapping("/searchBySystemCodeAndTenantId")
    fun searchBySystemCodeAndTenantId(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?,
    ): List<VSysAccessRuleWithIpRow> = vSysAccessRuleIpService.searchBySystemCodeAndTenantId(systemCode, tenantId)

    /**
     * Update only the enabled status of a single IP access rule (list toggle).
     *
     * @param id `sys_access_rule_ip.id`
     * @param active whether enabled
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean = service.updateActive(id, active)
}
