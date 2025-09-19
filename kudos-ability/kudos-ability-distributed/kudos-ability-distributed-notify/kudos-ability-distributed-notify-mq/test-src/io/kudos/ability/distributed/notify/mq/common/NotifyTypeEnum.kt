package io.kudos.ability.distributed.notify.mq.common

import io.kudos.base.enums.ienums.IDictEnum


enum class NotifyTypeEnum(
    override val code: String,
    override val trans: String
) : IDictEnum {

    DS("DS", "数据源");

}
