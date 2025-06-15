package io.kudos.context.init

import io.kudos.base.logger.LogFactory


/**
 * 组件初始化器接口
 *
 * @author K
 * @since 1.0.0
 */
interface IComponentInitializer {

    /**
     * 获取组件的名称
     *
     * @return 组件名称
     * @author K
     * @since 1.0.0
     */
    fun getComponentName(): String

    /**
     * 组件初始化工作开始前
     *
     * @author K
     * @since 1.0.0
     */
    fun beforeInit() {
        LogFactory.getLog(this).info(">>>>>>>>>>>>>>>>>>>>  组件【${getComponentName()}】开始初始化...")
    }

    /**
     * 组件初始化工作完成后
     *
     * @author K
     * @since 1.0.0
     */
    fun afterInit() {
        LogFactory.getLog(this).info("<<<<<<<<<<<<<<<<<<<<  组件【${getComponentName()}】初始化完成.")
    }

}