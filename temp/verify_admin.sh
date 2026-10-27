#!/usr/bin/env bash
# 管理后台验证 v2（2026-10-27 三级权限制 + 文章归属权）
# 角色：0=普通用户 1=管理员 2=超级管理员（部署者 SQL 指定，接口只在 0/1 任免）
# 重点：
#   ① 卡口：普通用户/无票据 2002/2001；用户任免与硬节点仅超管（管理员 2002）
#   ② 文章归属权：所有管理员可看全部文章（标作者），仅作者本人或超管可编辑/发布/下线/删除
#   ③ 任免规则：不能操作自己 3001；超管不可被任免 3001；role 只收 0/1
#   ④ 文章生命周期 + 封面 MinIO 全链路（归档/换/清/删）+ C 端作者标识
set -u
OUT="D:/todo/temp/verify_admin.txt"
API="http://localhost:8080/api"
EMAIL_A="test_admin_a@example.com"   # 管理员 A（role=1，文章作者）
EMAIL_B="test_admin_b@example.com"   # 管理员 B（role=1，另一作者）
EMAIL_S="test_super_s@example.com"   # 超级管理员 S（role=2）
EMAIL_U="test_user_u@example.com"    # 普通用户 U（role=0）
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
         DELETE FROM t_article WHERE author_id=$uid;
         DELETE FROM t_user WHERE id=$uid;" >/dev/null
  fi
}
reg_login() { # $1=email $2=code -> token 到 $3
  sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$1','$2',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"$2\",\"subjects\":[\"政治\",\"英语一\"]}" > "$D/admin_reg_$1.json"
  login_only "$1" "$3"
}
login_only() { # $1=email -> token 到 $2（不注册，用于角色变更后重登）
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\"}" > "$D/admin_login_$1.json"
  eval "$2=\$(field token < \"$D/admin_login_$1.json\")"
}

log ""
log "== 1. 注册 A/B/S/U（注册只产普通用户）=="
for e in "$EMAIL_A" "$EMAIL_B" "$EMAIL_S" "$EMAIL_U"; do cleanup_user "$e"; done
sql "DELETE FROM t_article WHERE title LIKE '[verify-admin]%';" >/dev/null
sql "DELETE FROM t_exam_milestone WHERE exam_year=2099;" >/dev/null
reg_login "$EMAIL_A" "246810" TOKEN_A
reg_login "$EMAIL_B" "135790" TOKEN_B
reg_login "$EMAIL_S" "112233" TOKEN_S
reg_login "$EMAIL_U" "445566" TOKEN_U
UID_A=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_A';" | tail -1)
UID_B=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_B';" | tail -1)
UID_S=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_S';" | tail -1)
UID_U=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_U';" | tail -1)
[ -n "$TOKEN_A" ] && [ -n "$TOKEN_S" ] && [ -n "$TOKEN_U" ] && log "PASS: 注册 A/B/S/U 成功（A=$UID_A B=$UID_B S=$UID_S U=$UID_U）" || { log "FAIL: 注册"; exit 1; }
AUTH_A="Authorization: Bearer $TOKEN_A"; AUTH_B="Authorization: Bearer $TOKEN_B"
AUTH_S="Authorization: Bearer $TOKEN_S"; AUTH_U="Authorization: Bearer $TOKEN_U"

log ""
log "== 2. 库中指定角色（部署者 SQL）→ 重新登录 =="
sql "UPDATE t_user SET role=1 WHERE id=$UID_A; UPDATE t_user SET role=1 WHERE id=$UID_B; UPDATE t_user SET role=2 WHERE id=$UID_S;" >/dev/null
login_only "$EMAIL_A" TOKEN_A   # 重登拿带角色的会话
login_only "$EMAIL_B" TOKEN_B
login_only "$EMAIL_S" TOKEN_S
AUTH_A="Authorization: Bearer $TOKEN_A"; AUTH_B="Authorization: Bearer $TOKEN_B"
AUTH_S="Authorization: Bearer $TOKEN_S"

log ""
log "== 3. 权限卡口 =="
curl -s "$API/admin/articles" > "$D/admin_noauth.json"                              # 2001
curl -s "$API/admin/articles" -H "$AUTH_U" > "$D/admin_user_gate.json"             # 2002
curl -s -X POST "$API/admin/users/$UID_A/role" -H "$AUTH_U" -H "Content-Type: application/json" \
  -d '{"role":1}' > "$D/admin_self_promote.json"                                    # 2002
curl -s "$API/admin/users" -H "$AUTH_B" > "$D/admin_users_b.json"                   # 2002 仅超管
curl -s -X POST "$API/admin/milestones" -H "$AUTH_B" -H "Content-Type: application/json" \
  -d '{"examYear":2099,"name":"x","dateFrom":"2099-01-01"}' > "$D/admin_ms_b.json"  # 2002 仅超管
curl -s -X POST "$API/admin/users/$UID_U/role" -H "$AUTH_B" -H "Content-Type: application/json" \
  -d '{"role":1}' > "$D/admin_role_b.json"                                          # 2002 仅超管
curl -s "$API/admin/articles" -H "$AUTH_A" > "$D/admin_articles_a.json"            # 0 管理员可看

log ""
log "== 4. 文章归属权 =="
curl -s -X POST "$API/admin/articles" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]A 的文章","content":"正文 v1"}' > "$D/admin_create.json"
AID=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_create.json',encoding='utf-8'))['data']['id'])")
log "A 建文章 id=$AID"
# B 看得到（含作者标识）
curl -s "$API/admin/articles" -H "$AUTH_B" > "$D/admin_list_b.json"
curl -s "$API/admin/articles/$AID" -H "$AUTH_B" > "$D/admin_detail_b.json"
# B 操作 A 的文章 → 2002
curl -s -X PUT "$API/admin/articles/$AID" -H "$AUTH_B" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]B 篡改","content":"x"}' > "$D/admin_b_update.json"
curl -s -X POST "$API/admin/articles/$AID/publish" -H "$AUTH_B" > "$D/admin_b_publish.json"
curl -s -X POST "$API/admin/articles/$AID/unpublish" -H "$AUTH_B" > "$D/admin_b_unpublish.json"
curl -s -X DELETE "$API/admin/articles/$AID" -H "$AUTH_B" > "$D/admin_b_delete.json"
# A 自己操作 → 0
curl -s -X PUT "$API/admin/articles/$AID" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]第二稿","content":"正文 v2"}' > "$D/admin_update.json"
curl -s "$API/admin/articles/$AID" -H "$AUTH_A" > "$D/admin_detail_a.json"
curl -s -X POST "$API/admin/articles/$AID/publish" -H "$AUTH_A" > "$D/admin_publish.json"
curl -s "$API/articles/$AID" -H "$AUTH_U" > "$D/admin_cside_pub.json"               # C 端带 authorName
curl -s -X POST "$API/admin/articles/$AID/unpublish" -H "$AUTH_A" > "$D/admin_unpublish.json"
curl -s "$API/articles/$AID" -H "$AUTH_U" > "$D/admin_cside_draft.json"             # 3002
curl -s -X POST "$API/admin/articles/$AID/publish" -H "$AUTH_A" > "$D/admin_republish.json"
curl -s "$API/admin/articles/$AID" -H "$AUTH_A" > "$D/admin_detail_repub.json"
# 超管可操作他人文章 → 0
curl -s -X PUT "$API/admin/articles/$AID" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]第二稿","content":"超管修订"}' > "$D/admin_s_update.json"
curl -s -X DELETE "$API/admin/articles/$AID" -H "$AUTH_S" > "$D/admin_s_delete.json"
curl -s "$API/admin/articles/$AID" -H "$AUTH_A" > "$D/admin_detail_gone.json"
# 文章生命周期细节（空标题/状态过滤/publishedAt 首发不变）
curl -s -X POST "$API/admin/articles" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"title":"","content":"x"}' > "$D/admin_create_bad.json"
curl -s -X POST "$API/admin/articles" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]首发验证","content":"c"}' > "$D/admin_pub2.json"
AID2=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_pub2.json',encoding='utf-8'))['data']['id'])")
curl -s "$API/admin/articles?status=0" -H "$AUTH_A" > "$D/admin_list_draft.json"
curl -s "$API/admin/articles?status=1" -H "$AUTH_A" > "$D/admin_list_pub.json"
curl -s "$API/admin/articles?status=9" -H "$AUTH_A" > "$D/admin_list_badstatus.json"
curl -s -X POST "$API/admin/articles/$AID2/publish" -H "$AUTH_A" > "$D/admin_pub2_do.json"
curl -s "$API/admin/articles/$AID2" -H "$AUTH_A" > "$D/admin_pub2_d1.json"
curl -s -X POST "$API/admin/articles/$AID2/unpublish" -H "$AUTH_A" > "$D/admin_pub2_un.json"
curl -s -X POST "$API/admin/articles/$AID2/publish" -H "$AUTH_A" > "$D/admin_pub2_re.json"
curl -s "$API/admin/articles/$AID2" -H "$AUTH_A" > "$D/admin_pub2_d2.json"
curl -s -X DELETE "$API/admin/articles/$AID2" -H "$AUTH_A" > "$D/admin_pub2_del.json"

log ""
log "== 5. 封面 MinIO 全链路（A 的文章）=="
python - <<'PYEOF' > "$D/admin_px.png"
import base64
open(r'D:/todo/temp/admin_px.png','wb').write(base64.b64decode(
 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='))
PYEOF
curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/admin_px.png;type=image/png" > "$D/admin_up1.json"
K1=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_up1.json',encoding='utf-8'))['data']['objectKey'])")
U1=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_up1.json',encoding='utf-8'))['data']['previewUrl'])")
log "上传封面 1：$K1"
curl -s -X POST "$API/admin/articles" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"title\":\"[verify-admin]带封面\",\"content\":\"c\",\"coverObjectKey\":\"$K1\"}" > "$D/admin_cover_create.json"
CID=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_cover_create.json',encoding='utf-8'))['data']['id'])")
curl -s "$API/admin/articles/$CID" -H "$AUTH_A" > "$D/admin_cover_detail.json"
curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/admin_px.png;type=image/png" > "$D/admin_up2.json"
K2=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_up2.json',encoding='utf-8'))['data']['objectKey'])")
U2=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_up2.json',encoding='utf-8'))['data']['previewUrl'])")
curl -s -X PUT "$API/admin/articles/$CID" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"title\":\"[verify-admin]换封面\",\"content\":\"c\",\"coverObjectKey\":\"$K2\"}" > "$D/admin_cover_swap.json"
curl -s "$API/admin/articles/$CID" -H "$AUTH_A" > "$D/admin_cover_detail2.json"
NEW_URL=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_cover_detail2.json',encoding='utf-8'))['data']['coverUrl'])")
CODE_OLD_TMP=$(curl -s -o /dev/null -w "%{http_code}" "$U1")
CODE_NEW=$(curl -s -o /dev/null -w "%{http_code}" "$NEW_URL")
CODE_K2_TMP=$(curl -s -o /dev/null -w "%{http_code}" "$U2")
log "对象可达性：旧封面(已换掉)=$CODE_OLD_TMP 新封面=$CODE_NEW 换封面后旧tmp=$CODE_K2_TMP"
curl -s -X PUT "$API/admin/articles/$CID" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"title":"[verify-admin]清封面","content":"c","coverObjectKey":""}' > "$D/admin_cover_clear.json"
curl -s "$API/admin/articles/$CID" -H "$AUTH_A" > "$D/admin_cover_detail3.json"
CODE_CLEARED=$(curl -s -o /dev/null -w "%{http_code}" "$NEW_URL")
curl -s -X POST "$API/uploads/images" -H "$AUTH_A" -F "file=@$D/admin_px.png;type=image/png" > "$D/admin_up3.json"
K3=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_up3.json',encoding='utf-8'))['data']['objectKey'])")
curl -s -X PUT "$API/admin/articles/$CID" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"title\":\"[verify-admin]删文清封面\",\"content\":\"c\",\"coverObjectKey\":\"$K3\"}" > "$D/admin_cover_set3.json"
curl -s "$API/admin/articles/$CID" -H "$AUTH_A" > "$D/admin_cover_detail4.json"
U3=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_cover_detail4.json',encoding='utf-8'))['data'].get('coverUrl') or '')")
curl -s -X DELETE "$API/admin/articles/$CID" -H "$AUTH_A" > "$D/admin_cover_delete.json"
CODE_DEL_COVER=$(curl -s -o /dev/null -w "%{http_code}" "$U3")
log "清封面后可达=$CODE_CLEARED 删文后封面可达=$CODE_DEL_COVER"
echo "$CODE_OLD_TMP $CODE_NEW $CODE_K2_TMP $CODE_CLEARED $CODE_DEL_COVER" > "$D/admin_cover_codes.txt"

log ""
log "== 6. 用户任免（仅超管 S）=="
curl -s "$API/admin/users?keyword=${EMAIL_U%%@*}" -H "$AUTH_S" > "$D/admin_users_kw.json"
curl -s "$API/admin/users?keyword=$UID_U" -H "$AUTH_S" > "$D/admin_users_id.json"
curl -s -X POST "$API/admin/users/$UID_U/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":1}' > "$D/admin_promote.json"
curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_U\",\"password\":\"$PASS\"}" > "$D/admin_login_u2.json"
TOKEN_U2=$(field token < "$D/admin_login_u2.json")
curl -s "$API/admin/articles" -H "Authorization: Bearer $TOKEN_U2" > "$D/admin_u_after_promote.json"
curl -s -X POST "$API/admin/users/$UID_U/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":0}' > "$D/admin_demote.json"
curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL_U\",\"password\":\"$PASS\"}" > "$D/admin_login_u3.json"
TOKEN_U3=$(field token < "$D/admin_login_u3.json")
curl -s "$API/admin/articles" -H "Authorization: Bearer $TOKEN_U3" > "$D/admin_u_after_demote.json"
# 超管不能操作自己
curl -s -X POST "$API/admin/users/$UID_S/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":0}' > "$D/admin_self_demote.json"
# 超管不可被任免（临时把 A 提成超管再试，之后还原）
sql "UPDATE t_user SET role=2 WHERE id=$UID_A;" >/dev/null
curl -s -X POST "$API/admin/users/$UID_A/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":0}' > "$D/admin_super_guard.json"
sql "UPDATE t_user SET role=1 WHERE id=$UID_A;" >/dev/null
curl -s -X POST "$API/admin/users/$UID_B/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":2}' > "$D/admin_role_bad.json"
curl -s -X POST "$API/admin/users/999999/role" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"role":1}' > "$D/admin_user_missing.json"

log ""
log "== 7. 硬节点管理（仅超管 S；2099 测试届隔离）=="
curl -s -X POST "$API/admin/milestones" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"examYear":2099,"name":"初试","dateFrom":"2099-12-19","dateTo":"2099-12-20","sortOrder":5}' > "$D/admin_ms_create.json"
MID=$(python -c "import json;print(json.load(open(r'D:/todo/temp/admin_ms_create.json',encoding='utf-8'))['data']['id'])")
log "新建硬节点 id=$MID"
curl -s -X POST "$API/admin/milestones" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"examYear":2099,"name":"坏节点","dateFrom":"2099-12-19","dateTo":"2099-01-01"}' > "$D/admin_ms_bad_date.json"
curl -s -X POST "$API/admin/milestones" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"examYear":1500,"name":"坏年","dateFrom":"2099-01-01"}' > "$D/admin_ms_bad_year.json"
curl -s "$API/admin/milestones?examYear=2099" -H "$AUTH_S" > "$D/admin_ms_list.json"
curl -s "$API/admin/milestones" -H "$AUTH_S" > "$D/admin_ms_all.json"
curl -s -X PUT "$API/admin/milestones/$MID" -H "$AUTH_S" -H "Content-Type: application/json" \
  -d '{"examYear":2099,"name":"初试（改）","dateFrom":"2099-12-20","dateTo":"2099-12-21","sortOrder":6}' > "$D/admin_ms_update.json"
curl -s "$API/admin/milestones?examYear=2099" -H "$AUTH_S" > "$D/admin_ms_list2.json"
curl -s -X DELETE "$API/admin/milestones/$MID" -H "$AUTH_S" > "$D/admin_ms_delete.json"
curl -s "$API/admin/milestones?examYear=2099" -H "$AUTH_S" > "$D/admin_ms_list3.json"

log ""
log "== 8. 断言 =="
python <<'PYEOF' | tee -a "D:/todo/temp/verify_admin.txt"
import json, sys
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

# 注册只产普通用户 + 登录带 role（三级）
if code("admin_reg_test_admin_a@example.com.json") == 0 and data("admin_reg_test_admin_a@example.com.json")["role"] == 0:
    ok("注册响应 role=0（注册永远只产普通用户）")
else:
    fail(f"注册 {load('admin_reg_test_admin_a@example.com.json')}")
la, lb, ls = data("admin_login_test_admin_a@example.com.json"), data("admin_login_test_admin_b@example.com.json"), data("admin_login_test_super_s@example.com.json")
if la and la.get("role") == 1 and lb and lb.get("role") == 1 and ls and ls.get("role") == 2:
    ok("登录响应 role 三级制：A=1、B=1、S=2")
else:
    fail(f"登录 role A={la and la.get('role')} B={lb and lb.get('role')} S={ls and ls.get('role')}")

# 权限卡口
if code("admin_noauth.json") == 2001:
    ok("无票据 → 2001")
else:
    fail(f"noauth {load('admin_noauth.json')}")
if code("admin_user_gate.json") == 2002 and code("admin_self_promote.json") == 2002:
    ok("普通用户调管理接口 → 2002（含不能自我提拔）")
else:
    fail(f"gate {load('admin_user_gate.json')} / {load('admin_self_promote.json')}")
if code("admin_users_b.json") == 2002 and code("admin_ms_b.json") == 2002 and code("admin_role_b.json") == 2002:
    ok("管理员 B 调用户任免/硬节点 → 2002（仅超管）")
else:
    fail(f"admin-only {load('admin_users_b.json')} / {load('admin_ms_b.json')} / {load('admin_role_b.json')}")
if code("admin_articles_a.json") == 0:
    ok("管理员 A → 文章列表正常")
else:
    fail(f"admin articles {load('admin_articles_a.json')}")

# 文章归属权
items = data("admin_list_b.json")["items"]
mine = next((it for it in items if it.get("title") == "[verify-admin]A 的文章"), None)
if mine and mine.get("authorId") and mine.get("authorName"):
    ok(f"B 看得到 A 的文章且标作者（authorName={mine['authorName']}）")
else:
    fail(f"list_b 标作者 {mine}")
db = data("admin_detail_b.json")
if code("admin_detail_b.json") == 0 and db and db.get("editable") is False and db.get("authorName"):
    ok("B 看得到 A 文章详情，editable=false（不可操作）")
else:
    fail(f"detail_b {db}")
if code("admin_b_update.json") == 2002 and code("admin_b_publish.json") == 2002 and code("admin_b_unpublish.json") == 2002 and code("admin_b_delete.json") == 2002:
    ok("B 操作 A 的文章（改/发/下/删）全部 2002（互不干涉）")
else:
    fail(f"b-ops {load('admin_b_update.json')} / {load('admin_b_publish.json')} / {load('admin_b_unpublish.json')} / {load('admin_b_delete.json')}")
da = data("admin_detail_a.json")
if code("admin_update.json") == 0 and da and da.get("editable") is True and da["title"] == "[verify-admin]第二稿":
    ok("A 编辑自己的文章成功（editable=true）")
else:
    fail(f"update {load('admin_update.json')} / {da}")
if code("admin_s_update.json") == 0 and code("admin_s_delete.json") == 0 and code("admin_detail_gone.json") == 3002:
    ok("超管 S 可改/删 A 的文章（全权），删后详情 3002")
else:
    fail(f"s-ops {load('admin_s_update.json')} / {load('admin_s_delete.json')} / {load('admin_detail_gone.json')}")

# C 端作者标识
cs = data("admin_cside_pub.json")
if code("admin_cside_pub.json") == 0 and cs and cs.get("authorName"):
    ok(f"C 端文章详情带作者（authorName={cs['authorName']}）")
else:
    fail(f"cside {cs}")
if code("admin_cside_draft.json") == 3002:
    ok("下线后 C 端 3002")
else:
    fail(f"cside_draft {load('admin_cside_draft.json')}")

# 生命周期细节
if code("admin_create_bad.json") == 1001:
    ok("空标题 → 1001")
else:
    fail(f"create_bad {load('admin_create_bad.json')}")
titles0 = [it["title"] for it in data("admin_list_draft.json")["items"]]
titles1 = [it["title"] for it in data("admin_list_pub.json")["items"]]
if any("[verify-admin]首发验证" in t for t in titles0) and not any("[verify-admin]首发验证" in t for t in titles1):
    ok("status=0 列表含草稿、status=1 不含")
else:
    fail(f"列表 status 过滤 {titles0} / {titles1}")
if code("admin_list_badstatus.json") == 1001:
    ok("status=9 → 1001")
else:
    fail(f"badstatus {load('admin_list_badstatus.json')}")
d1, d2 = data("admin_pub2_d1.json"), data("admin_pub2_d2.json")
if d1 and d1["publishedAt"] and d2 and d2["publishedAt"] == d1["publishedAt"]:
    ok("下线再发布 publishedAt 不变（=首次发布时间）")
else:
    fail(f"republish {d1 and d1.get('publishedAt')} != {d2 and d2.get('publishedAt')}")

# 封面 MinIO 全链路
cd = data("admin_cover_detail.json")
if cd and cd["coverObjectKey"] and cd["coverObjectKey"].startswith("user_") and "/article_" in cd["coverObjectKey"] and "/tmp/" not in cd["coverObjectKey"]:
    ok(f"封面归档：tmp 键转正 {cd['coverObjectKey']}")
else:
    fail(f"cover 归档 {cd}")
if cd and cd["coverUrl"]:
    ok("详情带封面预签名 coverUrl")
else:
    fail(f"coverUrl {cd}")
codes = open(f"{D}/admin_cover_codes.txt").read().split()
if len(codes) == 5:
    old_tmp, new_ok, k2_tmp, cleared, del_cover = codes
    if new_ok == "200":
        ok("新封面对象可达（MinIO 200）")
    else:
        fail(f"新封面可达性 {new_ok}")
    if old_tmp != "200" and k2_tmp != "200":
        ok(f"旧对象已清（换封面清旧 + move 删源，tmp 可达性 {old_tmp}/{k2_tmp}）")
    else:
        fail(f"旧对象残留 tmp={old_tmp} k2={k2_tmp}")
    if cleared != "200":
        ok(f"清封面后对象已删（{cleared}）")
    else:
        fail("清封面后对象仍在")
    if del_cover != "200":
        ok(f"删文章后封面对象已删（{del_cover}）")
    else:
        fail("删文章后封面对象残留")
else:
    fail(f"cover codes {codes}")
cd3 = data("admin_cover_detail3.json")
if cd3 and cd3["coverObjectKey"] is None and cd3["coverUrl"] is None:
    ok("清封面后 detail 归零（coverObjectKey=null）")
else:
    fail(f"cover_clear {cd3}")

# 用户任免（仅超管）
items = data("admin_users_kw.json")["items"]
if any(it["email"] == "test_user_u@example.com" for it in items) and all("password" not in json.dumps(it) for it in items):
    ok("超管按邮箱关键词定位用户（不含密码字段）")
else:
    fail(f"users_kw {items}")
items2 = data("admin_users_id.json")["items"]
if len(items2) == 1 and items2[0]["email"] == "test_user_u@example.com":
    ok("超管按 id 精确定位用户")
else:
    fail(f"users_id {items2}")
if code("admin_promote.json") == 0 and code("admin_u_after_promote.json") == 0 and data("admin_login_u2.json").get("role") == 1:
    ok("超管提拔：U role=1 后可调管理接口")
else:
    fail(f"promote {load('admin_promote.json')} / {load('admin_u_after_promote.json')} / {data('admin_login_u2.json')}")
if code("admin_demote.json") == 0 and code("admin_u_after_demote.json") == 2002 and data("admin_login_u3.json").get("role") == 0:
    ok("超管取消管理员：U role=0 后被拒 2002")
else:
    fail(f"demote {load('admin_demote.json')} / {load('admin_u_after_demote.json')}")
if code("admin_self_demote.json") == 3001:
    ok("超管不能变更自己的角色 → 3001（防锁死）")
else:
    fail(f"self_demote {load('admin_self_demote.json')}")
if code("admin_super_guard.json") == 3001:
    ok("超管身份不可被任免（对 role=2 目标操作 → 3001）")
else:
    fail(f"super_guard {load('admin_super_guard.json')}")
if code("admin_role_bad.json") == 1001 and code("admin_user_missing.json") == 3002:
    ok("role=2 入参 → 1001（接口只在 0/1 任免）；用户不存在 → 3002")
else:
    fail(f"role_bad {load('admin_role_bad.json')} / {load('admin_user_missing.json')}")

# 硬节点
m = data("admin_ms_create.json")
if code("admin_ms_create.json") == 0 and m and m["examYear"] == 2099 and m["name"] == "初试":
    ok(f"超管新建硬节点 id={m['id']}")
else:
    fail(f"ms_create {load('admin_ms_create.json')}")
if code("admin_ms_bad_date.json") == 1001 and code("admin_ms_bad_year.json") == 1001:
    ok("校验：结束日早于起始日 → 1001；届数越界 → 1001")
else:
    fail(f"ms_bad {load('admin_ms_bad_date.json')} / {load('admin_ms_bad_year.json')}")
l2 = data("admin_ms_list2.json")["items"]
if len(l2) == 1 and l2[0]["name"] == "初试（改）" and l2[0]["sortOrder"] == 6:
    ok("编辑硬节点生效（整体替换式）")
else:
    fail(f"ms_update {l2}")
all_items = data("admin_ms_all.json")["items"]
years = {it["examYear"] for it in all_items}
if 2099 in years and (2027 in years or 2028 in years):
    ok("全量列表跨届可见（含演示 2027/2028）")
else:
    fail(f"ms_all years={years}")
if code("admin_ms_delete.json") == 0 and data("admin_ms_list3.json")["items"] == []:
    ok("删除硬节点生效")
else:
    fail(f"ms_delete {load('admin_ms_delete.json')} / {load('admin_ms_list3.json')}")

print(f"\n== python 断言汇总: PASS={PASS_N} FAIL={FAIL_N} ==")
sys.exit(1 if FAIL_N else 0)
PYEOF

log ""
log "== 9. 清理测试数据 =="
sql "DELETE FROM t_article WHERE title LIKE '[verify-admin]%';" >/dev/null
sql "DELETE FROM t_exam_milestone WHERE exam_year=2099;" >/dev/null
for e in "$EMAIL_A" "$EMAIL_B" "$EMAIL_S" "$EMAIL_U"; do cleanup_user "$e"; done
REST=$(sql "SELECT COUNT(*) FROM t_article WHERE title LIKE '[verify-admin]%';" | tail -1)
log "测试文章残留=$REST（期望 0）；演示文章/硬节点保留"
