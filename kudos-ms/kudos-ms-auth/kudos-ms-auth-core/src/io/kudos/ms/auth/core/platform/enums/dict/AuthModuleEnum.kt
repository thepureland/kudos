package io.kudos.ms.auth.core.platform.enums.dict

import io.kudos.base.enums.ienums.IModuleEnum

/**
 * 鉴权服务的模块枚举占位。
 *
 * 实现 [IModuleEnum]、目前**无任何成员**——保留只为对接框架对 `IModuleEnum` SPI 的扫描契约
 * （字典 / 日志 / 权限组件会按 `IModuleEnum` 子类型聚合分类）。后续 auth 子系统拆出更细粒度的
 * 模块时（如 `ROLE` / `GROUP` / `PERMISSION`）在此追加枚举值即可，避免散落在各处自创 enum。
 *
 * @author K
 * @since 1.0.0
 */
enum class AuthModuleEnum : IModuleEnum {



}