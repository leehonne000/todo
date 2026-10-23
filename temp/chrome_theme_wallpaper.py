#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
chrome_theme_wallpaper.py —— 按 Chrome 网上应用店「主题」分类批量抓取壁纸

原理
----
1) 抓取分类页 (chromewebstore.google.com/category/themes/...)，解析出主题条目 ID；
2) 通过官方 CRX 下载端点拿到主题包（.crx 本质是 zip），解出包里的
   theme_ntp_background 等图片 —— 这就是主题真正使用的完整壁纸；
3) 拿不到 CRX 时，回退抓主题详情页里的高清预览图（带浏览器外框，属降级方案）。

依赖：仅 Python 3.8+ 标准库，无需 pip 安装任何东西。

典型用法
--------
  # 1. 先看有哪些分类（自动从商店页面发现真实 URL）
  python chrome_theme_wallpaper.py --list-categories --proxy auto

  # 2. 抓全部分类，每个分类 10 张
  python chrome_theme_wallpaper.py --categories all --limit 10 --proxy auto

  # 3. 只抓指定分类（中文名或 URL 片段均可）
  python chrome_theme_wallpaper.py --categories 动物 太空 --limit 20 -o D:/wallpapers --proxy http://127.0.0.1:7890

  # 4. 直接给分类页完整 URL（最稳，页面上地址栏复制即可）
  python chrome_theme_wallpaper.py --category-url https://chromewebstore.google.com/category/themes/xxx --limit 30

  # 5. 完全离线：把已下载好的 .crx / .zip 主题包丢进目录，批量解出壁纸
  python chrome_theme_wallpaper.py --from-crx D:/Downloads/crxs --category 动物

说明
----
* 支持 12 个分类（--list-categories 查看）；「艺术家系列」是集合落地页，
  脚本会自动跟进其中的 /collection/* 链接收集条目。
* 输出目录结构：<out>/<分类名>/<主题名>__<条目ID>.png
* 断点续传：<out>/_state.json 记录已完成条目，重复运行自动跳过。
* 抓取带随机延时与重试，尽量不影响商店服务；如需更快可调 --workers / --delay。
"""

from __future__ import annotations

import argparse
import io
import json
import os
import random
import re
import struct
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from html import unescape
from pathlib import Path

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
STORE = "https://chromewebstore.google.com"
CRX_ENDPOINT = ("https://clients2.google.com/service/update2/crx"
                "?response=redirect&prodversion=126.0&acceptformat=crx2,crx3"
                "&x=id%3D{item_id}%26installsource%3Dondemand%26uc")
TEST_URL = STORE + "/category/themes"

# 分类中文名 -> 商店分类页 slug（实测 2026-10 有效；艺术家系列是集合落地页，脚本会自动跟进 /collection/* 链接）
CATEGORIES = {
    "艺术家系列": "artist",
    "动物": "animals",
    "简约主义": "minimalist",
    "汽车": "cars",
    "深色与黑色主题": "dark",
    "太空": "space",
    "颜色": "colors",
    "艺术与设计": "art",
    "游戏与动漫": "games",
    "娱乐": "entertainment",
    "自然与景观": "nature",
    "其他": "other",
}

ITEM_RE = re.compile(r"/detail/([^/\"'?&#\s]+)/([a-p]{32})")
REC_RE = re.compile(
    r'"([a-p]{32})\s*",\s*"https://lh3\.googleusercontent\.com/[^"]+"\s*,\s*"([^"]{1,120})"')
BARE_ID_RE = re.compile(r'"([a-p]{32})"')
COLL_RE = re.compile(r"/collection/([A-Za-z0-9_\-]+)")
IMG_URL_RE = re.compile(r"https://lh3\.googleusercontent\.com/[A-Za-z0-9_\-=/\.]+")
TAG_RE = re.compile(r"<[^>]+>")

PROXY_CANDIDATES = [
    "http://127.0.0.1:7890", "http://127.0.0.1:7897", "http://127.0.0.1:7891",
    "http://127.0.0.1:7892",
    "http://127.0.0.1:1080", "http://127.0.0.1:10808", "http://127.0.0.1:10809",
    "http://127.0.0.1:8080", "http://127.0.0.1:8118", "http://127.0.0.1:33210",
]


# ---------------------------------------------------------------- 基础网络层

def make_opener(proxy: str | None) -> urllib.request.OpenerDirector:
    handlers = [urllib.request.HTTPSHandler(), urllib.request.HTTPHandler()]
    if proxy:
        handlers.insert(0, urllib.request.ProxyHandler({"http": proxy, "https": proxy}))
    opener = urllib.request.build_opener(*handlers)
    opener.addheaders = [("User-Agent", UA),
                         ("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")]
    return opener


def http_get(opener, url: str, timeout: int = 30, retries: int = 3) -> bytes:
    """GET 请求，带指数退避重试。"""
    last_err: Exception | None = None
    for i in range(retries):
        try:
            with opener.open(url, timeout=timeout) as resp:
                return resp.read()
        except Exception as e:  # noqa: BLE001 - 网络错误种类繁多，统一退避重试
            last_err = e
            if i < retries - 1:
                time.sleep(1.5 * (2 ** i) + random.random())
    raise RuntimeError(f"请求失败 {url}: {last_err}")


def resolve_proxy(arg: str | None) -> str | None:
    """--proxy 参数处理：显式地址 / auto 自动探测 / 环境变量。"""
    if arg and arg.lower() != "auto":
        return arg
    env = (os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy")
           or os.environ.get("HTTP_PROXY") or os.environ.get("http_proxy"))
    if arg is None:
        return env
    if env:
        print(f"[proxy] 使用环境变量代理: {env}")
        return env
    print("[proxy] 自动探测本机代理端口 ...")
    for cand in PROXY_CANDIDATES:
        try:
            opener = make_opener(cand)
            req = urllib.request.Request(TEST_URL, method="HEAD")
            opener.open(req, timeout=6).close()
            print(f"[proxy] 命中可用代理: {cand}")
            return cand
        except Exception:  # noqa: BLE001
            continue
    return None


def ensure_reachable(opener) -> None:
    try:
        http_get(opener, TEST_URL, timeout=20, retries=1)
    except Exception as e:  # noqa: BLE001
        sys.exit(
            "\n[!] 连不上 Chrome 网上应用店（Google 域名被网络环境拦截）：\n"
            f"    {e}\n\n"
            "可选做法：\n"
            "  1) 开启代理/VPN 后重试：  --proxy http://127.0.0.1:7890  或  --proxy auto\n"
            "  2) 换一台能访问 Google 的机器（含代理环境）运行本脚本；\n"
            "  3) 完全离线：自行下载主题 .crx 文件后用  --from-crx <目录>  解包壁纸。\n"
        )


# ---------------------------------------------------------------- 解析工具

def clean_text(html: str) -> str:
    return unescape(TAG_RE.sub("", html)).strip()


def sanitize(name: str, limit: int = 80) -> str:
    name = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "_", name).strip(" .")
    return (name or "unnamed")[:limit]


def upgrade_gu_url(url: str) -> str:
    """把 googleusercontent 缩略图 URL 升级成大图。"""
    if "=" in url:
        head, _, tail = url.rpartition("=")
        if re.fullmatch(r"[swh][\w\-.]*", tail):
            return head + "=w2560"
    return url + "=w2560"


def sniff_ext(data: bytes) -> str:
    if data.startswith(b"\x89PNG"):
        return "png"
    if data.startswith(b"\xff\xd8\xff"):
        return "jpg"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "webp"
    if data.startswith(b"GIF8"):
        return "gif"
    return "bin"


# 小于该分辨率的图片视为「非壁纸」（纯色主题包里只有图标/工具条碎图）
MIN_WALLPAPER = (1280, 720)


def image_dims(data: bytes) -> tuple[int, int] | None:
    """解析 PNG/JPEG/WEBP 的宽高，失败返回 None。"""
    try:
        if data.startswith(b"\x89PNG"):
            return struct.unpack(">II", data[16:24])
        if data.startswith(b"\xff\xd8\xff"):
            i = 2
            while i < len(data) - 9:
                if data[i] != 0xFF:
                    i += 1
                    continue
                marker = data[i + 1]
                if marker in (0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
                              0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF):
                    h, w = struct.unpack(">HH", data[i + 5:i + 9])
                    return w, h
                if marker in (0xD8, 0x01) or 0xD0 <= marker <= 0xD7:
                    i += 2
                else:
                    i += 2 + struct.unpack(">H", data[i + 2:i + 4])[0]
        if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
            if data[12:16] == b"VP8X":
                w = int.from_bytes(data[24:27], "little") + 1
                h = int.from_bytes(data[27:30], "little") + 1
                return w, h
            if data[12:16] == b"VP8 ":
                w, h = struct.unpack("<HH", data[26:30])
                return w & 0x3FFF, h & 0x3FFF
            if data[12:16] == b"VP8L":
                bits = int.from_bytes(data[21:25], "little")
                return (bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1
    except Exception:  # noqa: BLE001
        return None
    return None


# ---------------------------------------------------------------- 分类 / 条目

def category_url(slug: str) -> str:
    return f"{STORE}/category/themes/{slug}"


def unescape_name(s: str) -> str:
    """数据块里的名称是 JSON 字符串字面量（含 \\u0026 等转义），还原成可读文本。"""
    try:
        return json.loads('"' + s + '"')
    except Exception:  # noqa: BLE001
        return s.replace("\\u0026", "&")


def parse_items(html: str) -> list[tuple[str, str | None]]:
    """从页面 HTML 解析 (item_id, 名称) 列表，保序去重。

    依次尝试三种数据形态：
    1) 数据块里的记录元组 "<id>", "<lh3图标>", "<名称>"  -> 名称最好；
    2) /detail/<slug>/<id> 链接                          -> 名称取 slug；
    3) 数据块里的裸 "<id>"（集合页常见）                 -> 名称留空。
    """
    text = html.replace("\\/", "/")
    out: dict[str, str | None] = {}
    for item_id, name in REC_RE.findall(text):
        out.setdefault(item_id, unescape_name(name))
    for slug, item_id in ITEM_RE.findall(text):
        out.setdefault(item_id, urllib.parse.unquote(slug))
    for item_id in BARE_ID_RE.findall(text):
        out.setdefault(item_id, None)
    return list(out.items())


def resolve_categories(wanted: list[str] | None) -> list[tuple[str, str]]:
    """把用户输入解析成 [(分类页URL, 分类名)]。"""
    if not wanted or wanted == ["all"]:
        wanted = list(CATEGORIES)
    targets: list[tuple[str, str]] = []
    for w in wanted:
        if w in CATEGORIES:
            targets.append((category_url(CATEGORIES[w]), w))
        elif w.lower().startswith("http"):
            label = clean_text(urllib.parse.unquote(w.rstrip("/").rsplit("/", 1)[-1]))
            targets.append((w, label))
        elif w in CATEGORIES.values():
            targets.append((category_url(w), w))
        else:
            print(f"[warn] 未知分类「{w}」，按 slug 处理；可用 --list-categories 查看全部")
            targets.append((category_url(w), w))
    return targets


# ---------------------------------------------------------------- 壁纸提取

def crx_to_zip(data: bytes) -> bytes:
    if data[:4] == b"Cr24":
        ver = struct.unpack("<I", data[4:8])[0]
        if ver == 2:
            pk_len, sig_len = struct.unpack("<II", data[8:16])
            return data[16 + pk_len + sig_len:]
        if ver == 3:
            hlen = struct.unpack("<I", data[8:12])[0]
            return data[12 + hlen:]
    idx = data.find(b"PK\x03\x04")
    if idx >= 0:
        return data[idx:]
    raise ValueError("无法识别的 CRX/ZIP 数据")


PREFERRED = ("theme_ntp_background", "theme_tab_background", "theme_frame",
             "theme_toolbar_background", "theme_button_background")


def extract_wallpaper(blob: bytes) -> bytes:
    """从主题包（crx/zip 字节）里挑出最像壁纸的图片字节。"""
    zf = zipfile.ZipFile(io.BytesIO(crx_to_zip(blob)))
    names = {n.lstrip("./"): n for n in zf.namelist()
             if not n.endswith("/")}

    manifest = None
    for n in zf.namelist():
        if n.rsplit("/", 1)[-1] == "manifest.json" and n.count("/") <= 1:
            try:
                manifest = json.loads(zf.read(n).decode("utf-8", "replace"))
            except Exception:  # noqa: BLE001
                manifest = None
            break

    images = ((manifest or {}).get("theme") or {}).get("images") or {}
    for key in PREFERRED:
        path = str(images.get(key) or "").lstrip("./")
        if path in names:
            return zf.read(names[path])

    # 兜底：在所有图片里挑名字带 background/ntp 的，否则挑最大的
    imgs = [(n, names[n]) for n in names
            if Path(n).suffix.lower() in (".png", ".jpg", ".jpeg", ".webp", ".gif")]
    if not imgs:
        raise ValueError("主题包里没有找到图片")
    imgs.sort(key=lambda t: (0 if re.search(r"ntp|background", t[0], re.I) else 1,
                             -zf.getinfo(t[1]).file_size))
    return zf.read(imgs[0][1])


def preview_image(detail_html: str) -> str | None:
    """详情页里挑一张最大的预览图 URL（降级方案）。"""
    heads: dict[str, str] = {}
    for raw in IMG_URL_RE.findall(detail_html):
        raw = unescape(raw)
        head = raw.rpartition("=")[0] if "=" in raw else raw
        heads.setdefault(head, upgrade_gu_url(raw))
    if not heads:
        return None
    # 详情页首图通常是主题全景预览，出现在 HTML 较靠前的位置
    return next(iter(heads.values()))


# ---------------------------------------------------------------- 下载流程

class Crawler:
    def __init__(self, args):
        self.args = args
        self.proxy = resolve_proxy(args.proxy)
        self.opener = make_opener(self.proxy)
        self.out = Path(args.out).expanduser().resolve()
        self.out.mkdir(parents=True, exist_ok=True)
        self.state_path = self.out / "_state.json"
        self.state = self._load_state()
        self._lock = threading.Lock()

    def _load_state(self) -> dict:
        try:
            return json.loads(self.state_path.read_text("utf-8"))
        except Exception:  # noqa: BLE001
            return {"done": []}

    def _save_state(self) -> None:
        self.state_path.write_text(
            json.dumps(self.state, ensure_ascii=False, indent=1), "utf-8")

    # ---- 单个条目
    def grab_one(self, item_id: str, name: str | None, cat_dir: Path) -> str:
        display = name or item_id
        if item_id in self.state["done"]:
            return f"skip  {display}"
        stem = f"{sanitize(name)}__{item_id}" if name else item_id
        source = self.args.source

        def save(data: bytes) -> str:
            dims = image_dims(data)
            if dims and (dims[0] < MIN_WALLPAPER[0] or dims[1] < MIN_WALLPAPER[1]):
                with self._lock:  # 纯色/图标类主题没有真壁纸：记账跳过，不落盘
                    self.state["done"].append(item_id)
                    self._save_state()
                return f"tiny  {display}（{dims[0]}x{dims[1]}，非壁纸）"
            ext = sniff_ext(data)
            path = cat_dir / f"{stem}.{ext}"
            path.write_bytes(data)
            with self._lock:
                self.state["done"].append(item_id)
                self._save_state()
            return f"ok    {display} -> {path.name} ({len(data)//1024}KB)"

        if source in ("crx", "auto"):
            try:
                blob = http_get(self.opener, CRX_ENDPOINT.format(item_id=item_id),
                                timeout=60)
                return save(extract_wallpaper(blob))
            except Exception as e:  # noqa: BLE001
                if source == "crx":
                    return f"fail  {display}: CRX {e}"

        if source in ("preview", "auto"):
            try:
                detail = http_get(
                    self.opener, f"{STORE}/detail/{item_id}").decode(
                    "utf-8", "replace")
                url = preview_image(detail)
                if not url:
                    return f"fail  {display}: 详情页无预览图"
                return save(http_get(self.opener, url, timeout=60))
            except Exception as e:  # noqa: BLE001
                return f"fail  {display}: {e}"
        return f"fail  {display}: 无可用来源"

    def fetch_items(self, url: str) -> list[tuple[str, str | None]]:
        """抓分类/集合页条目；纯集合落地页（如艺术家系列）自动跟进 /collection/* 链接。"""
        html = http_get(self.opener, url).decode("utf-8", "replace")
        items = parse_items(html)
        if not items:
            seen: set[str] = set()
            for coll in COLL_RE.findall(html.replace("\\/", "/")):
                if coll in seen:
                    continue
                seen.add(coll)
                try:
                    sub = http_get(self.opener,
                                   f"{STORE}/collection/{coll}").decode("utf-8", "replace")
                    found = parse_items(sub)
                    print(f"    [集合 {coll}] {len(found)} 个条目")
                    items += found
                except Exception as e:  # noqa: BLE001
                    print(f"    [集合 {coll}] 抓取失败: {e}")
        out: dict[str, str | None] = {}
        for item_id, name in items:
            out.setdefault(item_id, name)
        return list(out.items())

    @staticmethod
    def count_good(cat_dir: Path) -> int:
        """目录里已有的、达到壁纸分辨率的图片数。"""
        n = 0
        for p in cat_dir.iterdir():
            if not p.is_file() or p.suffix.lower() not in (".png", ".jpg", ".jpeg", ".webp", ".gif"):
                continue
            dims = image_dims(p.read_bytes())
            if dims and dims[0] >= MIN_WALLPAPER[0] and dims[1] >= MIN_WALLPAPER[1]:
                n += 1
        return n

    def run(self) -> None:
        if self.args.from_crx:
            return self.run_local()
        if self.args.from_html:
            return self.run_from_html()

        if self.args.list_categories:
            print("支持的分类（中文名可直接用于 --categories）：")
            for zh, slug in CATEGORIES.items():
                print(f"  {zh:<8} {category_url(slug)}")
            return

        ensure_reachable(self.opener)

        targets: list[tuple[str, str]] = []
        for url in self.args.category_url or []:
            label = clean_text(urllib.parse.unquote(url.rstrip("/").rsplit("/", 1)[-1]))
            targets.append((url, label))
        targets += resolve_categories(self.args.categories)
        if not targets:
            sys.exit("[!] 没有可抓的分类。用 --categories all 或 --category-url 指定。")

        total = 0
        for url, label in targets:
            cat_dir = self.out / sanitize(label)
            cat_dir.mkdir(parents=True, exist_ok=True)
            try:
                items = self.fetch_items(url)
            except Exception as e:  # noqa: BLE001
                print(f"[{label}] 分类页抓取失败: {e}")
                continue
            if not items:
                print(f"[{label}] 页面里没解析到条目")
                continue
            limit = self.args.limit
            good = self.count_good(cat_dir)
            if limit and good >= limit:
                print(f"\n=== {label} ：已有 {good} 张壁纸，跳过 ===")
                continue
            print(f"\n=== {label} ：{len(items)} 个主题可选，"
                  f"目标 {limit or '不限'} 张（已有 {good}）===")
            idx = 0
            with ThreadPoolExecutor(max_workers=self.args.workers) as pool:
                while idx < len(items):
                    batch = items[idx: idx + self.args.workers * 2]
                    idx += self.args.workers * 2
                    futs = [pool.submit(self.grab_one, i, n, cat_dir) for i, n in batch]
                    for fut in as_completed(futs):
                        msg = fut.result()
                        print(" ", msg)
                        if msg.startswith("ok"):
                            good += 1
                            total += 1
                        time.sleep(self.args.delay * random.random())
                    if limit and good >= limit:
                        break
        print(f"\n完成：共保存 {total} 张壁纸 -> {self.out}")

    # ---- 离线：本地 crx/zip 批量解包
    def run_local(self) -> None:
        root = Path(self.args.from_crx).expanduser().resolve()
        files = [p for p in root.rglob("*") if p.suffix.lower() in (".crx", ".zip")]
        if not files:
            sys.exit(f"[!] {root} 下没有 .crx/.zip 文件")
        cat = sanitize(self.args.category or root.name)
        cat_dir = self.out / cat
        cat_dir.mkdir(parents=True, exist_ok=True)
        n = 0
        for p in files:
            try:
                data = extract_wallpaper(p.read_bytes())
                ext = sniff_ext(data)
                (cat_dir / f"{sanitize(p.stem)}__local.{ext}").write_bytes(data)
                print(f"  ok    {p.name}")
                n += 1
            except Exception as e:  # noqa: BLE001
                print(f"  fail  {p.name}: {e}")
        print(f"\n完成：{n}/{len(files)} 张壁纸 -> {cat_dir}")

    # ---- 离线：本地保存的分类页 HTML（配合 --source preview 慎用）
    def run_from_html(self) -> None:
        html = Path(self.args.from_html).expanduser().read_text("utf-8", "replace")
        items = parse_items(html)
        if not items:
            sys.exit("[!] 本地 HTML 里没解析到 /detail/<slug>/<id> 条目")
        cat = sanitize(self.args.category or "saved")
        cat_dir = self.out / cat
        cat_dir.mkdir(parents=True, exist_ok=True)
        print(f"解析到 {len(items)} 个条目，开始下载 ...")
        with ThreadPoolExecutor(max_workers=self.args.workers) as pool:
            futs = [pool.submit(self.grab_one, i, s, cat_dir) for i, s in items]
            for fut in as_completed(futs):
                print(" ", fut.result())


# ---------------------------------------------------------------- 入口

def main() -> None:
    ap = argparse.ArgumentParser(
        description="按 Chrome 网上应用店主题分类批量抓取壁纸",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__)
    ap.add_argument("--categories", nargs="*", default=None,
                    help="分类中文名（可多个），或 all 抓全部")
    ap.add_argument("--category-url", action="append", default=[],
                    help="分类页完整 URL（可重复）")
    ap.add_argument("--category", default=None,
                    help="离线模式下输出分类名（--from-crx / --from-html 用）")
    ap.add_argument("-o", "--out", default="wallpapers", help="输出目录（默认 ./wallpapers）")
    ap.add_argument("--limit", type=int, default=12, help="每个分类最多抓几张（0=不限）")
    ap.add_argument("--source", choices=["auto", "crx", "preview"], default="auto",
                    help="壁纸来源：crx=主题包内真壁纸（默认优先），preview=详情页预览图")
    ap.add_argument("--proxy", default=None,
                    help="代理地址如 http://127.0.0.1:7890，或 auto 自动探测")
    ap.add_argument("--workers", type=int, default=3, help="并发数（默认 3）")
    ap.add_argument("--delay", type=float, default=0.8, help="请求间随机延时基数秒")
    ap.add_argument("--list-categories", action="store_true", help="只列出分类就退出")
    ap.add_argument("--from-crx", default=None, help="离线模式：本地 .crx/.zip 目录")
    ap.add_argument("--from-html", default=None, help="离线模式：本地保存的分类页 HTML")
    args = ap.parse_args()

    if not (args.list_categories or args.from_crx or args.from_html
            or args.categories or args.category_url):
        args.categories = ["all"]
    Crawler(args).run()


if __name__ == "__main__":
    main()
