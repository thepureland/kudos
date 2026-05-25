package io.kudos.ms.msg.client.template.proxy

import io.kudos.ms.msg.client.template.fallback.MsgTemplateFallbackFactory
import io.kudos.ms.msg.common.template.api.IMsgTemplateApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 消息模板客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-template", fallbackFactory = MsgTemplateFallbackFactory::class)
interface IMsgTemplateProxy : IMsgTemplateApi
