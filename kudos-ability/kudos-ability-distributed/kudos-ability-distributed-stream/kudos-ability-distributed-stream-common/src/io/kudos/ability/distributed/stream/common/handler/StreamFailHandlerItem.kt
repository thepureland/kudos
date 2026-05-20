package io.kudos.ability.distributed.stream.common.handler

/**
 * 流式消息失败处理器注册表
 * 用于管理和查找不同绑定名称对应的失败处理器
 */
object StreamFailHandlerItem {
    private val STREAM_HANDLER = mutableMapOf<String, IStreamFailHandler>()

    fun put(bindName: String, listener: IStreamFailHandler) {
        STREAM_HANDLER[bindName] = listener
    }

    fun hasFailedHandler(bindName: String?): Boolean = STREAM_HANDLER.containsKey(bindName)

    fun get(bindName: String): IStreamFailHandler? =
        //没有设置，则用默认的实现
        STREAM_HANDLER[bindName] ?: STREAM_HANDLER[IStreamFailHandler.DEFAULT_BIND_NAME]
}
