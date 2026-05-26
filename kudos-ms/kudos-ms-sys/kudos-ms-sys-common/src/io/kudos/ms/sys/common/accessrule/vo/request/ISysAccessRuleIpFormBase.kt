package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.AbstractGroupSequenceProvider
import io.kudos.base.bean.validation.support.Group
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.group.GroupSequenceProvider
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * IP access rule form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
@GroupSequenceProvider(ISysAccessRuleIpFormBase.GroupSequenceProvider::class)
interface ISysAccessRuleIpFormBase {

    /** IPv4 start (string form) */
    @get:Matches(RegExpEnum.IPV4_FULL, groups = [Group.First::class])
    val ipv4StartStr: String?

    /** IPv4 end (string form) */
    @get:Matches(RegExpEnum.IPV4_FULL, groups = [Group.First::class])
    @get:Compare(anotherProperty = "ipv4StartStr", logic = LogicOperatorEnum.GE, groups = [Group.First::class], message = "sys.valid-msg.accessrule.ge-ip-start")
    val ipv4EndStr: String?

    /** IPv6 start (string form) */
    @get:Matches(RegExpEnum.IPV6_FULL, groups = [Group.Second::class])
    val ipv6StartStr: String?

    /** IPv6 end (string form) */
    @get:Matches(RegExpEnum.IPV6_FULL, groups = [Group.Second::class])
    @get:Compare(anotherProperty = "ipv6StartStr", logic = LogicOperatorEnum.GE, groups = [Group.Second::class], message = "sys.valid-msg.accessrule.ge-ip-start")
    val ipv6EndStr: String?

    /** IP type dict code */
    @get:NotBlank
    @get:MaxLength(4)
    @get:DictItemCode(dictType = SysDictTypes.IP_TYPE, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val ipTypeDictCode: String

    /** Expiration time */
    val expirationDate: LocalDateTime?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?

    class GroupSequenceProvider : AbstractGroupSequenceProvider<ISysAccessRuleIpFormBase>() {

        override fun getGroups(bean: ISysAccessRuleIpFormBase): List<KClass<*>> {
            val defaultGroupSequence = mutableListOf<KClass<*>>()
            if (bean.ipTypeDictCode == IpTypeEnum.IPV4.code) {
                defaultGroupSequence.add(Group.First::class)
            } else {
                defaultGroupSequence.add(Group.Second::class)
            }
            return defaultGroupSequence
        }

    }

}
