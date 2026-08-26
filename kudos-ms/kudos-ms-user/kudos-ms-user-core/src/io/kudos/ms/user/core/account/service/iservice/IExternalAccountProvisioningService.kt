package io.kudos.ms.user.core.account.service.iservice

import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.po.UserAccountThird

/** Creates an external-only local account and its first verified identity binding atomically. */
interface IExternalAccountProvisioningService {

    fun provision(command: ExternalAccountProvisioningCommand): UserAccountThird
}
