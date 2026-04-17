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
 * 后台「IP 访问规则」相关接口：维护某租户、某系统下的 IP 段白名单/黑名单条目，并支持按「访问规则 + IP 段」合一的列表与详情展示。
 *
 * - 继承 [BaseCrudController] 的接口：对单条 IP 规则做新增、编辑、删除、分页等常规管理。
 * - 本类额外提供的只读接口：用于在列表/详情中同时展示**访问规则属性**（租户、系统、规则类型等）与**IP 段**（起止、类型、是否启用等），便于前端少次请求拼齐一屏数据。
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

    /** 只读：用于「规则 + IP 段」合一列表/详情的查询与分页。 */
    @Resource
    private lateinit var vSysAccessRuleIpService: VSysAccessRuleIpService

    /** 与基类注入的 IP 规则服务相同，用于本类扩展的按规则拉取 IP 列表等接口。 */
    @Resource
    private lateinit var sysAccessRuleIpService: ISysAccessRuleIpService

    /**
     * 查询某一「访问规则」下已配置的全部 IP 段（仅 IP 侧字段，适合在已选定规则后展示子表或侧栏列表）。
     *
     * @param ruleId 访问规则主键（与规则维护页、规则详情中的规则 id 一致）
     * @return 该规则下每条 IP 规则一行；无数据时为空列表
     */
    @GetMapping("/getIpsByRuleId")
    fun getIpsByRuleId(@RequestParam ruleId: String): List<SysAccessRuleIpRow> =
        sysAccessRuleIpService.getIpsByRuleId(ruleId)

    /**
     * 按列表/检索结果中的行 id 拉取**一条**「访问规则 + IP 段」合一数据，用于详情抽屉或只读查看。
     * 若某规则尚未配置任何 IP 段，列表里仍可能有一条仅含规则信息、IP 信息为空的占位行，此时行 id 与规则 id 相同。
     *
     * @param id 列表或检索接口返回的 `id` 字段
     * @return 命中则返回一行；不存在则 `null`
     */
    @GetMapping("/getAccessRuleWithIp")
    fun getAccessRuleWithIp(id: String): VSysAccessRuleWithIpRow? =
        vSysAccessRuleIpService.get(id, VSysAccessRuleWithIpRow::class)

    /**
     * 分页查询「访问规则 + IP 段」合一数据，供管理端大表筛选、排序与翻页（条件与分页字段见请求体）。
     *
     * @param query 筛选条件（租户、系统、规则类型、是否启用等）与分页参数
     * @return 当前页数据与总条数等分页信息
     */
    @PostMapping("/pagingSearchAccessRuleWithIp")
    @Suppress("UNCHECKED_CAST")
    fun pagingSearchAccessRuleWithIp(
        @RequestBody query: VSysAccessRuleWithIpQuery,
    ): PagingSearchResult<VSysAccessRuleWithIpRow> =
        vSysAccessRuleIpService.pagingSearch(query) as PagingSearchResult<VSysAccessRuleWithIpRow>

    /**
     * 列出某一访问规则对应的全部「规则 + IP」行，用于进入规则详情后一次性渲染该规则下所有 IP 段（含「尚未添加 IP」时仅展示规则头信息的一行）。
     *
     * @param parentId 访问规则主键
     * @return 多行：每个 IP 段一行；无 IP 时通常仍有一行仅规则信息
     */
    @GetMapping("/searchByParentId")
    fun searchByParentId(@RequestParam parentId: String): List<VSysAccessRuleWithIpRow> =
        vSysAccessRuleIpService.searchByParentId(parentId)

    /**
     * 按**业务系统**与**租户**两个维度，列出当前可见的「规则 + IP」数据，常用于按租户/系统切换后的总览或联调排查。
     * 不传 `tenantId` 或传空字符串时，表示只查**未绑定具体租户**（平台级）的规则数据。
     *
     * @param systemCode 业务系统编码（与租户、子系统选择器一致）
     * @param tenantId 租户 id；省略或空串表示平台租户场景
     * @return 符合条件的行列表，无则空列表
     */
    @GetMapping("/searchBySystemCodeAndTenantId")
    fun searchBySystemCodeAndTenantId(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?,
    ): List<VSysAccessRuleWithIpRow> =
        vSysAccessRuleIpService.searchBySystemCodeAndTenantId(
            systemCode,
            tenantId?.takeIf { it.isNotBlank() },
        )

    /**
     * 仅更新单条 IP 访问规则的启用状态（列表开关）。
     *
     * @param id `sys_access_rule_ip.id`
     * @param active 是否启用
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean = service.updateActive(id, active)
}
