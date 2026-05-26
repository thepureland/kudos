package io.kudos.base.bean.validation.support

/**
 * Common regular expression constants. Names aim to express "what is matched" rather than historic abbreviations;
 * one-to-one correspondence with [RegExpEnum].
 *
 * Split into inner objects by purpose for ease of lookup and maintenance; flat constants with the same names are
 * preserved to avoid forcing existing callers to migrate.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object RegExps {

    /**
     * Regexes related to communication and contact information.
     * Covers mobile numbers, landline phones, QQ, email, WeChat ID and similar scenarios.
     *
     * @author K
     * @since 1.0.0
     */
    object Communication {

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
        const val CN_MAINLAND_MOBILE = "^1(3[0-9]|4[579]|5[0-35-9]|6[0-9]|7[0-9]|8[0-9]|9[0-9])\\d{8}$"

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
        const val QQ_NUMBER = "^\\d{5,11}$"

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
        const val PHONE_DIGITS_7_20 = "^\\d{7,20}$"

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
        const val TEL_OR_CN_MOBILE =
            "^0\\d{2,3}-?\\d{7,8}$|^(0\\d{2,3})-?(\\d{7,8})-?(\\d{1,4})$|^1(3[0-9]|4[57]|5[0-35-9]|7[0-9]|8[0-35-9])\\d{8}$"

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
        const val EMAIL = "^[a-zA-Z0-9]+([._\\-]*[a-zA-Z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+$"

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
        const val MAIL_OR_CN_MOBILE =
            "^([a-z0-9]+([._\\-]*[a-z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+)|(1(3[0-9]|5[0-35-9]|7[0-9]|8[0-35-9])\\d{8})$"

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
        const val CN_LANDLINE_PHONE =
            "^(((0\\d{2})[-](\\d{8})$)|(^(0\\d{3})[-](\\d{7,8})$)|(^(0\\d{2})[-](\\d{8})-(\\d+)$)|(^(0\\d{3})[-](\\d{7,8})-(\\d+)))$"

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
        const val WECHAT_ID = "^[a-zA-Z0-9]{1}[-_a-zA-Z0-9]{5,19}$"
    }

    /**
     * Regexes related to personal / display names.
     * Includes real names, bank account holder names, nicknames and other more strictly constrained name fields.
     *
     * @author K
     * @since 1.0.0
     */
    object Name {

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
        const val SHORT_PERSON_NAME = "^[a-z\\u4E00-\\u9FA5\\u0800-\\u4e00\\\\A-Z\\·]{2,30}$"

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
        const val REAL_PERSON_NAME = ("^[a-zA-Z\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]"
                + "[a-zA-Z0-9\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\.\\* ]{0,28}"
                + "[a-zA-Z\\u0020\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]$")

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
        const val BANK_ACCOUNT_HOLDER_NAME = ("^[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\*]"
                + "[a-zA-Z0-9\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\.()（）\\* ]{0,28}"
                + "[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\*()（）]$")

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
        const val PAYER_DISPLAY_NAME =
            "^[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00][a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00\\·\\. ]{0,28}[a-zA-Z\\u4E00-\\u9FA5\\u0800-\\u4e00]$"

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
        const val NICK_NAME = "^[a-zA-Z0-9\\u4E00-\\u9FA5\\u0800-\\u4e00]{3,15}$"
    }

    /**
     * Regexes related to general text.
     * Mainly constrain character sets, length and leading characters; not targeted at a specific business field.
     *
     * @author K
     * @since 1.0.0
     */
    object Text {

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
        const val TEXT_WITHOUT_SPECIAL_CHARS = "^[^&*=|{}<>/\\…—]*$"

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
        const val TEXT_1_TO_30_CHARS = "^(.){1,30}$"

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
        const val TEXT_STARTS_WITH_DIGIT = "^[0-9].*$"

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
        const val DIGITS_ONLY_OPTIONAL_EMPTY = "^[0-9]*$"

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
        const val HAN_LATIN_ALNUM = "^[0-9a-zA-Z\\u4e00-\\u9fa5]+$"

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
         * `/kudos-base`
         * `/kudos-base/api/v1`
         * `/kudos-base/api/v1/`
         */
        const val CONTEXT = "^/(?:[a-z0-9]+(?:-[a-z0-9]+)*/?)*$"
    }

    /**
     * Regexes related to character sets.
     * Used to validate that a string contains only certain kinds of characters (letters, digits, hyphens, underscores, etc.).
     *
     * @author K
     * @since 1.0.0
     */
    object CharacterSet {

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
        const val SINGLE_CHAR_REPEATED = "^(.)\\1+$"

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
        const val DIGITS_NON_EMPTY = "^\\d+$"

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
        const val LATIN_LETTERS_ONLY = "^[a-zA-Z]+$"

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
        const val LATIN_LOWERCASE_ONLY = "^[a-z]+$"

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
        const val LOWERCASE_ALNUM_NOT_ALL_DIGITS = "^(?!\\d+$)[\\da-z]+$"

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
        const val ASCII_DIGITS_ONLY = "^[0-9]+$"

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
        const val LATIN_ALNUM_ONLY = "^[a-zA-Z0-9]+$"

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
        const val LATIN_ALNUM_DASH_UNDERSCORE = "^[a-zA-Z0-9_-]+$"

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
        const val RELAXED_VAR_NAME = "^[A-Za-z_][A-Za-z0-9_-]*$"

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
        const val VAR_NAME = "^[A-Za-z_][A-Za-z0-9_]*$"

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
        const val SLUG_KEBAB_LOWERCASE = "^[a-z0-9]+(?:-[a-z0-9]+)*$"
    }

    /**
     * Regexes related to security.
     * Includes login passwords, PIN codes and the rule set used for password strength judgement.
     *
     * @author K
     * @since 1.0.0
     */
    object Security {

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
        const val LOGIN_PASSWORD = "^[A-Za-z0-9~\\\\\\-!@#$%^&*()_+\\{\\}\\[\\]|\\:;'\"<>,./?]{6,20}$"

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
        const val SECURITY_PIN_SIX_DIGITS = "^[0-9]{6}$"

        /**
         * Rules:
         * "Letters only" category used in password strength classification
         * Identical to [CharacterSet.LATIN_LETTERS_ONLY]
         *
         * Examples:
         * `abc`
         * `OnlyText`
         * `Password`
         */
        const val PASSWORD_STRENGTH_LETTERS_ONLY = CharacterSet.LATIN_LETTERS_ONLY

        /**
         * Rules:
         * "Digits only" category used in password strength classification
         * Identical to [CharacterSet.ASCII_DIGITS_ONLY]
         *
         * Examples:
         * `123456`
         * `987654`
         * `20260403`
         */
        const val PASSWORD_STRENGTH_DIGITS_ONLY = CharacterSet.ASCII_DIGITS_ONLY

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
        const val PASSWORD_STRENGTH_LETTERS_AND_DIGITS = "^[0-9a-zA-Z]+$"

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
        const val PASSWORD_STRENGTH_WITH_SYMBOLS =
            "^[A-Za-z0-9~!@#$%^&*()_+\\\\{\\\\}\\\\[\\\\]|\\\\:;\\'\\\"<>,./?]+$"
    }

    /**
     * Regexes related to numeric text.
     * Includes integers, positive numbers, amounts, percentages and scores expressed as strings.
     *
     * @author K
     * @since 1.0.0
     */
    object Numeric {

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
        const val POSITIVE_INT_TEXT = "^[1-9]\\d*$"

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
        const val SCORE_OR_HANDICAP_TEXT =
            "^(\\-?)((?:[0-9]{1,4})(?:\\.\\d{1,2})?)(\\/?|\\-?)((?:[0-9]{1,4}|0)(?:\\.\\d{1,2})?)?$"

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
        const val POSITIVE_DECIMAL_TEXT = "^(?!(0[0-9]{0,}$))[0-9]{1,}[.]{0,}[0-9]{0,}$"

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
        const val POSITIVE_NUMBER_TEXT = "^([1-9]\\d*\\.?\\d*)|(0\\.\\d*[1-9])$"

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
        const val SIGNED_INTEGER_TEXT = "^(-|\\+)?\\d+$"

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
        const val BTC_AMOUNT_TEXT = "^(?!0+(?:\\.0+)?$)(((?:[1-9]\\d*(\\.\\d{1,8})?))|(0\\.\\d{1,5}))?$"

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
        const val EMPTY_OR_POSITIVE_INT_TEXT = "^([0-9]*[1-9][0-9]*)?$"

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
        const val DIGITS_AT_MOST_9 = "^\\d{0,9}$"

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
        const val SIGNED_AMOUNT_LOOSE = "^\\-?([1-9]\\d{0,9}|0)([.]?|(\\.\\d{1,2})?)$"

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
        const val AMOUNT_NONZERO_TWO_DECIMALS = "^(?!0+(?:\\.0+)?$)(?:[1-9]\\d*|0)(?:\\.\\d{1,2})?$"

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
        const val PERCENT_INTEGER_0_100 = "^(?:100|[1-9]\\d?|0)$"
    }

    /**
     * Regexes related to networking and protocols.
     * Covers IPv4, IPv6, URL, JDBC, MAC, port, domain, CIDR and the like.
     *
     * @author K
     * @since 1.0.0
     */
    object Network {

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
        const val IPV4 = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"

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
        const val IPV4_FULL = "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|0\\d\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|0\\d\\d)$"

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
        const val IPV4_SEMICOLON_LIST =
            "^((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3})(;((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}))*$"

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
        const val IPV6_FULL = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"

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
        const val IPV6_COMPACT =
            "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"

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
        const val DOMAIN_LIST_COMMA_SEPARATED =
            "^(([a-zA-Z0-9]+\\.)+([a-zA-Z0-9]+){1}\\,)*(([a-zA-Z0-9]+\\.)+([a-zA-Z0-9]+){1}){1}$"

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
        const val MAC_ADDRESS_COLON_OR_HYPHEN = "^(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$"

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
        const val NETWORK_PORT_1_65535 =
            "^(?:[1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$"

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
        const val IPV4_CIDR_NOTATION =
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\/(?:3[0-2]|[12]?\\d)$"

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
        const val JDBC_URL = "^jdbc:[a-zA-Z0-9][a-zA-Z0-9+._-]*:\\S+$"

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
        const val DOMAIN = "^(localhost|(?=.{1,253}$)(?!-)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63})$"
    }

    /**
     * Business-specific regexes.
     * Contains fields obviously tied to the business domain such as bank card numbers, player accounts and site ID lists.
     *
     * @author K
     * @since 1.0.0
     */
    object Business {

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
        const val BANK_CARD_NUMBER = "^[0-9]{10,25}$"

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
        const val SITE_IDS_COMMA_SEPARATED = "^\\d+(,\\d+)*$|^\\d+$|^(\\s)*$"

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
        const val GAME_PLAYER_ACCOUNT = "^[a-zA-Z0-9_\\$][a-zA-Z0-9_]{3,14}$"
    }

    /**
     * Regexes related to common formats.
     * Includes UUID, date, time, postal code, hex color and other reusable standardized formats.
     *
     * @author K
     * @since 1.0.0
     */
    object Format {

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
        const val UUID_HYPHENATED = "^\\w{8}(-\\w{4}){3}-\\w{12}$"

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
        const val DATE_ISO_YYYY_MM_DD = "^(?:\\d{4})-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$"

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
        const val TIME_24H_MM_OPTIONAL_SS = "^(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?$"

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
        const val CN_MAINLAND_POSTAL_CODE = "^\\d{6}$"

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
        const val HEX_COLOR_CSS = "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"
    }

}
