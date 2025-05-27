package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * 微服务应用2的Feign客户端
 *
 * @author will
 * @since 5.1.1
 */
@FeignClient(value = "ms21")
interface IFeignClient21 : IClient2
