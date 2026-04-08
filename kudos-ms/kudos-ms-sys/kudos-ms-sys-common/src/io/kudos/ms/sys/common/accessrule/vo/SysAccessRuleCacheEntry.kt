package io.kudos.ms.sys.common.accessrule.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 访问规则在分布式/本地缓存中的值对象（与表 `sys_access_rule` 核心字段对应）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleCacheEntry (

    /** 主键 */
    override val id: String,

    /**
     * 租户 id；**空串**表示平台级（与库表 `tenant_id IS NULL` 对应），与 Hash 副属性索引取值一致。
     */
    val tenantId: String,

    /** 系统编码（子系统 / 系统维度 `system_code`） */
    val systemCode: String,

    /** 规则类型字典代码 */
    val accessRuleTypeDictCode: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8253788046293050901L
    }

}
