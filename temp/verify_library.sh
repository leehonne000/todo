#!/usr/bin/env bash
# 自由库 + 标签验证（契约 §7 五接口：列表/详情/修改/删除/标签）
# 重点：筛选分页、整体替换（保留/归档/删除三态）、复习状态不因编辑改变、
#       图片对象清理、他人记录 3002、标签词表独立保留
set -u
OUT="D:/todo/temp/verify_library.txt"
API="http://localhost:8080/api"
EMAIL_A="test_lib_a@example.com"
EMAIL_B="test_lib_b@example.com"
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
log "== 1. 注册 A/B 两用户 =="
cleanup_user "$EMAIL_A"; cleanup_user "$EMAIL_B"
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL_A','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REGA=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_A\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\"]}")
TOKEN_A=$(echo "$REGA" | field token)
AUTH_A="Authorization: Bearer $TOKEN_A"
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL_B','135790',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REGB=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_B\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"135790\",\"subjects\":[\"数学\",\"英语二\"]}")
TOKEN_B=$(echo "$REGB" | field token)
AUTH_B="Authorization: Bearer $TOKEN_B"
[ -n "$TOKEN_A" ] && [ -n "$TOKEN_B" ] && log "PASS: 注册 A/B 成功" || { log "FAIL: 注册 $REGA | $REGB"; exit 1; }
UID_A=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_A';" | tail -1)
SZ=$(sql "SELECT id FROM t_subject WHERE user_id=$UID_A AND name='政治';" | tail -1)
SE=$(sql "SELECT id FROM t_subject WHERE user_id=$UID_A AND name='英语一';" | tail -1)
log "UID_A=$UID_A 政治=$SZ 英语一=$SE"

log ""
log "== 2. 空态 =="
curl -s "$API/entries" -H "$AUTH_A" > "$D/lib_empty.json"
curl -s "$API/tags" -H "$AUTH_A" > "$D/lib_tags_empty.json"

log ""
log "== 3. 造数（两个 session + 上传 3 图 + 落 3 条记录）=="
python - <<'PYEOF'
import struct, zlib
def png(path, rgb):
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    w = h = 6
    raw = b''.join(b'\x00' + bytes(rgb) * w for _ in range(h))
    data = b'\x89PNG\r\n\x1a\n'
    data += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(raw))
    data += chunk(b'IEND', b'')
    open(path, 'wb').write(data)
png("D:/todo/temp/lib_img1.png", (220, 60, 60))
png("D:/todo/temp/lib_img2.png", (60, 180, 90))
png("D:/todo/temp/lib_img3.png", (70, 110, 220))
print("3 张测试 PNG 已生成")
PYEOF

UP1=$(curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/lib_img1.png")
UP2=$(curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/lib_img2.png")
UP3=$(curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/lib_img3.png")
K1=$(echo "$UP1" | field objectKey); K2=$(echo "$UP2" | field objectKey); K3=$(echo "$UP3" | field objectKey)
log "tmp键 K1=$K1 K2=$K2 K3=$K3"
[ -n "$K3" ] || { log "FAIL: 上传失败 $UP1|$UP2|$UP3"; exit 1; }

S1=$(curl -s -X POST "$API/sessions" -H "$AUTH_A" -H "Content-Type: application/json" -d "{\"subjectId\":$SZ}")
S2=$(curl -s -X POST "$API/sessions" -H "$AUTH_A" -H "Content-Type: application/json" -d "{\"subjectId\":$SE}")
SID1=$(echo "$S1" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
SID2=$(echo "$S2" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
[ -n "$SID1" ] && [ -n "$SID2" ] && log "session 政治=$SID1 英语一=$SID2" || { log "FAIL: 开 session $S1|$S2"; exit 1; }

# e1=note/政治/带图K1/标签重点+薄弱；e2=错题/英语一/纯文字/标签薄弱；e3=note/政治/纯图片K2
curl -s -X POST "$API/sessions/$SID1/entries" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"库测 note A\",\"tagNames\":[\"重点\",\"薄弱\"],\"imageObjectKeys\":[\"$K1\"]}]}" >/dev/null
curl -s -X POST "$API/sessions/$SID2/entries" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":2,\"content\":\"库测 错题 B\",\"tagNames\":[\"薄弱\"],\"imageObjectKeys\":[]}]}" >/dev/null
curl -s -X POST "$API/sessions/$SID1/entries" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"\",\"tagNames\":[],\"imageObjectKeys\":[\"$K2\"]}]}" >/dev/null

E1=$(sql "SELECT id FROM t_entry WHERE user_id=$UID_A ORDER BY id;" | sed -n '2p')
E2=$(sql "SELECT id FROM t_entry WHERE user_id=$UID_A ORDER BY id;" | sed -n '3p')
E3=$(sql "SELECT id FROM t_entry WHERE user_id=$UID_A ORDER BY id;" | sed -n '4p')
# created_at 拉开（同秒插入时倒序不稳定）：e1<e2<e3
sql "UPDATE t_entry SET created_at='2026-10-26 09:00:00' WHERE id=$E1;
     UPDATE t_entry SET created_at='2026-10-26 10:00:00' WHERE id=$E2;
     UPDATE t_entry SET created_at='2026-10-26 11:00:00' WHERE id=$E3;" >/dev/null
log "entry e1=$E1 e2=$E2 e3=$E3（created_at 已拉开）"
K1_ARCH=$(echo "$UP1" | field objectKey | sed "s#/tmp/#/entry_${E1}/#")
log "e1 归档图键（推导）=$K1_ARCH"
DUE1_BEFORE=$(sql "SELECT next_due_at FROM t_entry WHERE id=$E1;" | tail -1)
ROUND1_BEFORE=$(sql "SELECT round FROM t_entry WHERE id=$E1;" | tail -1)
log "e1 修改前 round=$ROUND1_BEFORE next_due_at=$DUE1_BEFORE"

log ""
log "== 4. 列表与筛选 =="
curl -s "$API/entries" -H "$AUTH_A" > "$D/lib_list_all.json"
curl -s "$API/entries?subjectId=$SZ" -H "$AUTH_A" > "$D/lib_list_subj1.json"
curl -s "$API/entries?subjectId=$SE" -H "$AUTH_A" > "$D/lib_list_subj2.json"
curl -s "$API/entries?type=2" -H "$AUTH_A" > "$D/lib_list_type2.json"
TAG_BORU=$(sql "SELECT id FROM t_tag WHERE user_id=$UID_A AND name='薄弱';" | tail -1)
TAG_ZHONG=$(sql "SELECT id FROM t_tag WHERE user_id=$UID_A AND name='重点';" | tail -1)
curl -s "$API/entries?tagIds=$TAG_BORU" -H "$AUTH_A" > "$D/lib_list_tag.json"
curl -s "$API/entries?tagIds=$TAG_ZHONG,$TAG_BORU" -H "$AUTH_A" > "$D/lib_list_tag2.json"
curl -s "$API/entries?page=2&size=2" -H "$AUTH_A" > "$D/lib_list_page2.json"

log ""
log "== 5. 详情与越权 =="
curl -s "$API/entries/$E1" -H "$AUTH_A" > "$D/lib_detail1.json"
curl -s "$API/entries/$E3" -H "$AUTH_A" > "$D/lib_detail3_before.json"
curl -s "$API/entries/999999" -H "$AUTH_A" > "$D/lib_detail_missing.json"
curl -s "$API/entries/$E1" -H "$AUTH_B" > "$D/lib_detail_other.json"

log ""
log "== 6. 修改（整体替换：保 K1 + 归档 K3，删旧图 K2 走 e3 用例）=="
curl -s -X PUT "$API/entries/$E1" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"content\":\"改后内容\",\"tagNames\":[\"重点\",\"新增\"],\"imageObjectKeys\":[\"$K1_ARCH\",\"$K3\"]}" > "$D/lib_update1.json"
curl -s "$API/entries/$E1" -H "$AUTH_A" > "$D/lib_detail1_after.json"
DUE1_AFTER=$(sql "SELECT next_due_at FROM t_entry WHERE id=$E1;" | tail -1)
ROUND1_AFTER=$(sql "SELECT round FROM t_entry WHERE id=$E1;" | tail -1)
log "e1 修改后 round=$ROUND1_AFTER next_due_at=$DUE1_AFTER（期望与修改前一致）"

log "空提交（预期 1001 文字与图片至少填一项）:"
curl -s -X PUT "$API/entries/$E1" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"content":"","imageObjectKeys":[]}' > "$D/lib_update_empty.json"
log "11 张图（预期 1001 最多 10 张）:"
IMGS11=$(python -c "print(','.join('\"user_$UID_A/tmp/f%d.png\"' % i for i in range(11)))")
curl -s -X PUT "$API/entries/$E1" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"content\":\"x\",\"imageObjectKeys\":[$IMGS11]}" > "$D/lib_update_11.json"
log "他人图片键（预期 1001 图片不属于当前用户）:"
curl -s -X PUT "$API/entries/$E1" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"content":"x","imageObjectKeys":["user_99999/tmp/a.png"]}' > "$D/lib_update_foreign.json"
log "别条记录的图键（预期 1001 图片不属于当前记录）:"
K2_ARCH=$(python -c "import json;print(json.load(open(r'D:/todo/temp/lib_detail3_before.json'))['data']['images'][0]['objectKey'])")
curl -s -X PUT "$API/entries/$E1" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"content\":\"x\",\"imageObjectKeys\":[\"$K2_ARCH\"]}" > "$D/lib_update_otherentry.json"

log ""
log "== 7. 删除图片对象清理（e3 去掉唯一一张图）=="
K2_URL=$(python -c "import json;print(json.load(open(r'D:/todo/temp/lib_detail3_before.json'))['data']['images'][0]['previewUrl'])")
curl -s -X PUT "$API/entries/$E3" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"content":"e3 加文字","tagNames":[],"imageObjectKeys":[]}' > "$D/lib_update3.json"
curl -s "$API/entries/$E3" -H "$AUTH_A" > "$D/lib_detail3_after.json"
K2_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$K2_URL")
log "被移除图片的预签名 URL 现状：HTTP $K2_CODE（期望非 200）"

log ""
log "== 8. 删除记录 =="
curl -s -X DELETE "$API/entries/$E2" -H "$AUTH_A" > "$D/lib_delete.json"
curl -s "$API/entries" -H "$AUTH_A" > "$D/lib_list_after_delete.json"
curl -s "$API/entries/$E2" -H "$AUTH_A" > "$D/lib_detail2_after.json"
curl -s "$API/tags" -H "$AUTH_A" > "$D/lib_tags.json"
curl -s "$API/tags?q=%E8%96%84%E5%BC%B1" -H "$AUTH_A" > "$D/lib_tags_q.json"

log ""
log "== 9. 无票据 =="
curl -s "$API/entries" > "$D/lib_noauth.json"

log ""
log "== 10. 断言 =="
python - "$DUE1_BEFORE" "$DUE1_AFTER" "$ROUND1_BEFORE" "$ROUND1_AFTER" "$K2_CODE" "$UID_A" "$E1" "$E3" "$K1_ARCH" <<'PYEOF'
import json, sys
D = "D:/todo/temp"
due_b, due_a, rd_b, rd_a, k2code, uid, e1, e3, k1_arch = sys.argv[1:10]
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

# 空态
if code("lib_empty.json") == 0 and data("lib_empty.json")["total"] == 0:
    ok("空库列表 total=0")
else:
    fail(f"空库列表 {load('lib_empty.json')}")
if code("lib_tags_empty.json") == 0 and data("lib_tags_empty.json") == []:
    ok("空库标签词表 []")
else:
    fail(f"空库标签 {load('lib_tags_empty.json')}")

# 列表
d = data("lib_list_all.json")
if code("lib_list_all.json") == 0 and d["total"] == 3 and len(d["items"]) == 3:
    ok("列表 total=3（createdAt 倒序 3 条）")
else:
    fail(f"列表 total={d and d.get('total')} items={d and len(d.get('items', []))}")
ids = [it["id"] for it in d["items"]]
if ids[0] == int(e3) and ids[-1] == int(e1):
    ok("列表按 createdAt 倒序（e3 最新在前，e1 最旧在后）")
else:
    fail(f"列表顺序 {ids}")
e1i = next(it for it in d["items"] if it["id"] == int(e1))
e3i = next(it for it in d["items"] if it["id"] == int(e3))
if e1i["imageCount"] == 1 and e1i["tags"] == ["重点", "薄弱"] and e1i["subjectName"] == "政治" and e1i["type"] == 1:
    ok("e1 列表项字段（imageCount/tags/subjectName/type）")
else:
    fail(f"e1 列表项 {e1i}")
if e3i["imageCount"] == 1 and e3i["content"] == "" and e3i["tags"] == []:
    ok("e3 纯图片记录列表项（content 空串）")
else:
    fail(f"e3 列表项 {e3i}")

# 筛选
def total(n):
    return data(n)["total"]
if total("lib_list_subj1.json") == 2 and total("lib_list_subj2.json") == 1:
    ok("科目筛选（政治 2 / 英语一 1）")
else:
    fail(f"科目筛选 {total('lib_list_subj1.json')}/{total('lib_list_subj2.json')}")
if total("lib_list_type2.json") == 1 and data("lib_list_type2.json")["items"][0]["type"] == 2:
    ok("类型筛选 type=2 命中 1 条错题")
else:
    fail(f"type 筛选 {load('lib_list_type2.json')}")
if total("lib_list_tag.json") == 2:
    ok("标签筛选「薄弱」命中 2 条")
else:
    fail(f"标签筛选 {total('lib_list_tag.json')}")
if total("lib_list_tag2.json") == 2:
    ok("多标签筛选（任一命中）2 条")
else:
    fail(f"多标签 {total('lib_list_tag2.json')}")
p2 = data("lib_list_page2.json")
if p2["page"] == 2 and len(p2["items"]) == 1 and p2["total"] == 3 and p2["size"] == 2:
    ok("分页 page=2&size=2 → 1 条，total=3")
else:
    fail(f"分页 {p2}")

# 详情
det = data("lib_detail1.json")
if det and det["content"] == "库测 note A" and det["images"][0]["objectKey"].startswith(f"user_{uid}/entry_{e1}/") and det["images"][0]["previewUrl"].startswith("http"):
    ok("详情：内容 + 归档图键 + 预签名 URL")
else:
    fail(f"详情 {det}")
if code("lib_detail_missing.json") == 3002:
    ok("不存在 id → 3002")
else:
    fail(f"missing {load('lib_detail_missing.json')}")
if code("lib_detail_other.json") == 3002:
    ok("他人记录 → 3002")
else:
    fail(f"other {load('lib_detail_other.json')}")

# 修改
if code("lib_update1.json") == 0:
    ok("修改整体替换 → 0")
else:
    fail(f"update1 {load('lib_update1.json')}")
da = data("lib_detail1_after.json")
if da["content"] == "改后内容":
    ok("内容已更新")
else:
    fail(f"内容 {da['content']}")
if da["tags"] == ["重点", "新增"]:
    ok("标签整体替换（薄弱→新增）")
else:
    fail(f"标签 {da['tags']}")
if len(da["images"]) == 2 and da["images"][0]["objectKey"] == k1_arch and da["images"][1]["objectKey"].startswith(f"user_{uid}/entry_{e1}/") and da["images"][1]["objectKey"] != k1_arch:
    ok("图片 2 张：旧键保留（K1）+ 新图（K3）归档到本记录")
else:
    fail(f"图片 {da['images']} (期望[0]={k1_arch})")
if due_b == due_a and rd_b == rd_a:
    ok(f"复习状态不因编辑改变（round={rd_a}, next_due_at={due_a}）")
else:
    fail(f"复习状态变了 {rd_b}/{due_b} → {rd_a}/{due_a}")
if code("lib_update_empty.json") == 1001:
    ok("空提交 → 1001")
else:
    fail(f"empty {load('lib_update_empty.json')}")
if code("lib_update_11.json") == 1001:
    ok("11 张图 → 1001")
else:
    fail(f"11imgs {load('lib_update_11.json')}")
if code("lib_update_foreign.json") == 1001:
    ok("他人图片键 → 1001")
else:
    fail(f"foreign {load('lib_update_foreign.json')}")
if code("lib_update_otherentry.json") == 1001:
    ok("别条记录的图键 → 1001")
else:
    fail(f"otherentry {load('lib_update_otherentry.json')}")

# 图片对象清理
if code("lib_update3.json") == 0 and data("lib_detail3_after.json")["images"] == []:
    ok("e3 图片移除成功（详情图空）")
else:
    fail(f"update3 {load('lib_update3.json')} / {data('lib_detail3_after.json')}")
if k2code != "200":
    ok(f"被移除图片对象已清理（预签名 URL HTTP {k2code}）")
else:
    fail("被移除图片对象仍可访问")

# 删除记录
if code("lib_delete.json") == 0 and data("lib_list_after_delete.json")["total"] == 2 and code("lib_detail2_after.json") == 3002:
    ok("删除记录：列表剩 2、详情 3002")
else:
    fail(f"delete {load('lib_delete.json')}/{data('lib_list_after_delete.json')}/{load('lib_detail2_after.json')}")
names = sorted(t["name"] for t in data("lib_tags.json"))
if names == sorted(["重点", "薄弱", "新增"]):
    ok("标签词表独立保留（重点/薄弱/新增）")
else:
    fail(f"tags {names}")
q = [t["name"] for t in data("lib_tags_q.json")]
if q == ["薄弱"]:
    ok("标签 q=薄弱 过滤正确")
else:
    fail(f"tags q {q}")

if code("lib_noauth.json") == 2001:
    ok("无票据 → 2001")
else:
    fail(f"noauth {load('lib_noauth.json')}")

print(f"\n== python 断言汇总: PASS={PASS_N} FAIL={FAIL_N} ==")
sys.exit(1 if FAIL_N else 0)
PYEOF

log ""
log "== 11. 清理测试数据 =="
cleanup_user "$EMAIL_A"; cleanup_user "$EMAIL_B"
REST=$(sql "SELECT (SELECT COUNT(*) FROM t_user)+(SELECT COUNT(*) FROM t_entry)+(SELECT COUNT(*) FROM t_tag);" | tail -1)
log "清场后 user+entry+tag 行数=$REST（期望 0）"
rm -f "$D"/lib_img*.png
log "（MinIO 测试对象为 tmp/entry 前缀残留，无碍）"
