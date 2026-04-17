package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * ip访问规则数据库实体（`sys_access_rule_ip`）。
 *
 * **列语义随 [ipTypeDictCode] 变化**（均为无符号整数，以 `NUMERIC(39,0)` 存十进制）：
 * - `ipv4`：`ipStart`/`ipEnd` 为 32 位起止（值 ≤ 2³²−1）。
 * - `ipv6`：`ipStart`/`ipEnd` 为 128 位起止（值 ≤ 2¹²⁸−1），各为完整地址的单一数值，不拆高低位。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface SysAccessRuleIp : IManagedDbEntity<String, SysAccessRuleIp> {

    @get:Sortable
    override var id: String

    /** 区间起点：ipv4 为 32 位；ipv6 为 128 位无符号整数的十进制。 */
    var ipStart: BigDecimal

    /** 区间终点；比较与校验均按数值大小。 */
    var ipEnd: BigDecimal

    /** ip类型字典代码 */
    var ipTypeDictCode: String

    /** 过期时间 */
    var expirationTime: LocalDateTime?

    /** 父规则id */
    var parentRuleId: String

    companion object : DbEntityFactory<SysAccessRuleIp>() {

        /**
         * 由表单创建载荷与父规则主键组装一条待持久化的实体（仅填充入参中已有字段，其余保持 Ktorm 默认）。
         *
         * @param formCreate 创建表单
         * @param parentRuleId 父访问规则主键
         * @return 新实体实例
         */
        fun of(formCreate: SysAccessRuleIpFormCreate, parentRuleId: String): SysAccessRuleIp {
            return SysAccessRuleIp {
                this.ipStart = formCreate.getIpStart()!!
                this.ipEnd = formCreate.getIpEnd()!!
                this.ipTypeDictCode = formCreate.ipTypeDictCode
                this.expirationTime = formCreate.expirationDate
                this.remark = formCreate.remark
                this.parentRuleId = parentRuleId
            }
        }

    }

}