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

    //region Communication
    /**
     * 规则：
     * 中国大陆 11 位手机号
     * 以 `1` 开头
     * 第二、三位需落在当前规则列出的号段内
     *
     * 例如：
     * `13800138000`
     * `15912345678`
     * `19912345678`
     */
    CN_MAINLAND_MOBILE(RegExps.Communication.CN_MAINLAND_MOBILE, regExpMsg("cn-mainland-mobile")),

    /**
     * 规则：
     * QQ 号
     * 只允许数字
     * 长度 5～11 位
     *
     * 例如：
     * `12345`
     * `10000`
     * `12345678901`
     */
    QQ_NUMBER(RegExps.Communication.QQ_NUMBER, regExpMsg("qq-number")),

    /**
     * 规则：
     * 手机或电话的纯数字串
     * 不区分手机、固话、分机
     * 只校验数字和长度 7～20 位
     *
     * 例如：
     * `1234567`
     * `02012345678`
     * `12345678901234567890`
     */
    PHONE_DIGITS_7_20(RegExps.Communication.PHONE_DIGITS_7_20, regExpMsg("phone-digits-7-20")),

    /**
     * 规则：
     * 国内固话或手机号
     * 固话支持区号、连字符和可选分机
     * 手机号按规则内列出的号段匹配
     *
     * 例如：
     * `010-12345678`
     * `010-12345678-123`
     * `13800138000`
     */
    TEL_OR_CN_MOBILE(RegExps.Communication.TEL_OR_CN_MOBILE, regExpMsg("tel-or-cn-mobile")),

    /**
     * 规则：
     * 电子邮箱
     * 本地部分支持字母数字及 `.`、`_`、`-`
     * 域名部分按常见邮箱域名形态宽松匹配
     *
     * 例如：
     * `user@example.com`
     * `user.name@example.com`
     * `abc-123@test-mail.com`
     */
    EMAIL(RegExps.Communication.EMAIL, regExpMsg("email")),

    /**
     * 规则：
     * 电子邮箱或中国大陆手机号
     * 只要满足两者之一即可
     *
     * 例如：
     * `user@example.com`
     * `abc_01@test.com`
     * `13800138000`
     */
    MAIL_OR_CN_MOBILE(RegExps.Communication.MAIL_OR_CN_MOBILE, regExpMsg("mail-or-cn-mobile")),

    /**
     * 规则：
     * 国内固定电话
     * 支持 2 位或 3 位区号
     * 支持主号码和分机号，使用 `-` 连接
     *
     * 例如：
     * `010-12345678`
     * `021-1234567`
     * `010-12345678-123`
     */
    CN_LANDLINE_PHONE(RegExps.Communication.CN_LANDLINE_PHONE, regExpMsg("cn-landline-phone")),

    /**
     * 规则：
     * 微信号
     * 首字符必须是字母或数字
     * 后续可含字母、数字、`-`、`_`
     * 总长度 6～20
     *
     * 例如：
     * `wechat1`
     * `wx_123456`
     * `abc-def_01`
     */
    WECHAT_ID(RegExps.Communication.WECHAT_ID, regExpMsg("wechat-id")),
    //endregion

    //region Name
    /**
     * 规则：
     * 简短姓名
     * 允许汉字、拉丁字母和 `·`
     * 长度 2～30
     *
     * 例如：
     * `张三`
     * `王小明`
     * `John·Doe`
     */
    SHORT_PERSON_NAME(RegExps.Name.SHORT_PERSON_NAME, regExpMsg("short-person-name")),

    /**
     * 规则：
     * 真实姓名
     * 首尾必须是字母、汉字、空格规则允许的姓名字符
     * 中间可含数字、空格、`·`、`.`、`*`
     * 不能为纯数字
     *
     * 例如：
     * `张三`
     * `John Smith`
     * `李 四`
     */
    REAL_PERSON_NAME(RegExps.Name.REAL_PERSON_NAME, regExpMsg("real-person-name")),

    /**
     * 规则：
     * 银行账户户名
     * 允许中英文、数字、空格、`·`、`.`、括号、`*`
     * 适用于个人或公司名称展示
     *
     * 例如：
     * `张三`
     * `王五（公司）`
     * `ABC Trading Co.`
     */
    BANK_ACCOUNT_HOLDER_NAME(RegExps.Name.BANK_ACCOUNT_HOLDER_NAME, regExpMsg("bank-account-holder-name")),

    /**
     * 规则：
     * 存款人姓名
     * 首尾必须是中英文字符
     * 中间允许空格、`.`、`·`
     *
     * 例如：
     * `张三`
     * `Jane Doe`
     * `Li Lei`
     */
    PAYER_DISPLAY_NAME(RegExps.Name.PAYER_DISPLAY_NAME, regExpMsg("payer-display-name")),

    /**
     * 规则：
     * 昵称
     * 允许中文、英文、数字
     * 长度 3～15
     *
     * 例如：
     * `小明007`
     * `Alice88`
     * `玩家123`
     */
    NICK_NAME(RegExps.Name.NICK_NAME, regExpMsg("nick-name")),
    //endregion

    //region Text
    /**
     * 规则：
     * 文本不得包含 `&`、`*`、`=`、`|`、`{}`、`<>`、`/`、`…`、`—`
     * 未列出的普通字符不受此规则限制
     *
     * 例如：
     * `hello world`
     * `备注信息`
     * `A_B-C.1`
     */
    TEXT_WITHOUT_SPECIAL_CHARS(RegExps.Text.TEXT_WITHOUT_SPECIAL_CHARS, regExpMsg("text-without-special-chars")),

    /**
     * 规则：
     * 任意字符
     * 长度 1～30
     * 常用于密保答案、自由文本短字段
     *
     * 例如：
     * `a`
     * `my answer`
     * `答案123`
     */
    TEXT_1_TO_30_CHARS(RegExps.Text.TEXT_1_TO_30_CHARS, regExpMsg("text-1-to-30-chars")),

    /**
     * 规则：
     * 首字符必须是数字
     * 后续字符不受此规则限制
     *
     * 例如：
     * `1`
     * `1abc`
     * `9-xyz`
     */
    TEXT_STARTS_WITH_DIGIT(RegExps.Text.TEXT_STARTS_WITH_DIGIT, regExpMsg("text-starts-with-digit")),

    /**
     * 规则：
     * 仅含数字
     * 允许空串
     * 常用于“只输数字”的可选字段
     *
     * 例如：
     * ``
     * `12345`
     * `0001`
     */
    DIGITS_ONLY_OPTIONAL_EMPTY(RegExps.Text.DIGITS_ONLY_OPTIONAL_EMPTY, regExpMsg("digits-only-optional-empty")),

    /**
     * 规则：
     * 仅中文、拉丁字母与数字
     * 不允许空格和符号
     *
     * 例如：
     * `abc123`
     * `中国123`
     * `Kudos中国123`
     */
    HAN_LATIN_ALNUM(RegExps.Text.HAN_LATIN_ALNUM, regExpMsg("han-latin-alnum")),

    /**
     * 上下文
     *
     * 规则：
     * 必须以 `/` 开头
     * 只用小写字母、数字、短横线 `-`
     * 多级路径用 `/` 分隔
     * 每一段不能为空
     * 不用空格、中文、下划线、连续斜杠
     * 不把 query、fragment 风格的内容塞进 path 里
     *
     * 例如：
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
     * 规则：
     * 整个字符串由同一个字符重复组成
     * 重复次数至少 2 次
     *
     * 例如：
     * `aaa`
     * `1111`
     * `%%%%`
     */
    SINGLE_CHAR_REPEATED(RegExps.CharacterSet.SINGLE_CHAR_REPEATED, regExpMsg("single-char-repeated")),

    /**
     * 规则：
     * 至少一位数字
     * 只允许数字
     *
     * 例如：
     * `0`
     * `123`
     * `987654321`
     */
    DIGITS_NON_EMPTY(RegExps.CharacterSet.DIGITS_NON_EMPTY, regExpMsg("digits-non-empty")),

    /**
     * 规则：
     * 仅拉丁字母
     * 允许大写和小写
     * 不允许数字和符号
     *
     * 例如：
     * `abc`
     * `ABC`
     * `AbCd`
     */
    LATIN_LETTERS_ONLY(RegExps.CharacterSet.LATIN_LETTERS_ONLY, regExpMsg("latin-letters-only")),

    /**
     * 规则：
     * 仅小写拉丁字母
     * 不允许大写字母、数字、符号
     *
     * 例如：
     * `abc`
     * `kudos`
     * `username`
     */
    LATIN_LOWERCASE_ONLY(RegExps.CharacterSet.LATIN_LOWERCASE_ONLY, regExpMsg("latin-lowercase-only")),

    /**
     * 规则：
     * 仅小写字母与数字
     * 不能是纯数字
     * 至少包含一个小写字母
     *
     * 例如：
     * `abc123`
     * `a1`
     * `user2026`
     */
    LOWERCASE_ALNUM_NOT_ALL_DIGITS(RegExps.CharacterSet.LOWERCASE_ALNUM_NOT_ALL_DIGITS, regExpMsg("lowercase-alnum-not-all-digits")),

    /**
     * 规则：
     * 仅 ASCII 数字
     * 至少一位
     * 不接受全角数字
     *
     * 例如：
     * `0`
     * `123456`
     * `987654321`
     */
    ASCII_DIGITS_ONLY(RegExps.CharacterSet.ASCII_DIGITS_ONLY, regExpMsg("ascii-digits-only")),

    /**
     * 规则：
     * 仅拉丁字母与数字
     * 不允许空格、下划线、连字符和其他符号
     *
     * 例如：
     * `abc`
     * `ABC123`
     * `Abc123`
     */
    LATIN_ALNUM_ONLY(RegExps.CharacterSet.LATIN_ALNUM_ONLY, regExpMsg("latin-alnum-only")),

    /**
     * 规则：
     * 仅拉丁字母、数字、连字符、下划线
     * 至少 1 位
     *
     * 例如：
     * `user`
     * `user_name`
     * `user-name_01`
     */
    LATIN_ALNUM_DASH_UNDERSCORE(RegExps.CharacterSet.LATIN_ALNUM_DASH_UNDERSCORE, regExpMsg("latin-alnum-dash-underscore")),

    /**
     * 规则：
     * 标识符
     * 首字符必须是字母或下划线
     * 后续允许字母、数字、下划线
     *
     * 例如：
     * `name`
     * `_value`
     * `user_name1`
     */
    VAR_NAME(RegExps.CharacterSet.VAR_NAME, regExpMsg("var-name")),

    /**
     * 规则：
     * 小写 slug
     * 只允许小写字母、数字、连字符
     * 不允许连续连字符分段为空
     *
     * 例如：
     * `article`
     * `user-profile`
     * `user-profile-01`
     */
    SLUG_KEBAB_LOWERCASE(RegExps.CharacterSet.SLUG_KEBAB_LOWERCASE, regExpMsg("slug-kebab-lowercase")),
    //endregion

    //region Security
    /**
     * 规则：
     * 登录密码
     * 长度 6～20
     * 允许字母、数字和常见 ASCII 符号
     *
     * 例如：
     * `Abc123`
     * `Abc123!`
     * `Pass_word#1`
     */
    LOGIN_PASSWORD(RegExps.Security.LOGIN_PASSWORD, regExpMsg("login-password")),

    /**
     * 规则：
     * 6 位数字安全码 / PIN
     * 只允许数字
     * 长度固定为 6
     *
     * 例如：
     * `123456`
     * `000001`
     * `987654`
     */
    SECURITY_PIN_SIX_DIGITS(RegExps.Security.SECURITY_PIN_SIX_DIGITS, regExpMsg("security-pin-six-digits")),

    /**
     * 规则：
     * 密码强度分类中的“仅字母”
     * 规则与 [LATIN_LETTERS_ONLY] 完全一致
     *
     * 例如：
     * `abc`
     * `OnlyText`
     * `Password`
     */
    PASSWORD_STRENGTH_LETTERS_ONLY(RegExps.Security.PASSWORD_STRENGTH_LETTERS_ONLY, regExpMsg("password-strength-letters-only")),

    /**
     * 规则：
     * 密码强度分类中的“仅数字”
     * 规则与 [ASCII_DIGITS_ONLY] 完全一致
     *
     * 例如：
     * `123456`
     * `987654`
     * `20260403`
     */
    PASSWORD_STRENGTH_DIGITS_ONLY(RegExps.Security.PASSWORD_STRENGTH_DIGITS_ONLY, regExpMsg("password-strength-digits-only")),

    /**
     * 规则：
     * 密码强度分类中的“字母与数字”
     * 允许大小写字母和数字
     * 不允许符号
     *
     * 例如：
     * `Abc123`
     * `abc999`
     * `PASS2026`
     */
    PASSWORD_STRENGTH_LETTERS_AND_DIGITS(RegExps.Security.PASSWORD_STRENGTH_LETTERS_AND_DIGITS, regExpMsg("password-strength-letters-and-digits")),

    /**
     * 规则：
     * 密码强度分类中的“字母、数字与常用符号”
     * 允许大小写字母、数字和 ASCII 常见符号
     *
     * 例如：
     * `Abc123!`
     * `P@ssw0rd#`
     * `A1_b2-C3`
     */
    PASSWORD_STRENGTH_WITH_SYMBOLS(RegExps.Security.PASSWORD_STRENGTH_WITH_SYMBOLS, regExpMsg("password-strength-with-symbols")),
    //endregion

    //region Numeric
    /**
     * 规则：
     * 正整数文本
     * 不允许负号、小数
     * 不允许以 `0` 开头
     *
     * 例如：
     * `1`
     * `42`
     * `999999`
     */
    POSITIVE_INT_TEXT(RegExps.Numeric.POSITIVE_INT_TEXT, regExpMsg("positive-int-text")),

    /**
     * 规则：
     * 让分或比分类比分串
     * 支持可选负号
     * 支持整数、小数、`/` 或 `-` 作为分隔形式
     *
     * 例如：
     * `1.5`
     * `1.5/2`
     * `-0.5`
     */
    SCORE_OR_HANDICAP_TEXT(RegExps.Numeric.SCORE_OR_HANDICAP_TEXT, regExpMsg("score-or-handicap-text")),

    /**
     * 规则：
     * 正数文本
     * 允许整数或小数
     * 不允许负号
     * 禁止多余前导零
     *
     * 例如：
     * `1`
     * `12`
     * `12.5`
     */
    POSITIVE_DECIMAL_TEXT(RegExps.Numeric.POSITIVE_DECIMAL_TEXT, regExpMsg("positive-decimal-text")),

    /**
     * 规则：
     * 正数
     * 可以是大于 0 的整数
     * 也可以是 `0.xxx` 且小数部分至少有一个非零位
     *
     * 例如：
     * `8`
     * `10.5`
     * `0.25`
     */
    POSITIVE_NUMBER_TEXT(RegExps.Numeric.POSITIVE_NUMBER_TEXT, regExpMsg("positive-number-text")),

    /**
     * 规则：
     * 整数文本
     * 可带 `+` 或 `-`
     * 不允许小数点
     *
     * 例如：
     * `0`
     * `-10`
     * `+8`
     */
    SIGNED_INTEGER_TEXT(RegExps.Numeric.SIGNED_INTEGER_TEXT, regExpMsg("signed-integer-text")),

    /**
     * 规则：
     * 比特币数量
     * 不允许全零
     * 整数部分可为正整数
     * 小数部分最多 8 位
     *
     * 例如：
     * `1`
     * `0.12345`
     * `1.00000001`
     */
    BTC_AMOUNT_TEXT(RegExps.Numeric.BTC_AMOUNT_TEXT, regExpMsg("btc-amount-text")),

    /**
     * 规则：
     * 空串或正整数
     * 如果有值，则必须是大于 0 的整数
     *
     * 例如：
     * ``
     * `1`
     * `25`
     */
    EMPTY_OR_POSITIVE_INT_TEXT(RegExps.Numeric.EMPTY_OR_POSITIVE_INT_TEXT, regExpMsg("empty-or-positive-int-text")),

    /**
     * 规则：
     * 最多 9 位数字
     * 可为空
     * 不允许其他字符
     *
     * 例如：
     * ``
     * `1`
     * `123456789`
     */
    DIGITS_AT_MOST_9(RegExps.Numeric.DIGITS_AT_MOST_9, regExpMsg("digits-at-most-9")),

    /**
     * 规则：
     * 金额
     * 可带负号
     * 整数部分 0 或非零开头整数
     * 小数部分最多 2 位
     *
     * 例如：
     * `0`
     * `12.5`
     * `-12.50`
     */
    SIGNED_AMOUNT_LOOSE(RegExps.Numeric.SIGNED_AMOUNT_LOOSE, regExpMsg("signed-amount-loose")),

    /**
     * 规则：
     * 非零金额
     * 金额不能为 `0` 或 `0.00`
     * 小数部分最多 2 位
     *
     * 例如：
     * `0.01`
     * `1`
     * `99.99`
     */
    AMOUNT_NONZERO_TWO_DECIMALS(RegExps.Numeric.AMOUNT_NONZERO_TWO_DECIMALS, regExpMsg("amount-nonzero-two-decimals")),

    /**
     * 规则：
     * 整数百分比
     * 只允许整数
     * 取值范围 0～100
     *
     * 例如：
     * `0`
     * `85`
     * `100`
     */
    PERCENT_INTEGER_0_100(RegExps.Numeric.PERCENT_INTEGER_0_100, regExpMsg("percent-integer-0-100")),
    //endregion

    //region Network
    /**
     * 规则：
     * IPv4 点分十进制地址
     * 共 4 段
     * 每段取值范围 0～255
     *
     * 例如：
     * `127.0.0.1`
     * `192.168.1.1`
     * `255.255.255.255`
     */
    IPV4(RegExps.Network.IPV4, regExpMsg("ipv4")),

    /**
     * 规则：
     * URL，支持 `http`、`https`、`ftp`、`sftp`
     * 允许域名、IPv4、端口、路径、query、fragment
     * 属于宽松匹配，不等同完整 RFC 严格校验
     *
     * 例如：
     * `https://example.com`
     * `http://example.com:8080/api`
     * `https://example.com/search?q=test#top`
     */
    HTTP_URL(RegExps.Network.HTTP_URL, regExpMsg("http-url")),

    /**
     * 规则：
     * 多个 IPv4 地址
     * 使用英文分号 `;` 分隔
     * 每个成员都必须是合法 IPv4
     *
     * 例如：
     * `10.0.0.1`
     * `10.0.0.1;192.168.1.1`
     * `127.0.0.1;8.8.8.8;1.1.1.1`
     */
    IPV4_SEMICOLON_LIST(RegExps.Network.IPV4_SEMICOLON_LIST, regExpMsg("ipv4-semicolon-list")),

    /**
     * 规则：
     * IPv6 全展开地址
     * 共 8 组十六进制段
     * 每组 1～4 位十六进制字符
     *
     * 例如：
     * `2001:0db8:0000:0000:0000:ff00:0042:8329`
     * `fe80:0000:0000:0000:0202:b3ff:fe1e:8329`
     */
    IPV6_FULL(RegExps.Network.IPV6_FULL, regExpMsg("ipv6-full")),

    /**
     * 规则：
     * IPv6 压缩地址
     * 必须包含 `::`
     * `::` 左右允许省略若干段
     *
     * 例如：
     * `2001:db8::1`
     * `::1`
     * `fe80::abcd`
     */
    IPV6_COMPACT(RegExps.Network.IPV6_COMPACT, regExpMsg("ipv6-compact")),

    /**
     * 规则：
     * 逗号分隔域名列表
     * 每个成员都必须是标准点分域名
     * 使用英文逗号 `,` 分隔
     *
     * 例如：
     * `a.example.com`
     * `a.example.com,b.example.com`
     * `api.test.com,cdn.test.com`
     */
    DOMAIN_LIST_COMMA_SEPARATED(RegExps.Network.DOMAIN_LIST_COMMA_SEPARATED, regExpMsg("domain-list-comma-separated")),

    /**
     * 规则：
     * MAC 地址
     * 6 组十六进制字符
     * 组之间允许 `:` 或 `-`
     *
     * 例如：
     * `00:1A:2B:3C:4D:5E`
     * `AA-BB-CC-DD-EE-FF`
     */
    MAC_ADDRESS_COLON_OR_HYPHEN(RegExps.Network.MAC_ADDRESS_COLON_OR_HYPHEN, regExpMsg("mac-address-colon-or-hyphen")),

    /**
     * 规则：
     * 网络端口号
     * 只允许十进制数字
     * 取值范围 1～65535
     *
     * 例如：
     * `80`
     * `443`
     * `65535`
     */
    NETWORK_PORT_1_65535(RegExps.Network.NETWORK_PORT_1_65535, regExpMsg("network-port-1-65535")),

    /**
     * 规则：
     * IPv4 CIDR 表示法
     * 前半部分必须是 IPv4
     * 前缀长度范围 0～32
     *
     * 例如：
     * `192.168.1.0/24`
     * `10.0.0.1/32`
     * `0.0.0.0/0`
     */
    IPV4_CIDR_NOTATION(RegExps.Network.IPV4_CIDR_NOTATION, regExpMsg("ipv4-cidr-notation")),

    /**
     * 规则：
     * JDBC URL
     * 必须以 `jdbc:` 开头
     * 协议名后可跟驱动类型和连接信息
     * 后续不允许空白字符
     *
     * 例如：
     * `jdbc:mysql://localhost:3306/kudos`
     * `jdbc:postgresql://db:5432/app`
     * `jdbc:h2:mem:test`
     */
    JDBC_URL(RegExps.Network.JDBC_URL, regExpMsg("jdbc-url")),

    /**
     * 规则：
     * 域名
     * 支持 `localhost`
     * 或标准点分域名
     * 总长度和每段首尾连字符规则受限
     *
     * 例如：
     * `localhost`
     * `example.com`
     * `api.example.com`
     */
    DOMAIN(RegExps.Network.DOMAIN, regExpMsg("domain")),
    //endregion

    //region Business
    /**
     * 规则：
     * 银行卡号
     * 只允许数字
     * 长度 10～25
     *
     * 例如：
     * `6222021234567890`
     * `6222021234567890123`
     * `1234567890123456789012345`
     */
    BANK_CARD_NUMBER(RegExps.Business.BANK_CARD_NUMBER, regExpMsg("bank-card-number")),

    /**
     * 规则：
     * 站点 ID 列表
     * 支持单个数字
     * 支持英文逗号分隔多个数字
     * 也允许全空白
     *
     * 例如：
     * `42`
     * `1,2,3`
     * `   `
     */
    SITE_IDS_COMMA_SEPARATED(RegExps.Business.SITE_IDS_COMMA_SEPARATED, regExpMsg("site-ids-comma-separated")),

    /**
     * 规则：
     * 玩家账号
     * 首字符允许字母、数字、下划线、`$`
     * 后续只允许字母、数字、下划线
     * 总长度 4～15
     *
     * 例如：
     * `user1`
     * `guest_01`
     * `$abc_123`
     */
    GAME_PLAYER_ACCOUNT(RegExps.Business.GAME_PLAYER_ACCOUNT, regExpMsg("game-player-account")),
    //endregion

    //region Format
    /**
     * 规则：
     * 带连字符的 UUID
     * 形如 `8-4-4-4-12`
     * 每段由字母数字下划线字符类 `\\w` 匹配
     *
     * 例如：
     * `123e4567-e89b-12d3-a456-426614174000`
     * `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee`
     */
    UUID_HYPHENATED(RegExps.Format.UUID_HYPHENATED, regExpMsg("uuid-hyphenated")),

    /**
     * 规则：
     * 日期格式 `yyyy-MM-dd`
     * 仅校验年月日格式
     * 不保证闰年和大小月绝对正确
     *
     * 例如：
     * `2026-04-03`
     * `1999-12-31`
     * `2024-02-29`
     */
    DATE_ISO_YYYY_MM_DD(RegExps.Format.DATE_ISO_YYYY_MM_DD, regExpMsg("date-iso-yyyy-mm-dd")),

    /**
     * 规则：
     * 24 小时制时间
     * 基础格式 `HH:mm`
     * 可追加秒 `:ss`
     *
     * 例如：
     * `09:30`
     * `23:59`
     * `23:59:59`
     */
    TIME_24H_MM_OPTIONAL_SS(RegExps.Format.TIME_24H_MM_OPTIONAL_SS, regExpMsg("time-24h-mm-optional-ss")),

    /**
     * 规则：
     * 中国大陆邮政编码
     * 只允许数字
     * 长度固定 6 位
     *
     * 例如：
     * `100000`
     * `200120`
     * `518000`
     */
    CN_MAINLAND_POSTAL_CODE(RegExps.Format.CN_MAINLAND_POSTAL_CODE, regExpMsg("cn-mainland-postal-code")),

    /**
     * 规则：
     * CSS 十六进制颜色值
     * 支持 `#RGB`
     * 支持 `#RRGGBB`
     * 支持 `#RRGGBBAA`
     *
     * 例如：
     * `#fff`
     * `#FF8800`
     * `#11223344`
     */
    HEX_COLOR_CSS(RegExps.Format.HEX_COLOR_CSS, regExpMsg("hex-color-css")),
    //endregion

    /* 与邮箱规则相同（历史 MSN）；新代码请用 [EMAIL] */
    MSN(RegExps.Communication.EMAIL, regExpMsg("msn")),

    ;
}
