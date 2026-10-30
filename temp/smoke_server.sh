#!/usr/bin/env bash
# 服务器冒烟（2026-10-30 部署验证）：走公网地址全链路，重点验证图片裂图修复（路线②）+ 复习页取图
# 用法：bash smoke_server.sh   （在本机 Git Bash 跑，目标=服务器公网）
set -u
BASE="http://124.220.182.134"
API="$BASE/api"
EMAIL="smoke_test@example.com"
PASS="Smoke-12345"
IMG1="/d/todo/pics/background/其他/Bits + Pieces__foocngohimdoacmhbjfkdldchhiipieo.png"
IMG2="/d/todo/pics/background/娱乐/Tardis__jnpaonbghmoocnkenfjigielilfmaboj.png"
KEY="id_ed25519_bigtree"
HOST="ubuntu@124.220.182.134"
PASS_CNT=0; FAIL_CNT=0
log()  { echo "$@"; }
pass() { echo "PASS: $@"; PASS_CNT=$((PASS_CNT+1)); }
fail() { echo "FAIL: $@"; FAIL_CNT=$((FAIL_CNT+1)); }
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }
sql() {
  ssh -i ~/.ssh/$KEY -o BatchMode=yes $HOST "PW=\$(sudo grep MYSQL_ROOT_PASSWORD /home/ubuntu/bigtree/deploy/.env | cut -d= -f2); sudo docker exec bt-mysql mysql -uroot -p\"\$PW\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}

log "== 0. 清场（上轮残留）=="
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
if [ -n "$UID_" ] && [ "$UID_" != "id" ]; then
  sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_); DELETE FROM t_entry_image WHERE user_id=$UID_; DELETE FROM t_entry WHERE user_id=$UID_; DELETE FROM t_study_session WHERE user_id=$UID_; DELETE FROM t_tag WHERE user_id=$UID_; DELETE FROM t_subject WHERE user_id=$UID_; DELETE FROM t_check_in WHERE user_id=$UID_; DELETE FROM t_verify_code WHERE email='$EMAIL'; DELETE FROM t_user WHERE id=$UID_;" >/dev/null
  log "已清理残留 id=$UID_"
fi

log ""
log "== 1. 注册（直灌验证码）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REG=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\",\"数学三\",\"计算机学科专业基础综合(408)\"]}")
TOKEN=$(echo "$REG" | field token)
AUTH="Authorization: Bearer $TOKEN"
if [ -n "$TOKEN" ]; then pass "注册拿到 token"; else fail "注册失败: $REG"; exit 1; fi

log ""
log "== 2. 上传图片 → 验证预签名 host（裂图修复核心）=="
UP1=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$IMG1")
URL1=$(echo "$UP1" | field previewUrl)
KEY1=$(echo "$UP1" | field objectKey)
UP2=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$IMG2")
URL2=$(echo "$UP2" | field previewUrl)
KEY2=$(echo "$UP2" | field objectKey)
log "previewUrl1=$URL1"
case "$URL1" in
  *"$BASE/"*) pass "预签名 host 是公网地址（非 localhost:9000）" ;;
  *) fail "预签名 host 不对: $URL1" ;;
esac
SZ=$(curl -s --noproxy "*" -o /tmp/smoke_img1.bin -w "%{http_code} %{content_type} %{size_download}" "$URL1")
case "$SZ" in
  "200 image/"*) N=${SZ##* }; [ "$N" -gt 1000 ] && pass "公网取图 OK ($N bytes)" || fail "图片字节异常: $SZ" ;;
  *) fail "取图响应异常: $SZ" ;;
esac
SZ2=$(curl -s --noproxy "*" -o /tmp/smoke_img2.bin -w "%{http_code} %{content_type} %{size_download}" "$URL2")
case "$SZ2" in
  "200 image/"*) pass "第二张图 OK" ;; *) fail "取图2异常: $SZ2" ;; esac
CODE=$(curl -s --noproxy "*" -o /dev/null -w "%{http_code}" "${URL1}bad")
[ "$CODE" = "403" ] && pass "篡改签名被拒 (403)" || fail "篡改签名未被拒: HTTP $CODE"

log ""
log "== 3. 计时 + Done 落库（含图）=="
SID=$(sql "SELECT id FROM t_subject WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL') AND name='英语一';" | tail -1)
START=$(curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d "{\"subjectId\": $SID}")
SESS=$(echo "$START" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
[ -n "$SESS" ] && pass "session id=$SESS" || fail "开计时失败: $START"
curl -s -X POST "$API/sessions/$SESS/finish" -H "$AUTH" -H "Content-Type: application/json" -d '{"durationSeconds": 1800}' >/dev/null
DONE=$(curl -s -X POST "$API/sessions/$SESS/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"冒烟测试条目：路线②图片修复验证\",\"tagNames\":[\"冒烟\"],\"imageObjectKeys\":[\"$KEY1\"]},{\"type\":2,\"content\":\"\",\"tagNames\":[],\"imageObjectKeys\":[\"$KEY2\"]}]}")
echo "$DONE" | grep -q '"code":0' && pass "Done 落库成功" || fail "Done 失败: $DONE"

log ""
log "== 4. 复习页裂图补测（next_due_at 造成本天）=="
sql "UPDATE t_entry SET next_due_at=DATE(NOW() - INTERVAL 4 HOUR) WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL');" >/dev/null
DUE=$(curl -s "$API/reviews/due" -H "$AUTH")
echo "$DUE" | grep -q '"code":0' && pass "reviews/due 正常" || fail "reviews/due 异常: $DUE"
IMG_URLS=$(echo "$DUE" | grep -o "$BASE/exam-timeline/[^\"]*" | sort -u)
if [ -z "$IMG_URLS" ]; then
  fail "复习列表里没有图片 URL（裂图补测未覆盖）"
else
  OK_ALL=1
  for u in $IMG_URLS; do
    RC=$(curl -s --noproxy "*" -o /tmp/smoke_due.bin -w "%{http_code} %{content_type}" "$u")
    case "$RC" in
      "200 image/"*) log "  取图 OK: ${u:0:80}..." ;;
      *) fail "复习页取图失败: $RC <- $u"; OK_ALL=0 ;;
    esac
  done
  [ "$OK_ALL" = "1" ] && pass "复习页全部图片可取（裂图修复生效）"
fi
E1=$(sql "SELECT id FROM t_entry WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL') ORDER BY id LIMIT 1;" | tail -1)
C1=$(curl -s -X POST "$API/reviews/$E1/complete" -H "$AUTH")
echo "$C1" | grep -q '"code":0' && pass "复习完成接口 OK" || fail "复习完成失败: $C1"

log ""
log "== 5. 周边接口快查 =="
for ep in "subjects" "home" "progress" "info/overview"; do
  R=$(curl -s -o /dev/null -w "%{http_code}" "$API/$ep" -H "$AUTH")
  [ "$R" = "200" ] && pass "GET /$ep => 200" || fail "GET /$ep => HTTP $R"
done
CK=$(curl -s -X POST "$API/check-ins" -H "$AUTH")
echo "$CK" | grep -q '"code":' && pass "签到接口响应正常" || fail "签到异常: $CK"

log ""
log "== 6. 资料接口（头像预签名与图片同链路，第 2 节已验）=="
PF=$(curl -s -o /dev/null -w "%{http_code}" "$API/profile" -H "$AUTH")
[ "$PF" = "200" ] && pass "GET /profile => 200" || fail "GET /profile => HTTP $PF"

log ""
log "== 结果: PASS=$PASS_CNT FAIL=$FAIL_CNT =="
[ "$FAIL_CNT" -eq 0 ] && log "全链路冒烟通过 ✅" || log "有失败项 ❌"
exit $FAIL_CNT
