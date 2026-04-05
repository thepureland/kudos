#!/usr/bin/env python3
"""Restructure *-ms-*-common packages: common.<module>.{api,consts,enums,vo} + platform.*"""
from __future__ import annotations

import re
import shutil
from pathlib import Path

# kudos-ms/restructure_common_layout.py -> kudos is parent.parent
KUDOS_ROOT = Path(__file__).resolve().parent.parent

CONFIGS: list[dict] = [
    {
        "base_pkg": "io.kudos.ms.user.common",
        "common_dir": KUDOS_ROOT
        / "kudos-ms/kudos-ms-user/kudos-ms-user-common/src/io/kudos/ms/user/common",
        "api_dest": {
            "IUserAccountApi.kt": "user/api",
            "IUserAccountThirdApi.kt": "user/api",
            "IUserAccountProtectionApi.kt": "protection/api",
            "IUserContactWayApi.kt": "contact/api",
            "IUserOrgApi.kt": "org/api",
            "IUserLoginRememberMeApi.kt": "loginremember/api",
        },
    },
    {
        "base_pkg": "io.kudos.ms.auth.common",
        "common_dir": KUDOS_ROOT
        / "kudos-ms/kudos-ms-auth/kudos-ms-auth-common/src/io/kudos/ms/auth/common",
        "api_dest": {
            "IAuthRoleApi.kt": "role/api",
            "IPermittedResource.kt": "platform/api",
        },
    },
    {
        "base_pkg": "io.kudos.ms.msg.common",
        "common_dir": KUDOS_ROOT
        / "kudos-ms/kudos-ms-msg/kudos-ms-msg-common/src/io/kudos/ms/msg/common",
        "api_dest": {
            "IMsgSendApi.kt": "send/api",
            "IMsgInstanceApi.kt": "instance/api",
            "IMsgReceiveApi.kt": "receive/api",
            "IMsgReceiverGroupApi.kt": "receivergroup/api",
            "IMsgTemplateApi.kt": "template/api",
        },
    },
]


def pkg_for_file(base_pkg: str, rel_to_common: Path) -> str:
    parts = list(rel_to_common.parts[:-1])
    return base_pkg + ("." + ".".join(parts) if parts else "")


def restructure_one(cfg: dict) -> list[tuple[str, str]]:
    """Returns list of (old_prefix, new_prefix) for extra import replacements."""
    base_pkg = cfg["base_pkg"]
    common = Path(cfg["common_dir"])
    api_dest: dict[str, str] = cfg["api_dest"]

    if not common.is_dir():
        raise SystemExit(f"Missing {common}")

    vo_dir = common / "vo"
    enums_dir = common / "enums"
    api_dir = common / "api"
    consts_dir = common / "consts"
    validation_dir = common / "validation"

    modules: list[str] = []
    if vo_dir.is_dir():
        for p in vo_dir.iterdir():
            if p.is_dir():
                modules.append(p.name)
    modules = sorted(set(modules), key=len, reverse=True)

    for m in modules:
        src = vo_dir / m
        dst = common / m / "vo"
        if not src.is_dir():
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if dst.exists():
            raise SystemExit(f"Target exists: {dst}")
        shutil.move(str(src), str(dst))

    if api_dir.is_dir():
        for fname, dest in api_dest.items():
            src = api_dir / fname
            if not src.is_file():
                raise SystemExit(f"Missing {src}")
            ddir = common / dest
            ddir.mkdir(parents=True, exist_ok=True)
            shutil.move(str(src), str(ddir / fname))

    if enums_dir.is_dir():
        for sub in list(enums_dir.iterdir()):
            if not sub.is_dir():
                continue
            m = sub.name
            dst = common / m / "enums"
            if dst.exists():
                raise SystemExit(f"enums exists: {dst}")
            shutil.move(str(sub), str(dst))

    platform = common / "platform"
    platform.mkdir(parents=True, exist_ok=True)
    if consts_dir.is_dir():
        shutil.move(str(consts_dir), str(platform / "consts"))
    if validation_dir.is_dir():
        shutil.move(str(validation_dir), str(platform / "validation"))

    for d in (api_dir, vo_dir, enums_dir):
        if d.exists() and d.is_dir() and not any(d.iterdir()):
            d.rmdir()

    extra: list[tuple[str, str]] = []
    prefix = base_pkg + "."

    for kt in common.rglob("*.kt"):
        rel = kt.relative_to(common)
        new_pkg = pkg_for_file(base_pkg, rel)
        text = kt.read_text(encoding="utf-8")
        text_new = re.sub(
            r"^package\s+[\w.]+\s*$",
            f"package {new_pkg}",
            text,
            count=1,
            flags=re.MULTILINE,
        )
        if text != text_new:
            kt.write_text(text_new, encoding="utf-8")

    # Build replacement tuples for imports (longest module names first)
    for m in modules:
        extra.append((f"{prefix}vo.{m}.", f"{prefix}{m}.vo."))
        extra.append((f"{prefix}enums.{m}.", f"{prefix}{m}.enums."))

    for fname, dest in api_dest.items():
        simple = fname.replace(".kt", "")
        extra.append(
            (
                f"{prefix}api.{simple}",
                f"{prefix}{dest.replace('/', '.')}.{simple}",
            )
        )

    extra.append((f"{prefix}consts.", f"{prefix}platform.consts."))
    extra.append((f"{prefix}validation.", f"{prefix}platform.validation."))

    return extra


def main() -> None:
    all_replacements: list[tuple[str, str]] = []
    for cfg in CONFIGS:
        all_replacements.extend(restructure_one(cfg))

    # Dedupe while preserving order (longer patterns should win — sort by len desc)
    seen: set[tuple[str, str]] = set()
    ordered: list[tuple[str, str]] = []
    for o, n in sorted(all_replacements, key=lambda x: len(x[0]), reverse=True):
        if (o, n) not in seen:
            seen.add((o, n))
            ordered.append((o, n))

    kt_files = list(KUDOS_ROOT.rglob("*.kt"))
    for kt in kt_files:
        text = kt.read_text(encoding="utf-8")
        orig = text
        for old, new in ordered:
            text = text.replace(old, new)
        if text != orig:
            kt.write_text(text, encoding="utf-8")

    print("OK:", len(ordered), "replacement rules applied across", len(kt_files), "kt files")


if __name__ == "__main__":
    main()
