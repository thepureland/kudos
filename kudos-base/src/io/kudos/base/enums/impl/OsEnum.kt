package io.kudos.base.enums.impl

/**
 * 操作系统枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class OsEnum {

    // 桌面/服务器
    MAC, WINDOWS, LINUX,

    // 移动
    ANDROID, IOS, HARMONY,

    // BSD家族
    FREEBSD, OPENBSD, NETBSD, DRAGONFLYBSD,

    // Solaris / Illumos
    SOLARIS, ILLUMOS,

    // IBM / UNIX 系
    AIX, HPUX,

    // Apple 其他平台
    TVOS, WATCHOS,

    OTHER

}