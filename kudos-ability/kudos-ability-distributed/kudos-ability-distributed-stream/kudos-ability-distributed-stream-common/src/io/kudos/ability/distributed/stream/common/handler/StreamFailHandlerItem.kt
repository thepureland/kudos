package io.kudos.ability.distributed.stream.common.handler

object StreamFailHandlerItem {
    private val STREAM_HANDLER = mutableMapOf<String, IStreamFailHandler>()

    fun put(bindName: String, listener: IStreamFailHandler) {
        STREAM_HANDLER.put(bindName, listener)
    }

    fun hasFailedHandler(bindName: String?): Boolean {
        return STREAM_HANDLER.containsKey(bindName)
    }

    fun get(bindName: String): IStreamFailHandler? {
        //没有设置，则用默认的实现
        if (!STREAM_HANDLER.containsKey(bindName)) {
            return STREAM_HANDLER.get(IStreamFailHandler.Companion.DEFAULT_BIND_NAME)
        }
        return STREAM_HANDLER.get(bindName)
    }
}
