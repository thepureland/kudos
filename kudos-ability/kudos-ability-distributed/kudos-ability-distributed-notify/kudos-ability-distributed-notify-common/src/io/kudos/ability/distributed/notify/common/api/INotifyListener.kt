package io.kudos.ability.distributed.notify.api

import io.kudos.ability.distributed.notify.model.NotifyMessageVo


/**
 * 创建人： Younger
 * 日期： 2022/11/14 14:14
 * 描述：
 */
interface INotifyListener<T> {
    fun notifyType(): String?

    fun notifyProcess(notifyMessageVo: NotifyMessageVo<T?>?)
}
