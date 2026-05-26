package io.kudos.ms.auth.core.platform.enums.dict

import io.kudos.base.enums.ienums.IModuleEnum

/**
 * Placeholder module enum for the auth service.
 *
 * Implements [IModuleEnum] and currently has **no members**. Kept only to satisfy the framework's
 * scanning contract for the `IModuleEnum` SPI (dictionary / logging / permission components
 * aggregate and classify by `IModuleEnum` subtype). When the auth subsystem is later split into
 * finer-grained modules (such as `ROLE` / `GROUP` / `PERMISSION`), append new enum values here
 * rather than declaring ad-hoc enums elsewhere.
 *
 * @author K
 * @since 1.0.0
 */
enum class AuthModuleEnum : IModuleEnum {



}