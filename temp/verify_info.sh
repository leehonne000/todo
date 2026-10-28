#!/usr/bin/env bash
# 信息模块验证（契约 §9 三接口：overview / articles / article 详情）
# 重点：倒计时学习日口径、届数过滤、只含已发布+倒序、分页、草稿 3002、2001
set -u
OUT="D:/todo/temp/verify_info.txt"
API="http://localhost:8080/api"
EMAIL_A="test_info_a@example.com"
EMAIL_B="test_info_b@example.com"
PASS="Test-12345"
D="D:/todo/temp"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }

log "== 0. 等服务就绪 =="
for i in $(seq 1 36); do
  grep -q "Started ExamTimelineApplication" D:/todo/temp/server_boot.log 2>/dev/null && { log "服务已启动"; break; }
  [ "$i" -eq 36 ] && { log "FAIL: 超时"; exit 1; }
  sleep 5
done

cleanup_user() {
  local email="$1"
  local uid
  uid=$(sql "SELECT id FROM t_user WHERE email='$email';" | tail -1)
  if [ -n "$uid" ] && [ "$uid" != "id" ]; then
    sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$uid);
         DELETE FROM t_entry_image WHERE user_id=$uid;
         DELETE FROM t_entry WHERE user_id=$uid;
         DELETE FROM t_study_session WHERE user_id=$uid;
         DELETE FROM t_check_in WHERE user_id=$uid;
         DELETE FROM t_tag WHERE user_id=$uid;
         DELETE FROM t_subject WHERE user_id=$uid;
         DELETE FROM t_verify_code WHERE email='$email';
         DELETE FROM t_user WHERE id=$uid;" >/dev/null
  fi
}

log ""
log "== 1. 注册 A（2028 届）/ B（2027 届）=="
cleanup_user "$EMAIL_A"; cleanup_user "$EMAIL_B"
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL_A','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REGA=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_A\",\"password\":\"$PASS\",\"examYear\":2028,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\"]}")
TOKEN_A=$(echo "$REGA" | field token)
AUTH_A="Authorization: Bearer $TOKEN_A"
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL_B','135790',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REGB=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_B\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"135790\",\"subjects\":[\"数学\",\"英语二\"]}")
TOKEN_B=$(echo "$REGB" | field token)
AUTH_B="Authorization: Bearer $TOKEN_B"
[ -n "$TOKEN_A" ] && [ -n "$TOKEN_B" ] && log "PASS: 注册 A/B 成功" || { log "FAIL: 注册"; exit 1; }

log ""
log "== 2. 造测试文章（id 901~903，只测不污染演示数据）=="
sql "DELETE FROM t_article WHERE id IN (901,902,903);" >/dev/null
sql "INSERT INTO t_article (id,author_id,title,content,status,published_at,created_at,updated_at) VALUES
 (901,3,'[verify]旧文','内容901',1,'2026-10-26 08:00:00','2026-10-26 08:00:00','2026-10-26 08:00:00'),
 (902,3,'[verify]新文','内容902',1,'2026-10-27 07:00:00','2026-10-27 07:00:00','2026-10-27 07:00:00'),
 (903,3,'[verify]草稿','内容903',0,NULL,'2026-10-27 07:30:00','2026-10-27 07:30:00');" >/dev/null
log "测试文章 3 篇（2 发布 + 1 草稿）"

log ""
log "== 3. 拉取 =="
curl -s "$API/info/overview" -H "$AUTH_A" > "$D/info_ov_a.json"
curl -s "$API/info/overview" -H "$AUTH_B" > "$D/info_ov_b.json"
curl -s "$API/articles" -H "$AUTH_A" > "$D/info_articles.json"
curl -s "$API/articles?page=2&size=2" -H "$AUTH_A" > "$D/info_articles_p2.json"
curl -s "$API/articles/902" -H "$AUTH_A" > "$D/info_detail_ok.json"
curl -s "$API/articles/903" -H "$AUTH_A" > "$D/info_detail_draft.json"
curl -s "$API/articles/203" -H "$AUTH_A" > "$D/info_detail_draft_demo.json"
curl -s "$API/articles/999999" -H "$AUTH_A" > "$D/info_detail_missing.json"
curl -s "$API/info/overview" > "$D/info_noauth.json"

log ""
log "== 4. 断言 =="
python <<'PYEOF' | tee -a "D:/todo/temp/verify_info.txt"
import json, sys
from datetime import datetime, timedelta, date
D = "D:/todo/temp"
PASS_N = FAIL_N = 0
def ok(m):
    global PASS_N; PASS_N += 1; print("PASS: " + m)
def fail(m):
    global FAIL_N; FAIL_N += 1; print("FAIL: " + m)
def load(n):
    with open(f"{D}/{n}", encoding="utf-8") as f:
        return json.load(f)
def code(n):
    return load(n).get("code")
def data(n):
    return load(n).get("data")

# overview A（2028 届）
a = data("info_ov_a.json")
if code("info_ov_a.json") == 0 and a["examYear"] == 2028:
    ok("A overview examYear=2028")
else:
    fail(f"A overview {load('info_ov_a.json')}")
ms = a["milestones"]
names = [m["name"] for m in ms]
if len(ms) == 8 and names[0] == "预报名" and names[4] == "初试" and names == sorted(names, key=lambda n: [m for m in ms if m["name"]==n][0]["sortOrder"]):
    ok("硬节点 8 个、sortOrder 升序（预报名…调剂系统开放）")
else:
    fail(f"milestones {names}")
chu = next(m for m in ms if m["name"] == "初试")
if chu["dateFrom"] == "2027-12-19" and chu["dateTo"] == "2027-12-20":
    ok("2028 届初试 2027-12-19~20")
else:
    fail(f"初试 {chu}")
studyday = (datetime.now() - timedelta(hours=4)).date()
exp = (date(2027, 12, 19) - studyday).days
if a["daysLeft"] == exp:
    ok(f"daysLeft={exp}（初试 − 学习日今天）")
else:
    fail(f"daysLeft={a['daysLeft']} 期望 {exp}")

# overview B（2027 届 → 届数过滤）
b = data("info_ov_b.json")
chu_b = next((m for m in b["milestones"] if m["name"] == "初试"), None)
if code("info_ov_b.json") == 0 and b["examYear"] == 2027 and chu_b and chu_b["dateFrom"] == "2026-12-19":
    ok("B overview 届数过滤（2027 届初试 2026-12-19）")
else:
    fail(f"B overview {b}")

# 列表：只含已发布 + 倒序
arts = data("info_articles.json")
titles = [it["title"] for it in arts["items"]]
if code("info_articles.json") == 0 and arts["total"] == 4 and len(arts["items"]) == 4:
    ok("列表 total=4（演示 2 + 测试 2 发布；2 草稿被排除）")
else:
    fail(f"列表 total={arts and arts.get('total')} titles={titles}")
if titles[0] == "[verify]新文" and titles[-1] == "2028 考研全年规划：四个阶段走稳上岸路":
    ok("发布倒序（10-27 最新在前，10-20 最旧在后）")
else:
    fail(f"顺序 {titles}")
if any("草稿" in t for t in titles):
    fail("草稿泄漏进列表")
else:
    ok("草稿不在列表")
if all(it["coverUrl"] is None for it in arts["items"]):
    ok("无封面 coverUrl=null")
else:
    fail("coverUrl 不该有值")

# 分页
p2 = data("info_articles_p2.json")
if p2["page"] == 2 and p2["size"] == 2 and len(p2["items"]) == 2 and p2["total"] == 4:
    ok("分页 page=2&size=2 正常")
else:
    fail(f"分页 {p2}")

# 详情
d = data("info_detail_ok.json")
if code("info_detail_ok.json") == 0 and d["title"] == "[verify]新文" and d["content"] == "内容902" and d["publishedAt"].startswith("2026-10-27"):
    ok("详情：已发布可见（标题/正文/发布时间）")
else:
    fail(f"详情 {load('info_detail_ok.json')}")
if code("info_detail_draft.json") == 3002 and code("info_detail_draft_demo.json") == 3002:
    ok("草稿详情 → 3002（测试草稿 + 演示草稿都不可见）")
else:
    fail(f"草稿 {load('info_detail_draft.json')} / {load('info_detail_draft_demo.json')}")
if code("info_detail_missing.json") == 3002:
    ok("不存在 id → 3002")
else:
    fail(f"missing {load('info_detail_missing.json')}")
if code("info_noauth.json") == 2001:
    ok("无票据 → 2001")
else:
    fail(f"noauth {load('info_noauth.json')}")

print(f"\n== python 断言汇总: PASS={PASS_N} FAIL={FAIL_N} ==")
sys.exit(1 if FAIL_N else 0)
PYEOF

log ""
log "== 5. 清理测试数据（保留演示文章与硬节点）=="
sql "DELETE FROM t_article WHERE id IN (901,902,903);" >/dev/null
cleanup_user "$EMAIL_A"; cleanup_user "$EMAIL_B"
REST=$(sql "SELECT COUNT(*) FROM t_article WHERE id IN (901,902,903);" | tail -1)
log "测试文章残留=$REST（期望 0）；硬节点/演示文章保留"
