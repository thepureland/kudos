#!/usr/bin/env python3
"""Restructure *-ms-*-core packages by business module. Run once."""
from __future__ import annotations

import os
import re
import shutil
from pathlib import Path

KUDOS = Path(__file__).resolve().parent.parent


def pkg_for(base: str, rel: Path) -> str:
    parts = list(rel.parts[:-1])
    return base + ("." + ".".join(parts) if parts else "")


def replace_package(text: str, new_pkg: str) -> str:
    return re.sub(r"^package\s+[\w.]+\s*$", f"package {new_pkg}", text, count=1, flags=re.MULTILINE)


def sys_module(stem: str) -> str:
    ex = {
        "SysAutoConfiguration": "platform",
        "CacheConfigProvider": "platform",
        "CrudLogSyncSupport": "platform",
        "AccessRuleIpsBySubSysAndTenantIdCache": "accessrule",
        "DomainByNameCache": "domain",
        "ParamByModuleAndNameCache": "param",
        "TenantByIdCache": "tenant",
        "DictItemCodeFinder": "dict",
    }
    if stem in ex:
        return ex[stem]
    pairs = [
        ("VSysAccessRuleWithIp", "accessrule"),
        ("AccessRuleIps", "accessrule"),
        ("AccessRuleIp", "accessrule"),
        ("AccessRule", "accessrule"),
        ("VSysDictItem", "dict"),
        ("SubSystemMicroService", "microservice"),
        ("TenantSystem", "tenant"),
        ("TenantResource", "tenant"),
        ("TenantLocale", "tenant"),
        ("DictItem", "dict"),
        ("Dict", "dict"),
        ("DataSource", "datasource"),
        ("MicroService", "microservice"),
        ("Resource", "resource"),
        ("I18N", "i18n"),
        ("I18n", "i18n"),
        ("Param", "param"),
        ("Domain", "domain"),
        ("Cache", "cache"),
        ("System", "system"),
        ("Tenant", "tenant"),
    ]
    for pat, mod in pairs:
        if pat in stem:
            return mod
    return "platform"


def user_module(stem: str) -> str:
    if stem == "UserAutoConfiguration":
        return "platform"
    pairs = [
        ("UserAccountThird", "user"),
        ("AccountThirdByUserIdAndProviderCodeCache", "user"),
        ("UserAccountProtection", "protection"),
        ("UserLoginRememberMe", "loginremember"),
        ("RememberMe", "loginremember"),
        ("UserContactWay", "contact"),
        ("UserOrgUser", "orguser"),
        ("UserOrg", "org"),
        ("UserLogLogin", "loglogin"),
        ("UserAccount", "user"),
    ]
    for pat, mod in pairs:
        if pat in stem:
            return mod
    return "platform"


def auth_module(stem: str) -> str:
    if stem == "AuthAutoConfiguration":
        return "platform"
    ex = {
        "ResourceIdsByTenantIdAndUsernameCache": "platform",
        "ResourceIdsByRoleIdCache": "platform",
        "ResourceIdsByTenantIdAndRoleCodeCache": "platform",
        "ResourceIdsByUserIdCache": "platform",
        "ResourceIdsByTenantIdAndGroupCodeCache": "platform",
    }
    if stem in ex:
        return ex[stem]
    pairs = [
        ("AuthGroupUser", "groupuser"),
        ("AuthGroupRole", "group"),
        ("AuthRoleResource", "roleresource"),
        ("AuthRoleUser", "roleuser"),
        ("AuthGroup", "group"),
        ("AuthRole", "role"),
        ("GroupIdsByUserIdCache", "group"),
        ("UserIdsByGroupIdCache", "group"),
        ("RoleIdsByUserIdCache", "role"),
        ("UserIdsByRoleIdCache", "role"),
        ("UserIdsByTenantIdAndRoleCodeCache", "role"),
        ("UserIdsByTenantIdAndGroupCodeCache", "group"),
    ]
    for pat, mod in pairs:
        if pat in stem:
            return mod
    return "platform"


def msg_module(stem: str) -> str:
    if stem == "MsgAutoConfiguration":
        return "platform"
    pairs = [
        ("MsgReceiverGroup", "receivergroup"),
        ("MsgTemplate", "template"),
        ("MsgInstance", "instance"),
        ("MsgReceive", "receive"),
        ("MsgSend", "send"),
    ]
    for pat, mod in pairs:
        if pat in stem:
            return mod
    return "platform"


def process_roots(base_pkg: str, roots: list[Path], module_fn) -> list[tuple[str, str]]:
    repl: list[tuple[str, str]] = []
    for root in roots:
        if not root.is_dir():
            continue
        files = sorted(root.rglob("*.kt"))
        for kt in files:
            rel = kt.relative_to(root)
            stem = kt.stem
            module = module_fn(stem)
            target_rel = Path(module, *rel.parts)
            dest = root / target_rel
            if dest.resolve() == kt.resolve():
                continue
            dest.parent.mkdir(parents=True, exist_ok=True)
            if dest.exists():
                raise SystemExit(f"Collision: {dest}")
            old_pkg = pkg_for(base_pkg, rel)
            shutil.move(str(kt), str(dest))
            new_pkg = pkg_for(base_pkg, target_rel)
            text = dest.read_text(encoding="utf-8")
            dest.write_text(replace_package(text, new_pkg), encoding="utf-8")
            repl.append((f"{old_pkg}.{stem}", f"{new_pkg}.{stem}"))

        # prune empty dirs
        for dirpath, _, _ in os.walk(root, topdown=False):
            p = Path(dirpath)
            if p == root:
                continue
            try:
                if not any(p.iterdir()):
                    p.rmdir()
            except OSError:
                pass
    return repl


def main() -> None:
    all_repl: list[tuple[str, str]] = []

    all_repl += process_roots(
        "io.kudos.ms.sys.core",
        [
            KUDOS / "kudos-ms/kudos-ms-sys/kudos-ms-sys-core/src/io/kudos/ms/sys/core",
            KUDOS
            / "kudos-ms/kudos-ms-sys/kudos-ms-sys-core/test-src/io/kudos/ms/sys/core",
        ],
        sys_module,
    )
    all_repl += process_roots(
        "io.kudos.ms.user.core",
        [
            KUDOS / "kudos-ms/kudos-ms-user/kudos-ms-user-core/src/io/kudos/ms/user/core",
            KUDOS
            / "kudos-ms/kudos-ms-user/kudos-ms-user-core/test-src/io/kudos/ms/user/core",
        ],
        user_module,
    )
    all_repl += process_roots(
        "io.kudos.ms.auth.core",
        [
            KUDOS / "kudos-ms/kudos-ms-auth/kudos-ms-auth-core/src/io/kudos/ms/auth/core",
            KUDOS
            / "kudos-ms/kudos-ms-auth/kudos-ms-auth-core/test-src/io/kudos/ms/auth/core",
        ],
        auth_module,
    )
    all_repl += process_roots(
        "io.kudos.ms.msg.core",
        [
            KUDOS / "kudos-ms/kudos-ms-msg/kudos-ms-msg-core/src/io/kudos/ms/msg/core",
        ],
        msg_module,
    )

    seen: set[tuple[str, str]] = set()
    ordered: list[tuple[str, str]] = []
    for o, n in sorted(all_repl, key=lambda x: len(x[0]), reverse=True):
        if (o, n) not in seen and o != n:
            seen.add((o, n))
            ordered.append((o, n))

    for kt in KUDOS.rglob("*.kt"):
        t = kt.read_text(encoding="utf-8")
        orig = t
        for old, new in ordered:
            t = t.replace(old, new)
        if t != orig:
            kt.write_text(t, encoding="utf-8")

    print("OK replacements:", len(ordered))


if __name__ == "__main__":
    main()
