package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * 创建人： Younger
 * 日期： 2022/11/14 14:14
 * 描述：
 */
interface  INotifyListener {

    fun notifyType(): String

    fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>)

}
