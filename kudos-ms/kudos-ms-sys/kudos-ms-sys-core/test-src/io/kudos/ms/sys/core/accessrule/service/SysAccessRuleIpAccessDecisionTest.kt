package io.kudos.ms.sys.core.accessrule.service

import io.kudos.ms.sys.common.accessrule.enums.AccessRuleTypeEnum
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.core.accessrule.service.impl.decideIpAccess
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for the black/white-list decision logic of `checkIpAccess`
 * (the internal `decideIpAccess` function); no Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAccessRuleIpAccessDecisionTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 6, 11, 12, 0, 0)

    private val ip = BigDecimal.valueOf(2130706433L) // 127.0.0.1

    private fun entry(
        ipStart: Long,
        ipEnd: Long,
        ruleType: AccessRuleTypeEnum? = null,
        expirationTime: LocalDateTime? = null,
    ) = SysAccessRuleIpCacheEntry(
        id = "test-id",
        ipStart = BigDecimal.valueOf(ipStart),
        ipEnd = BigDecimal.valueOf(ipEnd),
        ipTypeDictCode = "ipv4",
        expirationTime = expirationTime,
        accessRuleTypeDictCode = ruleType?.code,
    )

    @Test
    fun blacklistHitIsDenied() {
        val rules = listOf(entry(2130706432L, 2130706440L, AccessRuleTypeEnum.BLACKLIST))
        assertFalse(decideIpAccess(ip, rules, now))
    }

    @Test
    fun blacklistMissIsAllowed() {
        val rules = listOf(entry(3232235520L, 3232235775L, AccessRuleTypeEnum.BLACKLIST)) // 192.168.0.0/24
        assertTrue(decideIpAccess(ip, rules, now))
    }

    @Test
    fun whitelistHitIsAllowed() {
        val rules = listOf(entry(2130706432L, 2130706440L, AccessRuleTypeEnum.WHITELIST))
        assertTrue(decideIpAccess(ip, rules, now))
    }

    @Test
    fun whitelistMissIsDenied() {
        // whitelist mode: any whitelist rule present and not hit means deny by default
        val rules = listOf(entry(3232235520L, 3232235775L, AccessRuleTypeEnum.WHITELIST))
        assertFalse(decideIpAccess(ip, rules, now))
    }

    @Test
    fun noRulesMeansAllowedByDefault() {
        assertTrue(decideIpAccess(ip, emptyList(), now))
    }

    @Test
    fun unlimitedRuleNeverDenies() {
        val rules = listOf(entry(2130706432L, 2130706440L, AccessRuleTypeEnum.UNLIMITED))
        assertTrue(decideIpAccess(ip, rules, now))
    }

    @Test
    fun blacklistWinsOverWhitelistOnDoubleHit() {
        val rules = listOf(
            entry(2130706432L, 2130706440L, AccessRuleTypeEnum.WHITELIST),
            entry(2130706433L, 2130706433L, AccessRuleTypeEnum.BLACKLIST),
        )
        assertFalse(decideIpAccess(ip, rules, now))
    }

    @Test
    fun expiredBlacklistIsIgnored() {
        val rules = listOf(
            entry(2130706432L, 2130706440L, AccessRuleTypeEnum.BLACKLIST, expirationTime = now.minusDays(1))
        )
        assertTrue(decideIpAccess(ip, rules, now))
    }

    @Test
    fun expiredWhitelistDoesNotGate() {
        // the only whitelist rule expired => dimension degenerates to "no restriction"
        val rules = listOf(
            entry(2130706432L, 2130706440L, AccessRuleTypeEnum.WHITELIST, expirationTime = now.minusDays(1))
        )
        assertTrue(decideIpAccess(ip, rules, now))
    }

    @Test
    fun combinedTypeIsTreatedAsWhitelist() {
        val hitRules = listOf(entry(2130706432L, 2130706440L, AccessRuleTypeEnum.WHITELIST_BLACKLIST))
        assertTrue(decideIpAccess(ip, hitRules, now))

        val missRules = listOf(entry(3232235520L, 3232235775L, AccessRuleTypeEnum.WHITELIST_BLACKLIST))
        assertFalse(decideIpAccess(ip, missRules, now))
    }

    @Test
    fun legacyEntryWithoutTypeNeverDenies() {
        // legacy cache entries (no rule type) neither blacklist nor impose a whitelist requirement
        val rules = listOf(entry(2130706432L, 2130706440L, ruleType = null))
        assertTrue(decideIpAccess(ip, rules, now))

        val missRules = listOf(entry(3232235520L, 3232235775L, ruleType = null))
        assertTrue(decideIpAccess(ip, missRules, now))
    }
}
