# 壁纸批量压缩：pics/background（839MB 原图）→ deploy/web/wallpapers（webp ~200KB/张）
# 规则：宽度≤1600 不放大；webp quality=82；转换失败则原样拷贝并保留原文件名。
# 同时生成 deploy/web/wallpapers-manifest.json（保持原分类顺序，扩展名按转换结果改写）。
# 幂等：输出已存在且不比输入旧则跳过。用法：python compress_wallpapers.py
import json
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(r"D:\todo")
SRC = ROOT / "pics" / "background"
OUT = ROOT / "deploy" / "web" / "wallpapers"
MANIFEST_IN = ROOT / "web-user" / "public" / "wallpapers-manifest.json"
MANIFEST_OUT = ROOT / "deploy" / "web" / "wallpapers-manifest.json"

MAX_W = 1600
QUALITY = 82


def convert_one(src: Path, dst: Path) -> bool:
    """返回 True=转成 webp（dst 指向 .webp）；False=原样拷贝（dst 保持原名）"""
    if dst.exists() and dst.stat().st_mtime >= src.stat().st_mtime:
        return dst.suffix == ".webp"
    dst.parent.mkdir(parents=True, exist_ok=True)
    try:
        with Image.open(src) as im:
            im = im.convert("RGB")
            if im.width > MAX_W:
                ratio = MAX_W / im.width
                im = im.resize((MAX_W, max(1, int(im.height * ratio))), Image.LANCZOS)
            webp = src.with_suffix(".webp")
            out = dst.with_suffix(".webp") if dst.suffix != ".webp" else dst
            im.save(out, "WEBP", quality=QUALITY, method=5)
            return True
    except Exception as e:  # noqa: BLE001 — 单张失败不拖垮全批
        print(f"  [跳过转换] {src.name}: {e}", file=sys.stderr)
        shutil.copy2(src, dst.with_suffix(src.suffix) if dst.suffix != src.suffix else dst)
        return False


def main() -> None:
    manifest = json.loads(MANIFEST_IN.read_text(encoding="utf-8"))
    total_src = total_out = 0
    n_webp = n_copy = 0

    for cat in manifest["categories"]:
        name = cat["name"]
        files = []
        for fname in cat["files"]:
            src = SRC / name / fname
            if not src.exists():
                print(f"  [清单有但缺文件] {name}/{fname}", file=sys.stderr)
                continue
            ok = convert_one(src, OUT / name / fname)
            total_src += src.stat().st_size
            if ok:
                out_file = OUT / name / (Path(fname).stem + ".webp")
                files.append(out_file.name)
                n_webp += 1
            else:
                out_file = OUT / name / fname
                files.append(fname)
                n_copy += 1
            total_out += out_file.stat().st_size
        cat["files"] = files

    manifest["generatedAt"] = "2026-10-30T00:00:00.000Z"
    MANIFEST_OUT.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_OUT.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")

    print(f"完成：{n_webp} 张转 webp，{n_copy} 张原样拷贝")
    print(f"体积：{total_src / 1024 / 1024:.1f}MB -> {total_out / 1024 / 1024:.1f}MB"
          f"（{100 * total_out / max(1, total_src):.0f}%）")


if __name__ == "__main__":
    main()
