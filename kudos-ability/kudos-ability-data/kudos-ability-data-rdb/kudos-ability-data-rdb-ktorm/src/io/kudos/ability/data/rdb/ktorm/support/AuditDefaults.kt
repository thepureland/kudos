package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.common.IAuditable
import io.kudos.context.core.KudosContextHolder
import java.time.LocalDateTime

/**
 * Centralized auto-fill rules for the [IAuditable] columns on insert / update.
 *
 * Mirrors what soul's MyBatis-Plus `FieldMetaObjectHandler` does, expressed as plain Kotlin so it's
 * trivially callable from anywhere a DAO touches an entity — primarily [BaseCrudDao]. Extracted out
 * of the DAO so the rules are independently testable: pass in any [IAuditable] subject + a fixed
 * `now` / `userId` and assert the resulting field state.
 *
 * Rules:
 * - Fields the caller already populated are **never** overwritten — auto-fill is a default, not a
 *   policy. Lets ops scripts re-import data with explicit timestamps.
 * - `userId` comes from [KudosContextHolder.getOrNull]; if no request context is active (typical for
 *   scheduled jobs / startup tasks), the field stays null rather than being fabricated to "system",
 *   leaving the choice to the caller.
 * - Only `userId` is auto-filled, not `userName` — the kudos context exposes `IIdEntity<String>` for
 *   the current user which does not carry a name. Business teams with a richer user object should
 *   fill `createUserName` / `updateUserName` themselves before calling save.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuditDefaults {

    /**
     * Fills `createTime`, `createUserId`, `updateTime`, `updateUserId` when null. The update
     * fields are populated on insert too so the very first version of the row has matching audit
     * timestamps — the same trick MyBatis-Plus's default field handler uses.
     */
    fun fillForInsert(
        target: IAuditable,
        now: LocalDateTime = LocalDateTime.now(),
        userId: String? = currentUserId(),
    ) {
        if (target.createTime == null) target.createTime = now
        if (target.createUserId == null) target.createUserId = userId
        if (target.updateTime == null) target.updateTime = now
        if (target.updateUserId == null) target.updateUserId = userId
    }

    /** Fills `updateTime` / `updateUserId` when null. Creation fields are intentionally untouched. */
    fun fillForUpdate(
        target: IAuditable,
        now: LocalDateTime = LocalDateTime.now(),
        userId: String? = currentUserId(),
    ) {
        if (target.updateTime == null) target.updateTime = now
        if (target.updateUserId == null) target.updateUserId = userId
    }

    /**
     * Same as [fillForUpdate] but operates on a `Map<propertyName, value>` — used by DAO paths that
     * carry update properties as a map (e.g. `updateProperties(id, map)`). Entries the caller put in
     * the map are kept verbatim; missing keys are added with auto-fill defaults.
     */
    fun fillForUpdate(
        properties: MutableMap<String, Any?>,
        now: LocalDateTime = LocalDateTime.now(),
        userId: String? = currentUserId(),
    ) {
        val updateTimeKey = IAuditable::updateTime.name
        if (!properties.containsKey(updateTimeKey)) properties[updateTimeKey] = now
        val updateUserIdKey = IAuditable::updateUserId.name
        if (!properties.containsKey(updateUserIdKey)) properties[updateUserIdKey] = userId
    }

    /**
     * Map variant of [fillForInsert]. Used by the plain-bean insert path that carries values as a
     * map (e.g. `batchInsert(payloadList)` where each payload is a DTO, not an entity). Entries
     * already in the map are kept verbatim; missing audit keys are added with defaults.
     */
    fun fillForInsert(
        properties: MutableMap<String, Any?>,
        now: LocalDateTime = LocalDateTime.now(),
        userId: String? = currentUserId(),
    ) {
        val createTimeKey = IAuditable::createTime.name
        if (!properties.containsKey(createTimeKey)) properties[createTimeKey] = now
        val createUserIdKey = IAuditable::createUserId.name
        if (!properties.containsKey(createUserIdKey)) properties[createUserIdKey] = userId
        val updateTimeKey = IAuditable::updateTime.name
        if (!properties.containsKey(updateTimeKey)) properties[updateTimeKey] = now
        val updateUserIdKey = IAuditable::updateUserId.name
        if (!properties.containsKey(updateUserIdKey)) properties[updateUserIdKey] = userId
    }

    /** Current user id from the request-scoped [KudosContextHolder], or null when no context exists. */
    private fun currentUserId(): String? = KudosContextHolder.getOrNull()?.user?.id
}
