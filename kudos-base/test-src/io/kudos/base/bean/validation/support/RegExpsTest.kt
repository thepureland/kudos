package io.kudos.base.bean.validation.support

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [RegExps] 各常量的匹配行为回归测试
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class RegExpsTest {

    private fun rx(pattern: String) = Regex(pattern)

    private fun assertMatches(pattern: String, vararg inputs: String) {
        val r = rx(pattern)
        inputs.forEach { s ->
            assertTrue(r.matches(s), "expected match: <$s> for pattern")
        }
    }

    private fun assertNotMatches(pattern: String, vararg inputs: String) {
        val r = rx(pattern)
        inputs.forEach { s ->
            assertFalse(r.matches(s), "expected no match: <$s>")
        }
    }

    @Test
    fun cnMainlandMobile() {
        assertMatches(
            RegExps.Communication.CN_MAINLAND_MOBILE,
            "13800138000",
            "15912345678",
            "18800001111",
            "13012345678",
            "16612345678",
            "19812345678",
            "19912345678",
        )
        assertNotMatches(
            RegExps.Communication.CN_MAINLAND_MOBILE,
            "",
            "12345678901",
            "23800138000",
            "1380013800",
            "138001380001",
            "abcdefghijk",
            "+8613800138000",
            "138 0013 8000",
            "24800138000",
            // JVM \\d 不含全角数字，与常见「复制粘贴」号码形态
            "１３８００１３８０００",
        )
    }

    @Test
    fun qqNumber() {
        assertMatches(
            RegExps.Communication.QQ_NUMBER,
            "12345",
            "10000",
            "12345678901",
            "99999999999",
        )
        assertNotMatches(
            RegExps.Communication.QQ_NUMBER,
            "",
            "1234",
            "123456789012",
            "12a45",
            " 12345",
            "-12345",
            "１２３４５６７８９０１１",
        )
    }

    @Test
    fun phoneDigits7_20() {
        assertMatches(
            RegExps.Communication.PHONE_DIGITS_7_20,
            "1234567",
            "0000000",
            "99999999999999999999",
            "12345678901234567890",
        )
        assertNotMatches(
            RegExps.Communication.PHONE_DIGITS_7_20,
            "",
            "123456",
            "123456789012345678901",
            "12a4567",
            "123456 7",
            "１２３４５６７",
        )
    }

    @Test
    fun ipv4() {
        assertMatches(
            RegExps.Network.IPV4,
            "0.0.0.0",
            "127.0.0.1",
            "192.168.1.1",
            "255.255.255.255",
            "10.0.0.1",
            "172.16.0.1",
            "1.2.3.4",
            // 段内写法宽松：01 仍被底层 \d? 形态接受
            "192.168.01.1",
        )
        assertNotMatches(
            RegExps.Network.IPV4,
            "",
            "256.1.1.1",
            "192.168.1",
            "192.168.1.1.1",
            "300.1.1.1",
        )
    }

    @Test
    fun httpUrl() {
        assertMatches(
            RegExps.Network.HTTP_URL,
            "https://example.com",
            "http://example.com/",
            "https://api.example.com/v1/resource",
            "https://example.com/search?q=test&lang=zh",
            // 路径与 query 在宽松 URL 正则中可为 BMP Unicode（含中文）
            "https://example.com/中文路径",
            "https://example.com/search?q=中文",
        )
        assertNotMatches(
            RegExps.Network.HTTP_URL,
            "",
            "not-a-url",
            "://missing-scheme",
            // 当前宽松 URL 正则对纯 localhost 主机等形式可能不匹配，以实际为准
            "http://localhost:8080/",
            "https:// example.com/",
        )
    }

    @Test
    fun telOrCnMobile() {
        assertMatches(
            RegExps.Communication.TEL_OR_CN_MOBILE,
            "010-12345678",
            "021-1234567",
            "0755-12345678",
            "0551-12345678",
            "13800138000",
            "15912345678",
            "18800001111",
        )
        assertNotMatches(
            RegExps.Communication.TEL_OR_CN_MOBILE,
            "",
            "12345",
            "abc-def-ghij",
            "138-0013-8000",
            "+8613800138000",
            // 全角连字符 U+FF0D，非 ASCII '-'
            "010－12345678",
        )
    }

    @Test
    fun shortPersonName() {
        assertMatches(
            RegExps.Name.SHORT_PERSON_NAME,
            "张三",
            "ZhangSan",
            "李·四",
            "ab",
            "王小明",
            "John·Doe",
            "a".repeat(30),
        )
        assertNotMatches(
            RegExps.Name.SHORT_PERSON_NAME,
            "",
            "a",
            "a".repeat(31),
            "王",
        )
    }

    @Test
    fun realPersonName() {
        assertMatches(
            RegExps.Name.REAL_PERSON_NAME,
            "张三",
            "John Smith",
            "李 四",
            "王五",
            "佐藤 太郎",
        )
        assertNotMatches(
            RegExps.Name.REAL_PERSON_NAME,
            "",
            "12345",
            "999",
            // 中间连字符不在允许字符集中
            "Mary-Jane",
        )
    }

    @Test
    fun bankAccountHolderName() {
        assertMatches(
            RegExps.Name.BANK_ACCOUNT_HOLDER_NAME,
            "张三",
            "John",
            "王五（公司）",
            "Li（个人）",
        )
        assertNotMatches(
            RegExps.Name.BANK_ACCOUNT_HOLDER_NAME,
            "",
            "A",
            "a".repeat(50),
        )
    }

    @Test
    fun payerDisplayName() {
        assertMatches(
            RegExps.Name.PAYER_DISPLAY_NAME,
            "张三",
            "Li Si",
            "王五",
            "张·三丰",
        )
        assertNotMatches(
            RegExps.Name.PAYER_DISPLAY_NAME,
            "",
            "a".repeat(50),
        )
    }

    @Test
    fun textWithoutSpecialChars() {
        assertMatches(
            RegExps.Text.TEXT_WITHOUT_SPECIAL_CHARS,
            "",
            "普通文本",
            "hello world",
            "123,456（中文括号）",
            "多行\n第二行",
        )
        assertNotMatches(
            RegExps.Text.TEXT_WITHOUT_SPECIAL_CHARS,
            "a&b",
            "x=y",
            "a{b}",
            "有…非法",
            "破折号—",
            "a/b",
        )
    }

    @Test
    fun email() {
        // [RegExps.Communication.EMAIL] 实现对域名段形态较严，样例以实际匹配为准
        assertMatches(
            RegExps.Communication.EMAIL,
            "user@example.com",
            "a1@mail.example.org",
            "admin@site.example.org",
        )
        assertNotMatches(
            RegExps.Communication.EMAIL,
            "",
            "@nodomain",
            "no-at",
            "a@b.co",
            " user@example.com",
            // 本地部分与域名段均为 [a-zA-Z0-9] 系，不含中文
            "用户@example.com",
            "a@邮箱.cn",
        )
    }

    @Test
    fun mailOrCnMobile() {
        assertMatches(
            RegExps.Communication.MAIL_OR_CN_MOBILE,
            "user@example.com",
            "13800138000",
            "15812345678",
        )
        assertNotMatches(
            RegExps.Communication.MAIL_OR_CN_MOBILE,
            "",
            "12345",
            "user@",
            "24800138000",
            // [MAIL_OR_CN_MOBILE] 手机号分支比 [CN_MAINLAND_MOBILE] 窄，不含 19x 等号段
            "19812345678",
            "用户@example.com",
        )
    }

    @Test
    fun cnLandlinePhone() {
        assertMatches(
            RegExps.Communication.CN_LANDLINE_PHONE,
            "010-12345678",
            "0755-12345678",
            "010-12345678-9",
            "0755-1234567-123",
        )
        assertNotMatches(
            RegExps.Communication.CN_LANDLINE_PHONE,
            "",
            "1234",
            "01012345678",
            "010--12345678",
            "010－12345678",
        )
    }

    @Test
    fun loginPassword() {
        assertMatches(
            RegExps.Security.LOGIN_PASSWORD,
            "abc123",
            "Passw0rd!",
            "a".repeat(6),
            "x".repeat(20),
            "~key-1",
            "p@ss#1",
            "{}[]|:",
        )
        assertNotMatches(
            RegExps.Security.LOGIN_PASSWORD,
            "",
            "12345",
            "a".repeat(21),
            "含中文",
            "pass word",
        )
    }

    @Test
    fun securityPinSixDigits() {
        assertMatches(RegExps.Security.SECURITY_PIN_SIX_DIGITS, "000000", "123456", "987654")
        assertNotMatches(
            RegExps.Security.SECURITY_PIN_SIX_DIGITS,
            "",
            "12345",
            "1234567",
            "12a456",
            " 123456",
            "１２３４５６",
        )
    }

    @Test
    fun positiveIntText() {
        assertMatches(RegExps.Numeric.POSITIVE_INT_TEXT, "1", "9", "123456", "10")
        assertNotMatches(
            RegExps.Numeric.POSITIVE_INT_TEXT,
            "",
            "0",
            "01",
            "-1",
            "1.5",
            "+2",
            "１２３",
        )
    }

    @Test
    fun ipv4SemicolonList() {
        assertMatches(
            RegExps.Network.IPV4_SEMICOLON_LIST,
            "192.168.0.1",
            "10.0.0.1;10.0.0.2",
            "1.1.1.1;2.2.2.2;3.3.3.3",
        )
        assertNotMatches(
            RegExps.Network.IPV4_SEMICOLON_LIST,
            "",
            "192.168.0.1;",
            ";192.168.0.1",
            "256.0.0.1",
        )
    }

    @Test
    fun ipv6Full() {
        assertMatches(
            RegExps.Network.IPV6_FULL,
            "2001:0db8:0000:0000:0000:0000:0000:0001",
            "0000:0000:0000:0000:0000:0000:0000:0000",
            "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        )
        assertNotMatches(RegExps.Network.IPV6_FULL, "", "2001:db8::1", "gggg::1", "2001::1::1")
    }

    @Test
    fun ipv6Compact() {
        assertMatches(RegExps.Network.IPV6_COMPACT, "::1", "2001:db8::", "fe80::1", "::")
        assertNotMatches(RegExps.Network.IPV6_COMPACT, "", "not-ipv6", "2001:db8::1::2")
    }

    @Test
    fun singleCharRepeated() {
        assertMatches(RegExps.CharacterSet.SINGLE_CHAR_REPEATED, "aaa", "1111", "..", "··", "你你你")
        assertNotMatches(RegExps.CharacterSet.SINGLE_CHAR_REPEATED, "", "a", "aba", "aab")
    }

    @Test
    fun digitsNonEmpty() {
        assertMatches(RegExps.CharacterSet.DIGITS_NON_EMPTY, "0", "123", "000")
        assertNotMatches(RegExps.CharacterSet.DIGITS_NON_EMPTY, "", "12a", " 1", "1\n", "１２３")
    }

    @Test
    fun latinLettersOnly() {
        assertMatches(RegExps.CharacterSet.LATIN_LETTERS_ONLY, "a", "AbC", "XYZ")
        assertNotMatches(RegExps.CharacterSet.LATIN_LETTERS_ONLY, "", "a1", "a ", "ñ", "中文")
    }

    @Test
    fun passwordStrengthLettersOnly() {
        assertSame(RegExps.CharacterSet.LATIN_LETTERS_ONLY, RegExps.Security.PASSWORD_STRENGTH_LETTERS_ONLY)
        assertMatches(RegExps.Security.PASSWORD_STRENGTH_LETTERS_ONLY, "abc", "XYZ", "z")
        assertNotMatches(RegExps.Security.PASSWORD_STRENGTH_LETTERS_ONLY, "a1", "", "a-b", "中文")
    }

    @Test
    fun latinLowercaseOnly() {
        assertMatches(RegExps.CharacterSet.LATIN_LOWERCASE_ONLY, "abc", "z", "hello")
        assertNotMatches(RegExps.CharacterSet.LATIN_LOWERCASE_ONLY, "", "A", "a1", "a\n", "汉字")
    }

    @Test
    fun lowercaseAlnumNotAllDigits() {
        assertMatches(RegExps.CharacterSet.LOWERCASE_ALNUM_NOT_ALL_DIGITS, "a1", "ab", "1a", "0z9")
        assertNotMatches(RegExps.CharacterSet.LOWERCASE_ALNUM_NOT_ALL_DIGITS, "", "123", "A1", "a-A", "a中文")
    }

    @Test
    fun nickName() {
        assertMatches(RegExps.Name.NICK_NAME, "abc", "用户123", "nick01", "好的昵称12", "a".repeat(15))
        assertNotMatches(RegExps.Name.NICK_NAME, "", "ab", "a".repeat(16), "nick name", "用户_1")
    }

    @Test
    fun text1To30Chars() {
        assertMatches(RegExps.Text.TEXT_1_TO_30_CHARS, "a", "你好", "x".repeat(30), "\t!", " ")
        assertNotMatches(RegExps.Text.TEXT_1_TO_30_CHARS, "", "x".repeat(31))
    }

    @Test
    fun asciiDigitsOnly() {
        assertMatches(RegExps.CharacterSet.ASCII_DIGITS_ONLY, "0", "123", "0000")
        assertNotMatches(RegExps.CharacterSet.ASCII_DIGITS_ONLY, "", "12a", "12 3", "１２３")
    }

    @Test
    fun passwordStrengthDigitsOnly() {
        assertSame(RegExps.CharacterSet.ASCII_DIGITS_ONLY, RegExps.Security.PASSWORD_STRENGTH_DIGITS_ONLY)
        assertMatches(RegExps.Security.PASSWORD_STRENGTH_DIGITS_ONLY, "0", "999", "012")
    }

    @Test
    fun passwordStrengthLettersAndDigits() {
        assertMatches(RegExps.Security.PASSWORD_STRENGTH_LETTERS_AND_DIGITS, "a1", "A0", "123", "Z9z")
        assertNotMatches(RegExps.Security.PASSWORD_STRENGTH_LETTERS_AND_DIGITS, "a-b", "a_1", "a·1", "中1")
    }

    @Test
    fun passwordStrengthWithSymbols() {
        assertMatches(RegExps.Security.PASSWORD_STRENGTH_WITH_SYMBOLS, "Aa1!", "test@123", "P@ss#1")
        assertNotMatches(RegExps.Security.PASSWORD_STRENGTH_WITH_SYMBOLS, "", "a b", "中文")
    }

    @Test
    fun scoreOrHandicapText() {
        assertMatches(
            RegExps.Numeric.SCORE_OR_HANDICAP_TEXT,
            "1",
            "1/2",
            "3.5/4.25",
            "-1/2",
            "0/0",
            "10-5",
        )
        assertNotMatches(RegExps.Numeric.SCORE_OR_HANDICAP_TEXT, "", "a/b", "1//2", "一/二")
    }

    @Test
    fun positiveDecimalText() {
        assertMatches(RegExps.Numeric.POSITIVE_DECIMAL_TEXT, "1", "1.5", "0.5", "10", "100.00")
        assertNotMatches(RegExps.Numeric.POSITIVE_DECIMAL_TEXT, "", "01", "-1", "0")
    }

    @Test
    fun positiveNumberText() {
        assertMatches(RegExps.Numeric.POSITIVE_NUMBER_TEXT, "1", "1.5", "0.5", "0.000001", "999")
        assertNotMatches(RegExps.Numeric.POSITIVE_NUMBER_TEXT, "", "0", "-1", "0.0", "00.5")
    }

    @Test
    fun signedIntegerText() {
        assertMatches(RegExps.Numeric.SIGNED_INTEGER_TEXT, "0", "-1", "+42", "100", "-0")
        assertNotMatches(RegExps.Numeric.SIGNED_INTEGER_TEXT, "", "12.3", "1a", "--1", "+-2", "＋１００")
    }

    @Test
    fun bankCardNumber() {
        assertMatches(RegExps.Business.BANK_CARD_NUMBER, "1234567890", "1".repeat(25), "0".repeat(10))
        assertNotMatches(
            RegExps.Business.BANK_CARD_NUMBER,
            "",
            "123456789",
            "1".repeat(26),
            "12a4567890123456",
            "1234 5678",
            "１２３４５６７８９０１２３４５６７８９０",
        )
    }

    @Test
    fun btcAmountText() {
        assertMatches(RegExps.Numeric.BTC_AMOUNT_TEXT, "", "1", "0.1", "1.23456789", "0.00001")
        assertNotMatches(RegExps.Numeric.BTC_AMOUNT_TEXT, "0", "0.0", "0.000000")
    }

    @Test
    fun siteIdsCommaSeparated() {
        assertMatches(RegExps.Business.SITE_IDS_COMMA_SEPARATED, "", " ", "1", "1,2,3", "  \t  ")
        assertNotMatches(
            RegExps.Business.SITE_IDS_COMMA_SEPARATED,
            "1,",
            ",1",
            "1,,2",
            "1,2,",
            "１",
            "１，２，３",
        )
    }

    @Test
    fun emptyOrPositiveIntText() {
        assertMatches(RegExps.Numeric.EMPTY_OR_POSITIVE_INT_TEXT, "", "1", "100", "01", "0099")
        assertNotMatches(RegExps.Numeric.EMPTY_OR_POSITIVE_INT_TEXT, "0", "a", "00", "-1", "１２")
    }

    @Test
    fun digitsAtMost9() {
        assertMatches(RegExps.Numeric.DIGITS_AT_MOST_9, "", "0", "123456789")
        assertNotMatches(RegExps.Numeric.DIGITS_AT_MOST_9, "1234567890", "12a", " 0", "１２３")
    }

    @Test
    fun signedAmountLoose() {
        assertMatches(RegExps.Numeric.SIGNED_AMOUNT_LOOSE, "0", "1", "-1", "1.2", "-0.5", "1234567890", "-0.99")
        assertNotMatches(RegExps.Numeric.SIGNED_AMOUNT_LOOSE, "", "1.234", "00", "-00.5", "１２.３４")
    }

    @Test
    fun wechatId() {
        assertMatches(RegExps.Communication.WECHAT_ID, "a12345", "wx_user-name", "Z99999", "a-----b")
        assertNotMatches(RegExps.Communication.WECHAT_ID, "", "1", "a".repeat(25), "abcde", "_badwx", "微信abc")
    }

    @Test
    fun gamePlayerAccount() {
        assertMatches(RegExps.Business.GAME_PLAYER_ACCOUNT, "user1", "a123", "\$guest", "Ab12", "x".repeat(15), "1user")
        assertNotMatches(RegExps.Business.GAME_PLAYER_ACCOUNT, "", "ab", "a".repeat(20), "u\$er", "玩家1234")
    }

    @Test
    fun uuidHyphenated() {
        assertMatches(RegExps.Format.UUID_HYPHENATED, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertNotMatches(
            RegExps.Format.UUID_HYPHENATED,
            "",
            "not-a-uuid",
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee",
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeeee",
        )
    }

    @Test
    fun domainListCommaSeparated() {
        assertMatches(RegExps.Network.DOMAIN_LIST_COMMA_SEPARATED, "a.com", "a.com,b.com", "sub.a.com")
        assertNotMatches(
            RegExps.Network.DOMAIN_LIST_COMMA_SEPARATED,
            "",
            "a.com,",
            ",a.com",
            "a..com",
            // 标签仅 [a-zA-Z0-9]+：无连字符，Punycode 与 Unicode 域名均不匹配当前实现
            "例子.cn",
            "xn--fiqs8s.cn",
        )
    }

    @Test
    fun amountNonzeroTwoDecimals() {
        assertMatches(RegExps.Numeric.AMOUNT_NONZERO_TWO_DECIMALS, "1", "0.01", "10.99", "999")
        assertNotMatches(RegExps.Numeric.AMOUNT_NONZERO_TWO_DECIMALS, "", "0", "0.0", "1.234", "00.01")
    }

    @Test
    fun textStartsWithDigit() {
        assertMatches(RegExps.Text.TEXT_STARTS_WITH_DIGIT, "0a", "9xyz", "0", "1中文")
        assertNotMatches(RegExps.Text.TEXT_STARTS_WITH_DIGIT, "", "a1", "\t0")
    }

    @Test
    fun digitsOnlyOptionalEmpty() {
        assertMatches(RegExps.Text.DIGITS_ONLY_OPTIONAL_EMPTY, "", "0", "123", "000")
        assertNotMatches(RegExps.Text.DIGITS_ONLY_OPTIONAL_EMPTY, "12a", " 1", "-1", "１２")
    }

    @Test
    fun hanLatinAlnum() {
        assertMatches(RegExps.Text.HAN_LATIN_ALNUM, "abc", "中文", "a1中", "汉字ABC123")
        assertNotMatches(RegExps.Text.HAN_LATIN_ALNUM, "", "a b", "a-b", "a_1")
    }

    @Test
    fun dateIsoYyyyMmDd() {
        assertMatches(RegExps.Format.DATE_ISO_YYYY_MM_DD, "2020-01-01", "1999-12-31", "2024-02-29")
        assertNotMatches(
            RegExps.Format.DATE_ISO_YYYY_MM_DD,
            "",
            "2020-13-01",
            "20-01-01",
            "2020-1-01",
            "2020-00-01",
        )
    }

    @Test
    fun time24hMmOptionalSs() {
        assertMatches(RegExps.Format.TIME_24H_MM_OPTIONAL_SS, "00:00", "23:59", "12:30:45", "09:05:00")
        assertNotMatches(RegExps.Format.TIME_24H_MM_OPTIONAL_SS, "", "24:00", "1:30", "12:60", "12:30:")
    }

    @Test
    fun cnMainlandPostalCode() {
        assertMatches(RegExps.Format.CN_MAINLAND_POSTAL_CODE, "100000", "518000", "000000")
        assertNotMatches(RegExps.Format.CN_MAINLAND_POSTAL_CODE, "", "12345", "1234567", "abcdee", " 100000", "１０００００")
    }

    @Test
    fun hexColorCss() {
        assertMatches(RegExps.Format.HEX_COLOR_CSS, "#fff", "#ffffff", "#ffffffff", "#aBc", "#0F0F0F")
        assertNotMatches(RegExps.Format.HEX_COLOR_CSS, "", "#gggggg", "ffffff", "#ff", "#fffffff")
    }

    @Test
    fun macAddressColonOrHyphen() {
        assertMatches(
            RegExps.Network.MAC_ADDRESS_COLON_OR_HYPHEN,
            "00:11:22:33:44:55",
            "00-11-22-33-44-55",
            "ff:FF:aa:bb:cc:dd",
        )
        assertNotMatches(RegExps.Network.MAC_ADDRESS_COLON_OR_HYPHEN, "", "00:11:22:33:44", "gg:gg:gg:gg:gg:gg")
    }

    @Test
    fun networkPort1_65535() {
        assertMatches(RegExps.Network.NETWORK_PORT_1_65535, "1", "80", "65535", "8080", "443", "1024")
        assertNotMatches(RegExps.Network.NETWORK_PORT_1_65535, "", "0", "65536", "abc", "080")
    }

    @Test
    fun slugKebabLowercase() {
        assertMatches(RegExps.CharacterSet.SLUG_KEBAB_LOWERCASE, "a", "a-b", "ab-cd-ef", "api-v2")
        assertNotMatches(RegExps.CharacterSet.SLUG_KEBAB_LOWERCASE, "", "-a", "a-", "A-b", "a_b", "a--b", "slug-中文")
    }

    @Test
    fun latinAlnumOnly() {
        assertMatches(RegExps.CharacterSet.LATIN_ALNUM_ONLY, "a", "A1", "09", "Test01")
        assertNotMatches(RegExps.CharacterSet.LATIN_ALNUM_ONLY, "", "a-b", "a ", "a_1", "中文")
    }

    @Test
    fun ipv4CidrNotation() {
        assertMatches(RegExps.Network.IPV4_CIDR_NOTATION, "0.0.0.0/0", "192.168.0.0/24", "10.0.0.0/32", "172.16.0.0/16")
        assertNotMatches(RegExps.Network.IPV4_CIDR_NOTATION, "", "192.168.0.0/33", "256.0.0.0/24", "192.168.0.0")
    }

    @Test
    fun percentInteger0_100() {
        assertMatches(RegExps.Numeric.PERCENT_INTEGER_0_100, "0", "50", "100", "9", "99", "1")
        assertNotMatches(RegExps.Numeric.PERCENT_INTEGER_0_100, "", "101", "-1", "1.5", "01", "５０")
    }

    @Test
    fun latinAlnumDashUnderscore() {
        assertMatches(RegExps.CharacterSet.LATIN_ALNUM_DASH_UNDERSCORE, "a", "a-b", "a_b", "A1", "x-y_z")
        assertNotMatches(RegExps.CharacterSet.LATIN_ALNUM_DASH_UNDERSCORE, "", "a b", "a@b", "a·b", "用户_a")
    }

    @Test
    fun relaxedVarName() {
        assertMatches(
            RegExps.CharacterSet.RELAXED_VAR_NAME,
            "a",
            "_x",
            "Abc_123-x",
            "user-name",
            "_user-name",
        )
        assertNotMatches(
            RegExps.CharacterSet.RELAXED_VAR_NAME,
            "",
            "1abc",
            "-abc",
            "a b",
            "a@b",
            "变量名",
        )
    }

    @Test
    fun varName() {
        assertMatches(RegExps.CharacterSet.VAR_NAME, "a", "_x", "a1", "Ab_c", "__init")
        assertNotMatches(RegExps.CharacterSet.VAR_NAME, "", "1a", "a-b", "a.b", "9_", "变量名")
    }

    @Test
    fun jdbcUrl() {
        assertMatches(
            RegExps.Network.JDBC_URL,
            // 单实例
            "jdbc:mysql://localhost:3306/db",
            "jdbc:h2:mem:testdb",
            "jdbc:postgresql://pg.example.com:5432/myapp",
            // MySQL 多主机（复制 / Router / 集群常用写法：逗号分隔 host:port）
            "jdbc:mysql://db1.example.com:3306,db2.example.com:3306,db3.example.com:3306/order_db",
            "jdbc:mysql://10.0.0.11:3306,10.0.0.12:3306/inventory?useSSL=false&failOverReadOnly=false",
            // PostgreSQL 多主机（驱动支持列表或故障转移）
            "jdbc:postgresql://primary.db:5432,standby.db:5432/analytics?targetServerType=primary",
            // Oracle RAC / 多地址（thin 常含括号与逗号，整体仍为单 JDBC URl 串）
            "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(HOST=rac1)(PORT=1521))(ADDRESS=(HOST=rac2)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=orcl)))",
            // H2 / 其他基于 TCP 的多节点示例
            "jdbc:h2:tcp://node-a:9092,node-b:9092/~/cluster-db;MODE=MySQL",
            // \\S+ 段可含非 ASCII（如库名中文）
            "jdbc:mysql://localhost:3306/用户库",
        )
        assertNotMatches(
            RegExps.Network.JDBC_URL,
            "",
            "jdbc:",
            "mysql://localhost",
            // 含空白则 \S+ 无法整段匹配
            "jdbc:mysql:// host1:3306,host2:3306/db",
        )
    }

    @Test
    fun domain() {
        assertMatches(
            RegExps.Network.DOMAIN,
            "localhost",
            "example.com",
            "sub.example.co.uk",
            // Punycode 标签可含连字符，单域名正则支持
            "xn--fiqs8s.cn",
        )
        assertNotMatches(
            RegExps.Network.DOMAIN,
            "",
            "-bad.com",
            "example",
            "..com",
            // 明文 IDN（中文标签）不匹配 ASCII 域名段
            "例子.cn",
        )
    }
}
