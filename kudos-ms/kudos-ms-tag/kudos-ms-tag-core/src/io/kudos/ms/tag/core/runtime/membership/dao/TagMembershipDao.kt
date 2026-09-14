package io.kudos.ms.tag.core.runtime.membership.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.membership.model.po.TagMembership
import io.kudos.ms.tag.core.runtime.membership.model.table.TagMemberships
import org.springframework.stereotype.Repository

@Repository
open class TagMembershipDao : BaseCrudDao<String, TagMembership, TagMemberships>()
