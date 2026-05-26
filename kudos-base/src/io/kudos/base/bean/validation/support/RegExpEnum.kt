package io.kudos.base.bean.validation.support

/** Unified prefix for the i18n keys of the default validation-failure messages. */
private const val REG_EXP_DEFAULT_MSG_PREFIX = "sys.valid-msg.default.Pattern::"

/**
 * Concatenates a short suffix (e.g. `cn-mainland-mobile`) into a full i18n key
 * (`sys.valid-msg.default.Pattern::cn-mainland-mobile`).
 * Used by the `defaultMessageKey` field of each enum entry to avoid repeating the prefix on every line.
 *
 * @param suffix the short suffix customized by each entry
 * @return the full i18n key
 * @author K
 * @since 1.0.0
 */
private fun regExpMsg(suffix: String) = REG_EXP_DEFAULT_MSG_PREFIX + suffix

/**
 * Built-in categorized regexes corresponding one-to-one with [RegExps], for use by
 * [io.kudos.base.bean.validation.constraint.annotations.Matches].
 * For business-specific rules, use [@Pattern][jakarta.validation.constraints.Pattern] and reference the constants in [RegExps].
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
enum class RegExpEnum(
    val regex: String,
    val defaultMessageKey: String,
) {

    //region Communication
    /**
     * Rules:
     * 11-digit mainland China mobile number
     * Starts with `1`
     * The second and third digits must fall within the prefixes listed in this rule
     *
     * Examples:
     * `13800138000`
     * `15912345678`
     * `19912345678`
     */
    CN_MAINLAND_MOBILE(RegExps.Communication.CN_MAINLAND_MOBILE, regExpMsg("cn-mainland-mobile")),

    /**
     * Rules:
     * QQ number
     * Digits only
     * Length 5 to 11
     *
     * Examples:
     * `12345`
     * `10000`
     * `12345678901`
     */
    QQ_NUMBER(RegExps.Communication.QQ_NUMBER, regExpMsg("qq-number")),

    /**
     * Rules:
     * Pure-digit string for mobile or landline phone
     * Does not distinguish between mobile, landline or extension
     * Only validates digits and length 7 to 20
     *
     * Examples:
     * `1234567`
     * `02012345678`
     * `12345678901234567890`
     */
    PHONE_DIGITS_7_20(RegExps.Communication.PHONE_DIGITS_7_20, regExpMsg("phone-digits-7-20")),

    /**
     * Rules:
     * Domestic landline or mobile number
     * Landline supports area code, hyphens and optional extension
     * Mobile number is matched against the prefixes listed in the rule
     *
     * Examples:
     * `010-12345678`
     * `010-12345678-123`
     * `13800138000`
     */
    TEL_OR_CN_MOBILE(RegExps.Communication.TEL_OR_CN_MOBILE, regExpMsg("tel-or-cn-mobile")),

    /**
     * Rules:
     * Email address
     * Local part supports letters, digits and `.`, `_`, `-`
     * Domain part loosely matches the common email domain forms
     *
     * Examples:
     * `user@example.com`
     * `user.name@example.com`
     * `abc-123@test-mail.com`
     */
    EMAIL(RegExps.Communication.EMAIL, regExpMsg("email")),

    /**
     * Rules:
     * Email address or mainland China mobile number
     * Either one is sufficient
     *
     * Examples:
     * `user@example.com`
     * `abc_01@test.com`
     * `13800138000`
     */
    MAIL_OR_CN_MOBILE(RegExps.Communication.MAIL_OR_CN_MOBILE, regExpMsg("mail-or-cn-mobile")),

    /**
     * Rules:
     * Domestic landline phone
     * Supports 2 or 3 digit area codes
     * Supports main number and extension joined with `-`
     *
     * Examples:
     * `010-12345678`
     * `021-1234567`
     * `010-12345678-123`
     */
    CN_LANDLINE_PHONE(RegExps.Communication.CN_LANDLINE_PHONE, regExpMsg("cn-landline-phone")),

    /**
     * Rules:
     * WeChat ID
     * First character must be a letter or digit
     * Subsequent characters may include letters, digits, `-`, `_`
     * Total length 6 to 20
     *
     * Examples:
     * `wechat1`
     * `wx_123456`
     * `abc-def_01`
     */
    WECHAT_ID(RegExps.Communication.WECHAT_ID, regExpMsg("wechat-id")),
    //endregion

    //region Name
    /**
     * Rules:
     * Short personal name
     * Allows Han characters, Latin letters and `·`
     * Length 2 to 30
     *
     * Examples:
     * `Zhang San`
     * `Wang Xiaoming`
     * `John·Doe`
     */
    SHORT_PERSON_NAME(RegExps.Name.SHORT_PERSON_NAME, regExpMsg("short-person-name")),

    /**
     * Rules:
     * Real personal name
     * First and last characters must be letters, Han characters or other allowed name characters
     * Middle may contain digits, spaces, `·`, `.`, `*`
     * Cannot be all digits
     *
     * Examples:
     * `Zhang San`
     * `John Smith`
     * `Li Si`
     */
    REAL_PERSON_NAME(RegExps.Name.REAL_PERSON_NAME, regExpMsg("real-person-name")),

    /**
     * Rules:
     * Bank account holder name
     * Allows Chinese and English characters, digits, spaces, `·`, `.`, parentheses, `*`
     * Suitable for displaying personal or company names
     *
     * Examples:
     * `Zhang San`
     * `Wang Wu (Company)`
     * `ABC Trading Co.`
     */
    BANK_ACCOUNT_HOLDER_NAME(RegExps.Name.BANK_ACCOUNT_HOLDER_NAME, regExpMsg("bank-account-holder-name")),

    /**
     * Rules:
     * Payer (depositor) display name
     * First and last characters must be Chinese or English letters
     * Middle allows spaces, `.`, `·`
     *
     * Examples:
     * `Zhang San`
     * `Jane Doe`
     * `Li Lei`
     */
    PAYER_DISPLAY_NAME(RegExps.Name.PAYER_DISPLAY_NAME, regExpMsg("payer-display-name")),

    /**
     * Rules:
     * Nickname
     * Allows Chinese, English and digits
     * Length 3 to 15
     *
     * Examples:
     * `Xiaoming007`
     * `Alice88`
     * `Player123`
     */
    NICK_NAME(RegExps.Name.NICK_NAME, regExpMsg("nick-name")),
    //endregion

    //region Text
    /**
     * Rules:
     * Text must not contain `&`, `*`, `=`, `|`, `{}`, `<>`, `/`, `…`, `—`
     * Any ordinary characters not listed are not restricted by this rule
     *
     * Examples:
     * `hello world`
     * `note info`
     * `A_B-C.1`
     */
    TEXT_WITHOUT_SPECIAL_CHARS(RegExps.Text.TEXT_WITHOUT_SPECIAL_CHARS, regExpMsg("text-without-special-chars")),

    /**
     * Rules:
     * Arbitrary characters
     * Length 1 to 30
     * Commonly used for security-question answers and short free-text fields
     *
     * Examples:
     * `a`
     * `my answer`
     * `answer123`
     */
    TEXT_1_TO_30_CHARS(RegExps.Text.TEXT_1_TO_30_CHARS, regExpMsg("text-1-to-30-chars")),

    /**
     * Rules:
     * First character must be a digit
     * Subsequent characters are unrestricted by this rule
     *
     * Examples:
     * `1`
     * `1abc`
     * `9-xyz`
     */
    TEXT_STARTS_WITH_DIGIT(RegExps.Text.TEXT_STARTS_WITH_DIGIT, regExpMsg("text-starts-with-digit")),

    /**
     * Rules:
     * Digits only
     * Empty string allowed
     * Commonly used for optional "digits-only" fields
     *
     * Examples:
     * ``
     * `12345`
     * `0001`
     */
    DIGITS_ONLY_OPTIONAL_EMPTY(RegExps.Text.DIGITS_ONLY_OPTIONAL_EMPTY, regExpMsg("digits-only-optional-empty")),

    /**
     * Rules:
     * Only Han characters, Latin letters and digits
     * Spaces and symbols not allowed
     *
     * Examples:
     * `abc123`
     * `Zhongguo123`
     * `KudosZhongguo123`
     */
    HAN_LATIN_ALNUM(RegExps.Text.HAN_LATIN_ALNUM, regExpMsg("han-latin-alnum")),

    /**
     * Context path
     *
     * Rules:
     * Must start with `/`
     * Only lowercase letters, digits and hyphens `-`
     * Multi-level paths are separated by `/`
     * Each segment cannot be empty
     * No spaces, Chinese characters, underscores or consecutive slashes
     * Do not stuff query- or fragment-style content into the path
     *
     * Examples:
     * `/`
     * `/api`
     * `/user-center`
     * `/order/list`
     * `/v1/system-config`
     */
    CONTEXT(RegExps.Text.CONTEXT, regExpMsg("context")),
    //endregion

    //region CharacterSet
    /**
     * Rules:
     * The whole string is the same character repeated
     * Repeated at least twice
     *
     * Examples:
     * `aaa`
     * `1111`
     * `%%%%`
     */
    SINGLE_CHAR_REPEATED(RegExps.CharacterSet.SINGLE_CHAR_REPEATED, regExpMsg("single-char-repeated")),

    /**
     * Rules:
     * At least one digit
     * Digits only
     *
     * Examples:
     * `0`
     * `123`
     * `987654321`
     */
    DIGITS_NON_EMPTY(RegExps.CharacterSet.DIGITS_NON_EMPTY, regExpMsg("digits-non-empty")),

    /**
     * Rules:
     * Latin letters only
     * Both upper and lower case allowed
     * Digits and symbols not allowed
     *
     * Examples:
     * `abc`
     * `ABC`
     * `AbCd`
     */
    LATIN_LETTERS_ONLY(RegExps.CharacterSet.LATIN_LETTERS_ONLY, regExpMsg("latin-letters-only")),

    /**
     * Rules:
     * Lowercase Latin letters only
     * Uppercase letters, digits and symbols not allowed
     *
     * Examples:
     * `abc`
     * `kudos`
     * `username`
     */
    LATIN_LOWERCASE_ONLY(RegExps.CharacterSet.LATIN_LOWERCASE_ONLY, regExpMsg("latin-lowercase-only")),

    /**
     * Rules:
     * Lowercase letters and digits only
     * Cannot be all digits
     * Must contain at least one lowercase letter
     *
     * Examples:
     * `abc123`
     * `a1`
     * `user2026`
     */
    LOWERCASE_ALNUM_NOT_ALL_DIGITS(RegExps.CharacterSet.LOWERCASE_ALNUM_NOT_ALL_DIGITS, regExpMsg("lowercase-alnum-not-all-digits")),

    /**
     * Rules:
     * ASCII digits only
     * At least one digit
     * Full-width digits not accepted
     *
     * Examples:
     * `0`
     * `123456`
     * `987654321`
     */
    ASCII_DIGITS_ONLY(RegExps.CharacterSet.ASCII_DIGITS_ONLY, regExpMsg("ascii-digits-only")),

    /**
     * Rules:
     * Latin letters and digits only
     * Spaces, underscores, hyphens and other symbols not allowed
     *
     * Examples:
     * `abc`
     * `ABC123`
     * `Abc123`
     */
    LATIN_ALNUM_ONLY(RegExps.CharacterSet.LATIN_ALNUM_ONLY, regExpMsg("latin-alnum-only")),

    /**
     * Rules:
     * Latin letters, digits, hyphens and underscores only
     * At least 1 character
     *
     * Examples:
     * `user`
     * `user_name`
     * `user-name_01`
     */
    LATIN_ALNUM_DASH_UNDERSCORE(RegExps.CharacterSet.LATIN_ALNUM_DASH_UNDERSCORE, regExpMsg("latin-alnum-dash-underscore")),

    /**
     * Rules:
     * Relaxed identifier / variable name
     * Allows upper and lower case letters, digits, underscores and hyphens
     * First character may only be a letter or underscore
     * Subsequent characters allow letters, digits, underscores and hyphens
     *
     * Examples:
     * `name`
     * `_user-name`
     * `Abc_123-x`
     */
    RELAXED_VAR_NAME(
        RegExps.CharacterSet.RELAXED_VAR_NAME,
        regExpMsg("relaxed-var-name"),
    ),

    /**
     * Rules:
     * Identifier
     * First character must be a letter or underscore
     * Subsequent characters allow letters, digits and underscores
     *
     * Examples:
     * `name`
     * `_value`
     * `user_name1`
     */
    VAR_NAME(RegExps.CharacterSet.VAR_NAME, regExpMsg("var-name")),

    /**
     * Rules:
     * Lowercase slug
     * Only lowercase letters, digits and hyphens allowed
     * Consecutive hyphens producing empty segments are not allowed
     *
     * Examples:
     * `article`
     * `user-profile`
     * `user-profile-01`
     */
    SLUG_KEBAB_LOWERCASE(RegExps.CharacterSet.SLUG_KEBAB_LOWERCASE, regExpMsg("slug-kebab-lowercase")),
    //endregion

    //region Security
    /**
     * Rules:
     * Login password
     * Length 6 to 20
     * Allows letters, digits and common ASCII symbols
     *
     * Examples:
     * `Abc123`
     * `Abc123!`
     * `Pass_word#1`
     */
    LOGIN_PASSWORD(RegExps.Security.LOGIN_PASSWORD, regExpMsg("login-password")),

    /**
     * Rules:
     * 6-digit security code / PIN
     * Digits only
     * Fixed length of 6
     *
     * Examples:
     * `123456`
     * `000001`
     * `987654`
     */
    SECURITY_PIN_SIX_DIGITS(RegExps.Security.SECURITY_PIN_SIX_DIGITS, regExpMsg("security-pin-six-digits")),

    /**
     * Rules:
     * "Letters only" category used in password strength classification
     * Identical to [LATIN_LETTERS_ONLY]
     *
     * Examples:
     * `abc`
     * `OnlyText`
     * `Password`
     */
    PASSWORD_STRENGTH_LETTERS_ONLY(RegExps.Security.PASSWORD_STRENGTH_LETTERS_ONLY, regExpMsg("password-strength-letters-only")),

    /**
     * Rules:
     * "Digits only" category used in password strength classification
     * Identical to [ASCII_DIGITS_ONLY]
     *
     * Examples:
     * `123456`
     * `987654`
     * `20260403`
     */
    PASSWORD_STRENGTH_DIGITS_ONLY(RegExps.Security.PASSWORD_STRENGTH_DIGITS_ONLY, regExpMsg("password-strength-digits-only")),

    /**
     * Rules:
     * "Letters and digits" category used in password strength classification
     * Allows upper and lower case letters and digits
     * Symbols not allowed
     *
     * Examples:
     * `Abc123`
     * `abc999`
     * `PASS2026`
     */
    PASSWORD_STRENGTH_LETTERS_AND_DIGITS(RegExps.Security.PASSWORD_STRENGTH_LETTERS_AND_DIGITS, regExpMsg("password-strength-letters-and-digits")),

    /**
     * Rules:
     * "Letters, digits and common symbols" category used in password strength classification
     * Allows upper and lower case letters, digits and common ASCII symbols
     *
     * Examples:
     * `Abc123!`
     * `P@ssw0rd#`
     * `A1_b2-C3`
     */
    PASSWORD_STRENGTH_WITH_SYMBOLS(RegExps.Security.PASSWORD_STRENGTH_WITH_SYMBOLS, regExpMsg("password-strength-with-symbols")),
    //endregion

    //region Numeric
    /**
     * Rules:
     * Positive integer text
     * Negative sign and decimal point not allowed
     * Cannot start with `0`
     *
     * Examples:
     * `1`
     * `42`
     * `999999`
     */
    POSITIVE_INT_TEXT(RegExps.Numeric.POSITIVE_INT_TEXT, regExpMsg("positive-int-text")),

    /**
     * Rules:
     * Handicap- or score-style string
     * Supports an optional negative sign
     * Supports integers, decimals and `/` or `-` as separators
     *
     * Examples:
     * `1.5`
     * `1.5/2`
     * `-0.5`
     */
    SCORE_OR_HANDICAP_TEXT(RegExps.Numeric.SCORE_OR_HANDICAP_TEXT, regExpMsg("score-or-handicap-text")),

    /**
     * Rules:
     * Positive number text
     * Allows integers or decimals
     * Negative sign not allowed
     * Redundant leading zeros not allowed
     *
     * Examples:
     * `1`
     * `12`
     * `12.5`
     */
    POSITIVE_DECIMAL_TEXT(RegExps.Numeric.POSITIVE_DECIMAL_TEXT, regExpMsg("positive-decimal-text")),

    /**
     * Rules:
     * Positive number
     * Can be an integer greater than 0
     * Or `0.xxx` where the fractional part contains at least one non-zero digit
     *
     * Examples:
     * `8`
     * `10.5`
     * `0.25`
     */
    POSITIVE_NUMBER_TEXT(RegExps.Numeric.POSITIVE_NUMBER_TEXT, regExpMsg("positive-number-text")),

    /**
     * Rules:
     * Integer text
     * May carry `+` or `-`
     * Decimal point not allowed
     *
     * Examples:
     * `0`
     * `-10`
     * `+8`
     */
    SIGNED_INTEGER_TEXT(RegExps.Numeric.SIGNED_INTEGER_TEXT, regExpMsg("signed-integer-text")),

    /**
     * Rules:
     * Bitcoin amount
     * All zeros not allowed
     * Integer part may be a positive integer
     * Fractional part up to 8 digits
     *
     * Examples:
     * `1`
     * `0.12345`
     * `1.00000001`
     */
    BTC_AMOUNT_TEXT(RegExps.Numeric.BTC_AMOUNT_TEXT, regExpMsg("btc-amount-text")),

    /**
     * Rules:
     * Empty string or positive integer
     * If a value is present, it must be an integer greater than 0
     *
     * Examples:
     * ``
     * `1`
     * `25`
     */
    EMPTY_OR_POSITIVE_INT_TEXT(RegExps.Numeric.EMPTY_OR_POSITIVE_INT_TEXT, regExpMsg("empty-or-positive-int-text")),

    /**
     * Rules:
     * Up to 9 digits
     * May be empty
     * No other characters allowed
     *
     * Examples:
     * ``
     * `1`
     * `123456789`
     */
    DIGITS_AT_MOST_9(RegExps.Numeric.DIGITS_AT_MOST_9, regExpMsg("digits-at-most-9")),

    /**
     * Rules:
     * Amount
     * May carry a negative sign
     * Integer part is `0` or starts with a non-zero digit
     * Fractional part up to 2 digits
     *
     * Examples:
     * `0`
     * `12.5`
     * `-12.50`
     */
    SIGNED_AMOUNT_LOOSE(RegExps.Numeric.SIGNED_AMOUNT_LOOSE, regExpMsg("signed-amount-loose")),

    /**
     * Rules:
     * Non-zero amount
     * Amount cannot be `0` or `0.00`
     * Fractional part up to 2 digits
     *
     * Examples:
     * `0.01`
     * `1`
     * `99.99`
     */
    AMOUNT_NONZERO_TWO_DECIMALS(RegExps.Numeric.AMOUNT_NONZERO_TWO_DECIMALS, regExpMsg("amount-nonzero-two-decimals")),

    /**
     * Rules:
     * Integer percentage
     * Integers only
     * Value range 0 to 100
     *
     * Examples:
     * `0`
     * `85`
     * `100`
     */
    PERCENT_INTEGER_0_100(RegExps.Numeric.PERCENT_INTEGER_0_100, regExpMsg("percent-integer-0-100")),
    //endregion

    //region Network
    /**
     * Rules:
     * IPv4 dotted-decimal address
     * 4 octets in total
     * Each octet ranges from 0 to 255
     *
     * Examples:
     * `127.0.0.1`
     * `192.168.1.1`
     * `255.255.255.255`
     */
    IPV4(RegExps.Network.IPV4, regExpMsg("ipv4")),

    /**
     * Rules:
     * IPv4 dotted-decimal full (zero-padded) address
     * 4 octets in total
     * Each octet ranges from 000 to 255
     *
     * Examples:
     * `127.000.000.001`
     * `192.168.001.001`
     * `255.255.255.255`
     */
    IPV4_FULL(RegExps.Network.IPV4_FULL, regExpMsg("ipv4-full")),

    /**
     * Rules:
     * URL, supporting `http`, `https`, `ftp`, `sftp`
     * Allows domain, IPv4, port, path, query and fragment
     * Loose match; not equivalent to full strict RFC validation
     *
     * Examples:
     * `https://example.com`
     * `http://example.com:8080/api`
     * `https://example.com/search?q=test#top`
     */
    HTTP_URL(RegExps.Network.HTTP_URL, regExpMsg("http-url")),

    /**
     * Rules:
     * Multiple IPv4 addresses
     * Separated by ASCII semicolon `;`
     * Every member must be a valid IPv4 address
     *
     * Examples:
     * `10.0.0.1`
     * `10.0.0.1;192.168.1.1`
     * `127.0.0.1;8.8.8.8;1.1.1.1`
     */
    IPV4_SEMICOLON_LIST(RegExps.Network.IPV4_SEMICOLON_LIST, regExpMsg("ipv4-semicolon-list")),

    /**
     * Rules:
     * Fully expanded IPv6 address
     * 8 hexadecimal groups in total
     * Each group has 1 to 4 hexadecimal characters
     *
     * Examples:
     * `2001:0db8:0000:0000:0000:ff00:0042:8329`
     * `fe80:0000:0000:0000:0202:b3ff:fe1e:8329`
     */
    IPV6_FULL(RegExps.Network.IPV6_FULL, regExpMsg("ipv6-full")),

    /**
     * Rules:
     * Compressed IPv6 address
     * Must contain `::`
     * Either side of `::` may omit groups
     *
     * Examples:
     * `2001:db8::1`
     * `::1`
     * `fe80::abcd`
     */
    IPV6_COMPACT(RegExps.Network.IPV6_COMPACT, regExpMsg("ipv6-compact")),

    /**
     * Rules:
     * Comma-separated list of domain names
     * Every member must be a standard dotted domain name
     * Separated by ASCII comma `,`
     *
     * Examples:
     * `a.example.com`
     * `a.example.com,b.example.com`
     * `api.test.com,cdn.test.com`
     */
    DOMAIN_LIST_COMMA_SEPARATED(RegExps.Network.DOMAIN_LIST_COMMA_SEPARATED, regExpMsg("domain-list-comma-separated")),

    /**
     * Rules:
     * MAC address
     * 6 groups of hexadecimal characters
     * Groups may be separated by `:` or `-`
     *
     * Examples:
     * `00:1A:2B:3C:4D:5E`
     * `AA-BB-CC-DD-EE-FF`
     */
    MAC_ADDRESS_COLON_OR_HYPHEN(RegExps.Network.MAC_ADDRESS_COLON_OR_HYPHEN, regExpMsg("mac-address-colon-or-hyphen")),

    /**
     * Rules:
     * Network port number
     * Decimal digits only
     * Value range 1 to 65535
     *
     * Examples:
     * `80`
     * `443`
     * `65535`
     */
    NETWORK_PORT_1_65535(RegExps.Network.NETWORK_PORT_1_65535, regExpMsg("network-port-1-65535")),

    /**
     * Rules:
     * IPv4 CIDR notation
     * Leading part must be a valid IPv4 address
     * Prefix length ranges from 0 to 32
     *
     * Examples:
     * `192.168.1.0/24`
     * `10.0.0.1/32`
     * `0.0.0.0/0`
     */
    IPV4_CIDR_NOTATION(RegExps.Network.IPV4_CIDR_NOTATION, regExpMsg("ipv4-cidr-notation")),

    /**
     * Rules:
     * JDBC URL
     * Must start with `jdbc:`
     * The protocol name may be followed by a driver type and connection information
     * No whitespace characters allowed in the remainder
     *
     * Examples:
     * `jdbc:mysql://localhost:3306/kudos`
     * `jdbc:postgresql://db:5432/app`
     * `jdbc:h2:mem:test`
     */
    JDBC_URL(RegExps.Network.JDBC_URL, regExpMsg("jdbc-url")),

    /**
     * Rules:
     * Domain name
     * Supports `localhost`
     * Or a standard dotted domain name
     * Total length and per-label leading/trailing hyphen rules are constrained
     *
     * Examples:
     * `localhost`
     * `example.com`
     * `api.example.com`
     */
    DOMAIN(RegExps.Network.DOMAIN, regExpMsg("domain")),
    //endregion

    //region Business
    /**
     * Rules:
     * Bank card number
     * Digits only
     * Length 10 to 25
     *
     * Examples:
     * `6222021234567890`
     * `6222021234567890123`
     * `1234567890123456789012345`
     */
    BANK_CARD_NUMBER(RegExps.Business.BANK_CARD_NUMBER, regExpMsg("bank-card-number")),

    /**
     * Rules:
     * Site ID list
     * Supports a single number
     * Supports multiple numbers separated by ASCII commas
     * Also allows an all-whitespace value
     *
     * Examples:
     * `42`
     * `1,2,3`
     * `   `
     */
    SITE_IDS_COMMA_SEPARATED(RegExps.Business.SITE_IDS_COMMA_SEPARATED, regExpMsg("site-ids-comma-separated")),

    /**
     * Rules:
     * Game player account
     * First character may be a letter, digit, underscore or `$`
     * Subsequent characters allow only letters, digits and underscores
     * Total length 4 to 15
     *
     * Examples:
     * `user1`
     * `guest_01`
     * `$abc_123`
     */
    GAME_PLAYER_ACCOUNT(RegExps.Business.GAME_PLAYER_ACCOUNT, regExpMsg("game-player-account")),
    //endregion

    //region Format
    /**
     * Rules:
     * Hyphenated UUID
     * Shaped as `8-4-4-4-12`
     * Each segment is matched by the alphanumeric/underscore character class `\\w`
     *
     * Examples:
     * `123e4567-e89b-12d3-a456-426614174000`
     * `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee`
     */
    UUID_HYPHENATED(RegExps.Format.UUID_HYPHENATED, regExpMsg("uuid-hyphenated")),

    /**
     * Rules:
     * Date format `yyyy-MM-dd`
     * Only validates the year-month-day format
     * Does not guarantee absolute correctness of leap years or month lengths
     *
     * Examples:
     * `2026-04-03`
     * `1999-12-31`
     * `2024-02-29`
     */
    DATE_ISO_YYYY_MM_DD(RegExps.Format.DATE_ISO_YYYY_MM_DD, regExpMsg("date-iso-yyyy-mm-dd")),

    /**
     * Rules:
     * 24-hour time
     * Base format `HH:mm`
     * Seconds `:ss` may be appended
     *
     * Examples:
     * `09:30`
     * `23:59`
     * `23:59:59`
     */
    TIME_24H_MM_OPTIONAL_SS(RegExps.Format.TIME_24H_MM_OPTIONAL_SS, regExpMsg("time-24h-mm-optional-ss")),

    /**
     * Rules:
     * Mainland China postal code
     * Digits only
     * Fixed length of 6
     *
     * Examples:
     * `100000`
     * `200120`
     * `518000`
     */
    CN_MAINLAND_POSTAL_CODE(RegExps.Format.CN_MAINLAND_POSTAL_CODE, regExpMsg("cn-mainland-postal-code")),

    /**
     * Rules:
     * CSS hexadecimal color value
     * Supports `#RGB`
     * Supports `#RRGGBB`
     * Supports `#RRGGBBAA`
     *
     * Examples:
     * `#fff`
     * `#FF8800`
     * `#11223344`
     */
    HEX_COLOR_CSS(RegExps.Format.HEX_COLOR_CSS, regExpMsg("hex-color-css")),
    //endregion

    /* Same as the email rule (historical MSN); use [EMAIL] in new code. */
    MSN(RegExps.Communication.EMAIL, regExpMsg("msn")),

    ;
}
