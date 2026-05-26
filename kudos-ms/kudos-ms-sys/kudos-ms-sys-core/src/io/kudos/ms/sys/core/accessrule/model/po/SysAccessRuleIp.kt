package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * IP access rule database entity (`sys_access_rule_ip`).
 *
 * **Column semantics vary with [ipTypeDictCode]** (all are unsigned integers stored as decimal `NUMERIC(39,0)`):
 * - `ipv4`: `ipStart`/`ipEnd` are 32-bit range start/end (value ≤ 2³²−1).
 * - `ipv6`: `ipStart`/`ipEnd` are 128-bit range start/end (value ≤ 2¹²⁸−1); each is a single numeric value of the full address, not split into high/low halves.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface SysAccessRuleIp : IManagedDbEntity<String, SysAccessRuleIp> {

    @get:Sortable
    override var id: String

    /** Range start: 32-bit for ipv4; decimal of a 128-bit unsigned integer for ipv6. */
    var ipStart: BigDecimal

    /** Range end; both comparison and validation use numeric magnitude. */
    var ipEnd: BigDecimal

    /** IP type dict code */
    var ipTypeDictCode: String

    /** Expiration time */
    var expirationTime: LocalDateTime?

    /** Parent rule id */
    var parentRuleId: String

    companion object : DbEntityFactory<SysAccessRuleIp>() {

        /**
         * Assemble an entity to be persisted from the form-create payload and the parent rule's primary key
         * (only fills fields present in the input; the rest keep Ktorm defaults).
         *
         * @param formCreate create form
         * @param parentRuleId parent access rule primary key
         * @return new entity instance
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