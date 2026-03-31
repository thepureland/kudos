package io.kudos.base.bean.validation.support

private const val REG_EXP_DEFAULT_MSG_PREFIX = "sys.valid-msg.default.Pattern::"

private fun regExpMsg(suffix: String) = REG_EXP_DEFAULT_MSG_PREFIX + suffix

/**
 * 与 [RegExps] 一一对应的内置分类正则，供 [io.kudos.base.bean.validation.constraint.annotations.Matches] 使用。
 * 业务自定义规则请使用 [@Pattern][jakarta.validation.constraints.Pattern] 并引用 [RegExps] 中的常量。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
enum class RegExpEnum(
    val regex: String,
    val defaultMessageKey: String,
) {

    /* 中国大陆 11 位手机号 */
    CN_MAINLAND_MOBILE(RegExps.CN_MAINLAND_MOBILE, regExpMsg("cn-mainland-mobile")),

    /* QQ 号 5～11 位数字 */
    QQ_NUMBER(RegExps.QQ_NUMBER, regExpMsg("qq-number")),

    /* 7～20 位数字（手机或电话纯数字串） */
    PHONE_DIGITS_7_20(RegExps.PHONE_DIGITS_7_20, regExpMsg("phone-digits-7-20")),

    /* IPv4 */
    IPV4(RegExps.IPV4, regExpMsg("ipv4")),

    /* HTTP(S)/FTP(S) URL */
    HTTP_URL(RegExps.HTTP_URL, regExpMsg("http-url")),

    /* 国内固话或手机号 */
    TEL_OR_CN_MOBILE(RegExps.TEL_OR_CN_MOBILE, regExpMsg("tel-or-cn-mobile")),

    /* 简短姓名 2～30 */
    SHORT_PERSON_NAME(RegExps.SHORT_PERSON_NAME, regExpMsg("short-person-name")),

    /* 真实姓名 */
    REAL_PERSON_NAME(RegExps.REAL_PERSON_NAME, regExpMsg("real-person-name")),

    /* 银行账户户名 */
    BANK_ACCOUNT_HOLDER_NAME(RegExps.BANK_ACCOUNT_HOLDER_NAME, regExpMsg("bank-account-holder-name")),

    /* 存款人姓名 */
    PAYER_DISPLAY_NAME(RegExps.PAYER_DISPLAY_NAME, regExpMsg("payer-display-name")),

    /* 不含列出特殊符号 */
    TEXT_WITHOUT_SPECIAL_CHARS(RegExps.TEXT_WITHOUT_SPECIAL_CHARS, regExpMsg("text-without-special-chars")),

    /* 邮箱 */
    EMAIL(RegExps.EMAIL, regExpMsg("email")),

    /* 邮箱或中国大陆手机号 */
    MAIL_OR_CN_MOBILE(RegExps.MAIL_OR_CN_MOBILE, regExpMsg("mail-or-cn-mobile")),

    /* 国内固定电话 */
    CN_LANDLINE_PHONE(RegExps.CN_LANDLINE_PHONE, regExpMsg("cn-landline-phone")),

    /* 登录密码 6～20 */
    LOGIN_PASSWORD(RegExps.LOGIN_PASSWORD, regExpMsg("login-password")),

    /* 6 位数字安全码 */
    SECURITY_PIN_SIX_DIGITS(RegExps.SECURITY_PIN_SIX_DIGITS, regExpMsg("security-pin-six-digits")),

    /* 正整数文本 */
    POSITIVE_INT_TEXT(RegExps.POSITIVE_INT_TEXT, regExpMsg("positive-int-text")),

    /* 分号分隔 IPv4 列表 */
    IPV4_SEMICOLON_LIST(RegExps.IPV4_SEMICOLON_LIST, regExpMsg("ipv4-semicolon-list")),

    /* IPv6 全展开 */
    IPV6_FULL(RegExps.IPV6_FULL, regExpMsg("ipv6-full")),

    /* IPv6 压缩 */
    IPV6_COMPACT(RegExps.IPV6_COMPACT, regExpMsg("ipv6-compact")),

    /* 单字符连续重复 */
    SINGLE_CHAR_REPEATED(RegExps.SINGLE_CHAR_REPEATED, regExpMsg("single-char-repeated")),

    /* 至少一位数字 */
    DIGITS_NON_EMPTY(RegExps.DIGITS_NON_EMPTY, regExpMsg("digits-non-empty")),

    /* 仅拉丁字母 */
    LATIN_LETTERS_ONLY(RegExps.LATIN_LETTERS_ONLY, regExpMsg("latin-letters-only")),

    /* 仅小写拉丁字母 */
    LATIN_LOWERCASE_ONLY(RegExps.LATIN_LOWERCASE_ONLY, regExpMsg("latin-lowercase-only")),

    /* 小写字母与数字且非纯数字 */
    LOWERCASE_ALNUM_NOT_ALL_DIGITS(RegExps.LOWERCASE_ALNUM_NOT_ALL_DIGITS, regExpMsg("lowercase-alnum-not-all-digits")),

    /* 昵称 */
    NICK_NAME(RegExps.NICK_NAME, regExpMsg("nick-name")),

    /* 1～30 字符 */
    TEXT_1_TO_30_CHARS(RegExps.TEXT_1_TO_30_CHARS, regExpMsg("text-1-to-30-chars")),

    /* 与邮箱规则相同（历史 MSN）；新代码请用 [EMAIL] */
    MSN(RegExps.EMAIL, regExpMsg("msn")),

    /* 密码强度：仅字母 */
    PASSWORD_STRENGTH_LETTERS_ONLY(RegExps.PASSWORD_STRENGTH_LETTERS_ONLY, regExpMsg("password-strength-letters-only")),

    /* 密码强度：仅数字 */
    PASSWORD_STRENGTH_DIGITS_ONLY(RegExps.PASSWORD_STRENGTH_DIGITS_ONLY, regExpMsg("password-strength-digits-only")),

    /* 密码强度：字母与数字 */
    PASSWORD_STRENGTH_LETTERS_AND_DIGITS(RegExps.PASSWORD_STRENGTH_LETTERS_AND_DIGITS, regExpMsg("password-strength-letters-and-digits")),

    /* 密码强度：含符号 */
    PASSWORD_STRENGTH_WITH_SYMBOLS(RegExps.PASSWORD_STRENGTH_WITH_SYMBOLS, regExpMsg("password-strength-with-symbols")),

    /* 让分/比分 */
    SCORE_OR_HANDICAP_TEXT(RegExps.SCORE_OR_HANDICAP_TEXT, regExpMsg("score-or-handicap-text")),

    /* 正数小数串 */
    POSITIVE_DECIMAL_TEXT(RegExps.POSITIVE_DECIMAL_TEXT, regExpMsg("positive-decimal-text")),

    /* 正数整数或小数 */
    POSITIVE_NUMBER_TEXT(RegExps.POSITIVE_NUMBER_TEXT, regExpMsg("positive-number-text")),

    /* 带符号整数文本 */
    SIGNED_INTEGER_TEXT(RegExps.SIGNED_INTEGER_TEXT, regExpMsg("signed-integer-text")),

    /* 银行卡号 */
    BANK_CARD_NUMBER(RegExps.BANK_CARD_NUMBER, regExpMsg("bank-card-number")),

    /* 比特币数量 */
    BTC_AMOUNT_TEXT(RegExps.BTC_AMOUNT_TEXT, regExpMsg("btc-amount-text")),

    /* 逗号分隔站点 ID */
    SITE_IDS_COMMA_SEPARATED(RegExps.SITE_IDS_COMMA_SEPARATED, regExpMsg("site-ids-comma-separated")),

    /* 空或正整数 */
    EMPTY_OR_POSITIVE_INT_TEXT(RegExps.EMPTY_OR_POSITIVE_INT_TEXT, regExpMsg("empty-or-positive-int-text")),

    /* 至多 9 位数字 */
    DIGITS_AT_MOST_9(RegExps.DIGITS_AT_MOST_9, regExpMsg("digits-at-most-9")),

    /* 宽松金额 */
    SIGNED_AMOUNT_LOOSE(RegExps.SIGNED_AMOUNT_LOOSE, regExpMsg("signed-amount-loose")),

    /* 微信号 */
    WECHAT_ID(RegExps.WECHAT_ID, regExpMsg("wechat-id")),

    /* 玩家账号 */
    GAME_PLAYER_ACCOUNT(RegExps.GAME_PLAYER_ACCOUNT, regExpMsg("game-player-account")),

    /* UUID */
    UUID_HYPHENATED(RegExps.UUID_HYPHENATED, regExpMsg("uuid-hyphenated")),

    /* 域名列表 */
    DOMAIN_LIST_COMMA_SEPARATED(RegExps.DOMAIN_LIST_COMMA_SEPARATED, regExpMsg("domain-list-comma-separated")),

    /* 非零金额两位小数 */
    AMOUNT_NONZERO_TWO_DECIMALS(RegExps.AMOUNT_NONZERO_TWO_DECIMALS, regExpMsg("amount-nonzero-two-decimals")),

    /* 以数字开头 */
    TEXT_STARTS_WITH_DIGIT(RegExps.TEXT_STARTS_WITH_DIGIT, regExpMsg("text-starts-with-digit")),

    /* 仅数字可为空 */
    DIGITS_ONLY_OPTIONAL_EMPTY(RegExps.DIGITS_ONLY_OPTIONAL_EMPTY, regExpMsg("digits-only-optional-empty")),

    /* 中文、拉丁、数字 */
    HAN_LATIN_ALNUM(RegExps.HAN_LATIN_ALNUM, regExpMsg("han-latin-alnum")),

    /* ISO 日期 yyyy-MM-dd */
    DATE_ISO_YYYY_MM_DD(RegExps.DATE_ISO_YYYY_MM_DD, regExpMsg("date-iso-yyyy-mm-dd")),

    /* 24 小时制 HH:mm[:ss] */
    TIME_24H_MM_OPTIONAL_SS(RegExps.TIME_24H_MM_OPTIONAL_SS, regExpMsg("time-24h-mm-optional-ss")),

    /* 大陆邮编 6 位 */
    CN_MAINLAND_POSTAL_CODE(RegExps.CN_MAINLAND_POSTAL_CODE, regExpMsg("cn-mainland-postal-code")),

    /* #RGB / #RRGGBB / #RRGGBBAA */
    HEX_COLOR_CSS(RegExps.HEX_COLOR_CSS, regExpMsg("hex-color-css")),

    /* MAC 地址 */
    MAC_ADDRESS_COLON_OR_HYPHEN(RegExps.MAC_ADDRESS_COLON_OR_HYPHEN, regExpMsg("mac-address-colon-or-hyphen")),

    /* 端口 1～65535 */
    NETWORK_PORT_1_65535(RegExps.NETWORK_PORT_1_65535, regExpMsg("network-port-1-65535")),

    /* 小写 kebab slug */
    SLUG_KEBAB_LOWERCASE(RegExps.SLUG_KEBAB_LOWERCASE, regExpMsg("slug-kebab-lowercase")),

    /* 拉丁字母与数字 */
    LATIN_ALNUM_ONLY(RegExps.LATIN_ALNUM_ONLY, regExpMsg("latin-alnum-only")),

    /* IPv4/CIDR */
    IPV4_CIDR_NOTATION(RegExps.IPV4_CIDR_NOTATION, regExpMsg("ipv4-cidr-notation")),

    /* 0～100 整数百分比 */
    PERCENT_INTEGER_0_100(RegExps.PERCENT_INTEGER_0_100, regExpMsg("percent-integer-0-100")),

    /* 拉丁字母数字-_ */
    LATIN_ALNUM_DASH_UNDERSCORE(RegExps.LATIN_ALNUM_DASH_UNDERSCORE, regExpMsg("latin-alnum-dash-underscore")),

    /* 变量名 */
    VAR_NAME(RegExps.VAR_NAME, regExpMsg("var-name")),

    /* jdbc url */
    JDBC_URL(RegExps.JDBC_URL, regExpMsg("jdbc-url")),

    ;
}
