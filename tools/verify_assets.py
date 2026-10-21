#!/usr/bin/env python3
"""Cross-check WebDisplays 1.20.1 resources against the registry names."""
import json
import os
import sys

ROOT = os.environ.get("WD_RESOURCES") or os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common", "src", "main", "resources")
)
ASSETS = os.path.join(ROOT, "assets")
MOD = "webdisplays"
BASE = os.path.join(ASSETS, MOD)

BLOCKS = ["screen", "peripheral"]

# Plain Item subclasses: the translation key is item.<mod>.<name>.
ITEMS = [
    "screen_configurator", "linker", "minepad", "laser_pointer",
    "ownership_thief", "upgrade", "craft_component",
]

# BlockItem subclasses. They use the block translation key, and PeripheralItem
# overrides getName() to append the variant, so the key is block.<mod>.<block>.<kind>.
BLOCK_ITEMS = {
    "screen": "block.webdisplays.screen",
    "peripheral_keyboard": "block.webdisplays.peripheral.keyboard",
    "peripheral_remotectrl": "block.webdisplays.peripheral.remotectrl",
    "peripheral_redstonectrl": "block.webdisplays.peripheral.redstonectrl",
    "peripheral_server": "block.webdisplays.peripheral.server",
}

# Variant names still read at runtime by UpgradeItem / CraftComponentItem.
LIVE_VARIANT_PREFIXES = (
    "item.webdisplays.craftcomp.",
    "item.webdisplays.upgrade.",
)

problems = []


def rel(p):
    return os.path.relpath(p, ROOT).replace("\\", "/")


def resolve(path, kind):
    """kind is 'model' or 'texture'. Returns (checked, full_path)."""
    if ":" not in path:
        ns, rest = "minecraft", path      # no namespace means vanilla
    else:
        ns, rest = path.split(":", 1)
    if ns == "minecraft":
        return False, None          # vanilla assets, not ours to verify
    if kind == "model":
        return True, os.path.join(ASSETS, ns, "models", rest + ".json")
    return True, os.path.join(ASSETS, ns, "textures", rest + ".png")


def load_json(p):
    try:
        with open(p, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        problems.append(f"[JSON 语法] {rel(p)}: {e}")
        return None


# --- 1. blockstate files ---
print("=== 1. blockstate 存在性 ===")
for b in BLOCKS:
    p = os.path.join(BASE, "blockstates", b + ".json")
    if os.path.isfile(p):
        print(f"  OK   blockstates/{b}.json")
    else:
        print(f"  MISS blockstates/{b}.json")
        problems.append(f"缺少 blockstate: {b}.json")

# --- 2. blockstate -> model ---
print("\n=== 2. blockstate 引用的 model ===")
for b in BLOCKS:
    p = os.path.join(BASE, "blockstates", b + ".json")
    if not os.path.isfile(p):
        continue
    data = load_json(p)
    if not data:
        continue
    seen = set()
    for variant, body in data.get("variants", {}).items():
        models = []
        if isinstance(body, list):
            models = [x.get("model") for x in body]
        elif isinstance(body, dict):
            models = [body.get("model")]
        for m in models:
            if not m or m in seen:
                continue
            seen.add(m)
            checked, full = resolve(m, "model")
            if not checked:
                continue
            ok = os.path.isfile(full)
            print(f"  {'OK  ' if ok else 'MISS'} {b}: {variant} -> {m}")
            if not ok:
                problems.append(f"blockstate {b} 引用了不存在的模型 {m}")

# --- 3. item models ---
print("\n=== 3. item model 存在性 ===")
for it in ITEMS:
    p = os.path.join(BASE, "models", "item", it + ".json")
    if os.path.isfile(p):
        print(f"  OK   models/item/{it}.json")
    else:
        print(f"  MISS models/item/{it}.json")
        problems.append(f"缺少 item model: {it}.json")

# --- 4. model parent / textures ---
print("\n=== 4. model 的 parent 与 texture 引用 ===")
for sub in ("block", "item"):
    d = os.path.join(BASE, "models", sub)
    if not os.path.isdir(d):
        continue
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".json"):
            continue
        p = os.path.join(d, fn)
        data = load_json(p)
        if not data:
            continue
        parent = data.get("parent")
        if parent:
            checked, full = resolve(parent, "model")
            if checked and not os.path.isfile(full):
                print(f"  MISS parent {sub}/{fn} -> {parent}")
                problems.append(f"{sub}/{fn} 的 parent 不存在: {parent}")
        for k, tex in (data.get("textures") or {}).items():
            if tex.startswith("#"):
                continue
            checked, full = resolve(tex, "texture")
            if checked and not os.path.isfile(full):
                print(f"  MISS tex    {sub}/{fn} [{k}] -> {tex}")
                problems.append(f"{sub}/{fn} 的贴图不存在: {tex}")

# --- 5. lang ---
print("\n=== 5. 语言文件 ===")
langdir = os.path.join(BASE, "lang")
langs = sorted(f for f in os.listdir(langdir) if f.endswith(".json"))
print(f"  语言文件: {', '.join(langs)}")
for lf in langs:
    lang = load_json(os.path.join(langdir, lf)) or {}
    keys = set(lang.keys())
    missing = []
    for name in BLOCKS:
        if f"block.{MOD}.{name}" not in keys:
            missing.append(f"block.{MOD}.{name}")
    for name in ITEMS:
        if f"item.{MOD}.{name}" not in keys:
            missing.append(f"item.{MOD}.{name}")
    for item, key in BLOCK_ITEMS.items():
        if key not in keys:
            missing.append(f"{key}   (block item {item})")
    if missing:
        print(f"  {lf}: 缺 {len(missing)} 个键")
        for m in missing:
            problems.append(f"{lf} 缺少 {m}")
    else:
        print(f"  {lf}: 注册项翻译键齐全")
    # informational: leftover 1.12.2 keys
    stale = sorted(
        k for k in keys
        if (k.startswith("tile.") or k.endswith(".name"))
        and not k.startswith(LIVE_VARIANT_PREFIXES)
    )
    if stale:
        print(f"  {lf}: {len(stale)} 个 1.12.2 遗留键（不会被读取，仅提示）")

# --- 6. sounds ---
print("\n=== 6. sounds.json ===")
sp = os.path.join(BASE, "sounds.json")
if os.path.isfile(sp):
    sd = load_json(sp) or {}
    for ev in ("keyboard_type", "upgrade_add", "upgrade_del", "screencfg_open", "server", "ironic"):
        if ev not in sd:
            print(f"  MISS 事件 {ev}")
            problems.append(f"sounds.json 缺少事件 {ev}")
            continue
        entries = sd[ev].get("sounds", [])
        names = []
        for s in entries:
            if isinstance(s, str):
                names.append(s)
            elif isinstance(s, dict):
                names.append(s.get("name"))
        bad = 0
        for f in names:
            if not f:
                continue
            checked, full = resolve(f, "texture")   # same ns/rest layout
            full = full.replace("textures", "sounds").replace(".png", ".ogg") if full else None
            if checked and not os.path.isfile(full):
                print(f"  MISS {ev} -> {f}")
                problems.append(f"sounds.json 事件 {ev} 引用的音频不存在: {f}")
                bad += 1
        if not bad:
            print(f"  OK   {ev} ({len(names)} 个音频)")
else:
    problems.append("缺少 sounds.json")

# --- summary ---
print("\n" + "=" * 62)
if problems:
    print(f"发现 {len(problems)} 个问题:")
    for p in problems:
        print("  - " + p)
    sys.exit(1)
print("全部通过")
