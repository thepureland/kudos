package io.kudos.ability.distributed.stream.common.handler

/**
 * 流式消息失败处理器注册表
 * 用于管理和查找不同绑定名称对应的失败处理器
 */
object StreamFailHandlerItem {
    /** bindName → IStreamFailHandler 注册表；写入在 Spring 装配期，读在消息处理期 */
    private val STREAM_HANDLER = mutableMapOf<String, IStreamFailHandler>()

    /**
     * 注册一个 bindName 对应的失败处理器。
     *
     * @param bindName Spring Cloud Stream 的 binding 名（如 `outboundOrder-out-0`）
     * @param listener 失败处理器实例
     * @author K
     * @since 1.0.0
     */
    fun put(bindName: String, listener: IStreamFailHandler) {
        STREAM_HANDLER[bindName] = listener
    }

    /**
     * 判断指定 bindName 是否注册过失败处理器。
     *
     * @param bindName binding 名
     * @return true 表示已注册
     * @author K
     * @since 1.0.0
     */
    fun hasFailedHandler(bindName: String?): Boolean {
        return STREAM_HANDLER.containsKey(bindName)
    }

    /**
     * 取 bindName 对应的失败处理器；未注册时回落到 [IStreamFailHandler.DEFAULT_BIND_NAME] 对应的默认实现。
     * 默认实现也未注册时返回 null，由调用方决定如何降级。
     *
     * @param bindName binding 名
     * @return 已注册的处理器或默认实现，皆无则 null
     * @author K
     * @since 1.0.0
     */
    fun get(bindName: String): IStreamFailHandler? {
        //没有设置，则用默认的实现
        if (!STREAM_HANDLER.containsKey(bindName)) {
            return STREAM_HANDLER.get(IStreamFailHandler.DEFAULT_BIND_NAME)
        }
        return STREAM_HANDLER.get(bindName)
    }
}
