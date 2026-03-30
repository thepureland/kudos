package io.kudos.base.bean.validation.support

/**
 * 常用正则表达式常量。命名尽量表达「匹配什么」而非历史缩写；与 [RegExpEnum] 一一对应。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object RegExps {

    /** 中国大陆 11 位手机号 */
    const val CN_MAINLAND_MOBILE = "^1(3[0-9]|4[579]|5[0-35-9]|6[0-9]|7[0-9]|8[0-9]|9[0-9])\\d{8}$"

    /** QQ 号：5～11 位数字 */
    const val QQ_NUMBER = "^\\d{5,11}$"

    /** 手机或电话的纯数字串，7～20 位（不区分固话/手机形态，仅长度） */
    const val PHONE_DIGITS_7_20 = "^\\d{7,20}$"

    /** IPv4 点分十进制 */
    const val IPV4 = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"

    /** HTTP(S) / FTP(S) URL（宽松 RFC 式） */
    const val HTTP_URL = (("^(https?|s?ftp):\\/\\/(((([A-Za-z]|\\d|-|\\.|_|~|"
            + "[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(%[\\da-f]{2})|[!\\$&'\\(\\)\\*\\+,;=]|:)*@)?(((\\d|"
            + "[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d\\d|"
            + "2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]))|((([A-Za-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|"
            + "(([A-Za-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])*"
            + "([A-Za-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])))\\.)+(([A-Za-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|"
            + "(([A-Za-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])*"
            + "([A-Za-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])))\\.?)(:\\d*)?)(\\/((([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|"
            + "(%[\\da-f]{2})|[!\\$&'\\(\\)\\*\\+,;=]|:|@)+(\\/(([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(%[\\da-f]{2})|"
            + "[!\\$&'\\(\\)\\*\\+,;=]|:|@)*)*)?)?(\\?((([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(%[\\da-f]{2})|[!\\$&'\\(\\)\\*\\+,;=]|:"
            + "|@)|[\\uE000-\\uF8FF]|\\/|\\?)*)?(#((([A-Za-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(%[\\da-f]{2})|[!\\$&'\\(\\)\\*\\+,;=]|:|@)|\\/|\\?)*)?$"))

    /** 国内固话或手机号（区号、分机等常见形态） */
    const val TEL_OR_CN_MOBILE =
        "^0\\d{2,3}-?\\d{7,8}$|^(0\\d{2,3})-?(\\d{7,8})-?(\\d{1,4})$|^1(3[0-9]|4[57]|5[0-35-9]|7[0-9]|8[0-35-9])\\d{8}$"

    /** 简短姓名：汉字、小写拉丁、大写拉丁与间隔号 ·，2～30 位 */
    const val SHORT_PERSON_NAME = "^[a-z\\u4E00-\\u9FA5\\u0800-\\u4e00\\\\A-Z\\·]{2,30}$"

    /** 真实姓名：多脚本、空格与分隔符，不能为纯数字 */
    const val REAL_PERSON_NAME = ("^[a-zA-Z\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]"
            + "[a-zA-Z0-9\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\.\\* ]{0,28}"
            + "[a-zA-Z\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]$")

    /** 银行账户户名 */
    const val BANK_ACCOUNT_HOLDER_NAME = ("^[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]"
            + "[a-zA-Z0-9\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\.()（）\\* ]{0,28}"
            + "[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\*()（）]$")

    /** 存款人姓名 */
    const val PAYER_DISPLAY_NAME =
        "^[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00][a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\. ]{0,28}[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00]$"

    /** 不得包含 & * = | { } < > / … — 等列出符号 */
    const val TEXT_WITHOUT_SPECIAL_CHARS = "^[^&*=|{}<>/\\…—]*$"

    /** 电子邮箱（宽松） */
    const val EMAIL = "^[a-zA-Z0-9]+([._\\-]*[a-zA-Z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+$"

    /** 邮箱或中国大陆手机号 */
    const val MAIL_OR_CN_MOBILE =
        "^([a-z0-9]+([._\\-]*[a-z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+)|(1(3[0-9]|5[0-35-9]|7[0-9]|8[0-35-9])\\d{8})$"

    /** 国内固定电话（含区号、分机连字符等） */
    const val CN_LANDLINE_PHONE =
        "^(((0\\d{2})[-](\\d{8})$)|(^(0\\d{3})[-](\\d{7,8})$)|(^(0\\d{2})[-](\\d{8})-(\\d+)$)|(^(0\\d{3})[-](\\d{7,8})-(\\d+)))$"

    /** 登录密码：6～20，字母数字及允许符号集 */
    const val LOGIN_PASSWORD = "^[A-Za-z0-9~\\\\\\-!@#$%^&*()_+\\{\\}\\[\\]|\\:;'\"<>,./?]{6,20}$"

    /** 6 位数字安全码 / PIN */
    const val SECURITY_PIN_SIX_DIGITS = "^[0-9]{6}$"

    /** 正整数文本（不以 0 开头，除单字符 0 外不适用—此处为 1–9 开头） */
    const val POSITIVE_INT_TEXT = "^[1-9]\\d*$"

    /** 多个 IPv4，分号分隔 */
    const val IPV4_SEMICOLON_LIST =
        "^((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3})(;((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}))*$"

    /** IPv6 全展开 */
    const val IPV6_FULL = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"

    /** IPv6 压缩（含 ::） */
    const val IPV6_COMPACT =
        "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"

    /** 同一字符连续重复（如 aaa） */
    const val SINGLE_CHAR_REPEATED = "^(.)\\1+$"

    /** 至少一位数字（\\d+） */
    const val DIGITS_NON_EMPTY = "^\\d+$"

    /** 仅拉丁字母（大小写）；与 [PASSWORD_STRENGTH_LETTERS_ONLY] 同规则 */
    const val LATIN_LETTERS_ONLY = "^[a-zA-Z]+$"

    /** 密码强度：仅字母（同 [LATIN_LETTERS_ONLY]） */
    const val PASSWORD_STRENGTH_LETTERS_ONLY = LATIN_LETTERS_ONLY

    /** 仅小写拉丁字母 */
    const val LATIN_LOWERCASE_ONLY = "^[a-z]+$"

    /** 小写字母与数字，且不能为纯数字 */
    const val LOWERCASE_ALNUM_NOT_ALL_DIGITS = "^(?!\\d+$)[\\da-z]+$"

    /** 昵称：中文、英文、数字，3～15 */
    const val NICK_NAME = "^[a-zA-Z0-9\\u4E00-\\u9FA5\\u0800-\\u4e00]{3,15}$"

    /** 任意字符 1～30（如密保答案） */
    const val TEXT_1_TO_30_CHARS = "^(.){1,30}$"

    /** 仅 ASCII 数字（至少一位）；密码强度「仅数字」同规则 */
    const val ASCII_DIGITS_ONLY = "^[0-9]+$"

    /** 密码强度：仅数字（同 [ASCII_DIGITS_ONLY]） */
    const val PASSWORD_STRENGTH_DIGITS_ONLY = ASCII_DIGITS_ONLY

    /** 密码强度：字母与数字 */
    const val PASSWORD_STRENGTH_LETTERS_AND_DIGITS = "^[0-9a-zA-Z]+$"

    /** 密码强度：字母、数字与常用符号 */
    const val PASSWORD_STRENGTH_WITH_SYMBOLS =
        "^[A-Za-z0-9~!@#$%^&*()_+\\\\{\\\\}\\\\[\\\\]|\\\\:;\\'\\\"<>,./?]+$"

    /** 让分 / 比分类比分串 */
    const val SCORE_OR_HANDICAP_TEXT =
        "^(\\-?)((?:[0-9]{1,4})(?:\\.\\d{1,2})?)(\\/?|\\-?)((?:[0-9]{1,4}|0)(?:\\.\\d{1,2})?)?$"

    /** 正数（整数或小数），禁止多余前导零 */
    const val POSITIVE_DECIMAL_TEXT = "^(?!(0[0-9]{0,}$))[0-9]{1,}[.]{0,}[0-9]{0,}$"

    /** 正数：整数或形如 0.xxx 且小数部有非零 */
    const val POSITIVE_NUMBER_TEXT = "^([1-9]\\d*\\.?\\d*)|(0\\.\\d*[1-9])$"

    /** 整数文本，可选正负号 */
    const val SIGNED_INTEGER_TEXT = "^(-|\\+)?\\d+$"

    /** 银行卡号 10～25 位数字 */
    const val BANK_CARD_NUMBER = "^[0-9]{10,25}$"

    /** 比特币数量（精度与下限按规则） */
    const val BTC_AMOUNT_TEXT = "^(?!0+(?:\\.0+)?$)(((?:[1-9]\\d*(\\.\\d{1,8})?))|(0\\.\\d{1,5}))?$"

    /** 逗号分隔站点 ID；单数字；或空白 */
    const val SITE_IDS_COMMA_SEPARATED = "^\\d+(,\\d+)*$|^\\d+$|^(\\s)*$"

    /** 空串或正整数 */
    const val EMPTY_OR_POSITIVE_INT_TEXT = "^([0-9]*[1-9][0-9]*)?$"

    /** 至多 9 位数字（含空） */
    const val DIGITS_AT_MOST_9 = "^\\d{0,9}$"

    /** 金额：可选负号，整数部与最多两位小数 */
    const val SIGNED_AMOUNT_LOOSE = "^\\-?([1-9]\\d{0,9}|0)([.]?|(\\.\\d{1,2})?)$"

    /** 微信号 */
    const val WECHAT_ID = "^[a-zA-Z0-9]{1}[-_a-zA-Z0-9]{5,19}$"

    /** 玩家账号（含游客） */
    const val GAME_PLAYER_ACCOUNT = "^[a-zA-Z0-9_\\$][a-zA-Z0-9_]{3,14}$"

    /** UUID 带连字符 8-4-4-4-12 */
    const val UUID_HYPHENATED = "^\\w{8}(-\\w{4}){3}-\\w{12}$"

    /** 逗号分隔域名列表 */
    const val DOMAIN_LIST_COMMA_SEPARATED =
        "^(([a-zA-Z0-9]+\\.)+([a-zA-Z0-9]+){1}\\,)*(([a-zA-Z0-9]+\\.)+([a-zA-Z0-9]+){1}){1}$"

    /** 金额：非零，最多两位小数 */
    const val AMOUNT_NONZERO_TWO_DECIMALS = "^(?!0+(?:\\.0+)?$)(?:[1-9]\\d*|0)(?:\\.\\d{1,2})?$"

    /** 首字符为数字（0–9）；并非「非负整数集」语义 */
    const val TEXT_STARTS_WITH_DIGIT = "^[0-9].*$"

    /** 仅含数字，可为空（常用于「只输入数字」框，不等同于手机号） */
    const val DIGITS_ONLY_OPTIONAL_EMPTY = "^[0-9]*$"

    /** 中文、拉丁字母与数字 */
    const val HAN_LATIN_ALNUM = "^[0-9a-zA-Z\\u4e00-\\u9fa5]+$"

    /** ISO 8601 日历日期 yyyy-MM-dd（仅格式校验，不保证闰年与每月天数绝对正确） */
    const val DATE_ISO_YYYY_MM_DD = "^(?:\\d{4})-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$"

    /** 24 小时制时间 HH:mm，可选 :ss */
    const val TIME_24H_MM_OPTIONAL_SS = "^(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?$"

    /** 中国大陆邮政编码 6 位数字 */
    const val CN_MAINLAND_POSTAL_CODE = "^\\d{6}$"

    /** CSS 十六进制颜色：#RGB、#RRGGBB、#RRGGBBAA */
    const val HEX_COLOR_CSS = "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"

    /** MAC 地址，冒号或连字符分隔的 6 组十六进制 */
    const val MAC_ADDRESS_COLON_OR_HYPHEN = "^(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$"

    /** TCP/UDP 端口 1～65535 */
    const val NETWORK_PORT_1_65535 =
        "^(?:[1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$"

    /** 小写 slug：小写字母与数字，段以连字符连接（常用于 URL 路径段） */
    const val SLUG_KEBAB_LOWERCASE = "^[a-z0-9]+(?:-[a-z0-9]+)*$"

    /** 仅拉丁字母与数字（无空格与符号） */
    const val LATIN_ALNUM_ONLY = "^[a-zA-Z0-9]+$"

    /** IPv4 CIDR，前缀长度 0～32 */
    const val IPV4_CIDR_NOTATION =
        "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\/(?:3[0-2]|[12]?\\d)$"

    /** 整数百分比 0～100 */
    const val PERCENT_INTEGER_0_100 = "^(?:100|[1-9]\\d?|0)$"

    /** 拉丁字母、数字、连字符与下划线（常见「登录名」宽松形态，至少一位） */
    const val LATIN_ALNUM_DASH_UNDERSCORE = "^[a-zA-Z0-9_-]+$"

    /** 标识符：字母或下划线开头，后跟字母数字下划线 */
    const val VAR_NAME = "^[A-Za-z_][A-Za-z0-9_]*$"
}
