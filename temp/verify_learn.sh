#!/usr/bin/env bash
# Learn 流程验证（契约 §5）：选科目 → 计时 → 上传图 → Done 落库（含艾宾浩斯队列/标签/图片归档）
set -u
OUT="D:/todo/temp/verify_learn.txt"
BASE="http://localhost:8080"
API="$BASE/api"
EMAIL="test_learn@example.com"
PASS="Test-12345"
IMG1="D:/todo/pics/background/其他/Bits + Pieces__foocngohimdoacmhbjfkdldchhiipieo.png"
IMG2="D:/todo/pics/background/娱乐/Tardis__jnpaonbghmoocnkenfjigielilfmaboj.png"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }

log "== 0. 等服务就绪（最多 3 分钟）=="
for i in $(seq 1 36); do
  grep -q "Started ExamTimelineApplication" D:/todo/temp/server_boot.log 2>/dev/null && { log "服务已启动（$((i*5))s）"; break; }
  grep -q "APPLICATION FAILED TO START" D:/todo/temp/server_boot.log 2>/dev/null && { log "FAIL: 启动失败"; grep -A 8 "APPLICATION FAILED" D:/todo/temp/server_boot.log | head -12 | tee -a "$OUT"; exit 1; }
  [ "$i" -eq 36 ] && { log "FAIL: 超时"; exit 1; }
  sleep 5
done

# ---- 清场：上轮测试残留 ----
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
if [ -n "$UID_" ] && [ "$UID_" != "id" ]; then
  sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
       DELETE FROM t_entry_image WHERE user_id=$UID_;
       DELETE FROM t_entry WHERE user_id=$UID_;
       DELETE FROM t_study_session WHERE user_id=$UID_;
       DELETE FROM t_tag WHERE user_id=$UID_;
       DELETE FROM t_subject WHERE user_id=$UID_;
       DELETE FROM t_verify_code WHERE email='$EMAIL';
       DELETE FROM t_user WHERE id=$UID_;" >/dev/null
  log "已清理残留测试用户 id=$UID_"
fi

log ""
log "== 1. 注册（直灌验证码）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REG=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\",\"数学三\",\"计算机学科专业基础综合(408)\"]}")
echo "$REG" | tee -a "$OUT"; echo "" >> "$OUT"
TOKEN=$(echo "$REG" | field token)
AUTH="Authorization: Bearer $TOKEN"
[ -n "$TOKEN" ] && log "PASS: 拿到 token" || { log "FAIL: 注册未返回 token"; exit 1; }

log ""
log "== 2. GET /api/subjects（预期 4 门科目）=="
curl -s "$API/subjects" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"
log "未带票据（预期 HTTP 401 / code=2001）:"
curl -s -o /tmp/learn_nobody.txt -w "HTTP=%{http_code} " "$API/subjects"; cat /tmp/learn_nobody.txt | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 3. POST /api/sessions 开始计时 =="
log "科目不存在（预期 code=1001）:"
curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d '{"subjectId": 99999}' | tee -a "$OUT"; echo "" >> "$OUT"
SID=$(sql "SELECT id FROM t_subject WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL') AND name='英语一';" | tail -1)
log "合法科目 subjectId=$SID（预期 code=0 + id/startedAt）:"
START=$(curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d "{\"subjectId\": $SID}")
echo "$START" | tee -a "$OUT"; echo "" >> "$OUT"
SESS_ID=$(echo "$START" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
[ -n "$SESS_ID" ] && log "PASS: session id=$SESS_ID" || { log "FAIL: 开始计时无 id"; exit 1; }

log ""
log "== 4. POST /api/sessions/$SESS_ID/finish 结束计时（净学习 3600 秒）=="
curl -s -X POST "$API/sessions/$SESS_ID/finish" -H "$AUTH" -H "Content-Type: application/json" -d '{"durationSeconds": 3600}' | tee -a "$OUT"; echo "" >> "$OUT"
sql "SELECT id,subject_id,duration_seconds,ended_at IS NOT NULL AS has_end FROM t_study_session WHERE id=$SESS_ID;" | tee -a "$OUT"
log "负数秒（预期 code=1001）:"
curl -s -X POST "$API/sessions/$SESS_ID/finish" -H "$AUTH" -H "Content-Type: application/json" -d '{"durationSeconds": -5}' | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 5. POST /api/uploads/images 上传两张图 =="
UP1=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$IMG1")
echo "$UP1" | tee -a "$OUT"; echo "" >> "$OUT"
KEY1=$(echo "$UP1" | field objectKey)
URL1=$(echo "$UP1" | field previewUrl)
UP2=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$IMG2")
KEY2=$(echo "$UP2" | field objectKey)
URL2=$(echo "$UP2" | field previewUrl)
[ -n "$KEY1" ] && [ -n "$KEY2" ] && log "PASS: objectKey1=$KEY1 / objectKey2=$KEY2" || { log "FAIL: 上传无 objectKey"; exit 1; }
echo "$UP1" | grep -q '"sizeBytes":0,' && { log "FAIL: sizeBytes=0（空文件）"; exit 1; } || log "PASS: sizeBytes 非 0"
log "预览 URL 取图（预期 HTTP 200 + 图片类型）:"
SZ=$(curl -s -o /tmp/learn_img.bin -w "%{http_code} %{content_type} %{size_download}" "$URL1"); echo "预览取图: $SZ" | tee -a "$OUT"
case "$SZ" in "200 image/"*) N=${SZ##* }; if [ "$N" -gt 1000 ]; then log "PASS: 图片字节数正常 ($N bytes)"; else log "FAIL: 图片字节数异常 ($N bytes)"; exit 1; fi;; *) log "FAIL: 取图响应异常 $SZ"; exit 1;; esac
log "篡改签名（预期 HTTP 403）:"
curl -s -o /dev/null -w "HTTP=%{http_code}\n" "${URL1}bad" | tee -a "$OUT"

log ""
log "== 6. POST /api/sessions/$SESS_ID/entries Done 批量落库 =="
BODY="{\"entries\":[
  {\"type\":1,\"content\":\"政治新增考点：实践与认识的辩证关系\",\"tagNames\":[\"马原\",\"认识论\"],\"imageObjectKeys\":[\"$KEY1\"]},
  {\"type\":2,\"content\":\"\",\"tagNames\":[\"极限\"],\"imageObjectKeys\":[\"$KEY2\"]}
]}"
DONE=$(curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" -d "$BODY")
echo "$DONE" | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 7. 落库核对 =="
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
sql "SELECT id,session_id,subject_id,type,LEFT(content,20) AS content,round,next_due_at,review_status FROM t_entry WHERE user_id=$UID_ ORDER BY id;" | tee -a "$OUT"
EXPECT_DUE=$(sql "SELECT DATE(DATE(NOW() - INTERVAL 4 HOUR) + INTERVAL 1 DAY);" | tail -1)
log "学习日+1 应为 $EXPECT_DUE（上面 next_due_at 应一致；round=0 / review_status=0）"
sql "SELECT e.id, ei.sort_order, ei.object_key, ei.content_type, ei.original_name FROM t_entry_image ei JOIN t_entry e ON ei.entry_id=e.id WHERE e.user_id=$UID_ ORDER BY ei.sort_order;" | tee -a "$OUT"
log "归档检查（object_key 应为 user_${UID_}/entry_*/ 形态）:"
echo "$UP1" | grep -o '"objectKey":"[^"]*"' | head -1 | tee -a "$OUT"
log "tmp 键在 Done 后应已删除（预期 HTTP 404）:"
curl -s -o /dev/null -w "HTTP=%{http_code}\n" "$URL1" | tee -a "$OUT"
sql "SELECT id,name FROM t_tag WHERE user_id=$UID_ ORDER BY name;" | tee -a "$OUT"
sql "SELECT COUNT(*) AS entry_tag_links FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);" | tee -a "$OUT"

log ""
log "== 8. 异常路径 =="
log "文字与图片全空（预期 code=1001）:"
curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"\",\"imageObjectKeys\":[]}]}" | tee -a "$OUT"; echo "" >> "$OUT"
log "超 10 张图（预期 code=1001）:"
ELEVEN=$(printf '"%s",' "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1" "$KEY1")
curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"x\",\"imageObjectKeys\":[${ELEVEN%,}]}]}" | tee -a "$OUT"; echo "" >> "$OUT"
log "别人的图片键（预期 code=1001 图片不属于当前用户）:"
curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"x\",\"imageObjectKeys\":[\"user_999/tmp/a.jpg\"]}]}" | tee -a "$OUT"; echo "" >> "$OUT"
log "type 非法（预期 code=1001）:"
curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":9,\"content\":\"x\"}]}" | tee -a "$OUT"; echo "" >> "$OUT"
log "会话不存在（预期 code=3002）:"
curl -s -X POST "$API/sessions/999999/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"x\"}]}" | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 9. 标签复用 + 同图重复提交 =="
UP3=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$IMG1")
KEY3=$(echo "$UP3" | field objectKey)
log "同一张图提交两次（预期 code=1001）:"
curl -s -X POST "$API/sessions/$SESS_ID/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"a\",\"imageObjectKeys\":[\"$KEY3\"]},{\"type\":2,\"content\":\"b\",\"imageObjectKeys\":[\"$KEY3\"]}]}" | tee -a "$OUT"; echo "" >> "$OUT"
log "新 session + 复用标签「马原」（预期 t_tag 不新增行）:"
START2=$(curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d "{\"subjectId\": $SID}")
SESS2=$(echo "$START2" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
curl -s -X POST "$API/sessions/$SESS2/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"entries\":[{\"type\":1,\"content\":\"复用标签测试\",\"tagNames\":[\"马原\"]}]}" | tee -a "$OUT"; echo "" >> "$OUT"
sql "SELECT COUNT(*) AS tag_rows FROM t_tag WHERE user_id=$UID_;" | tee -a "$OUT"
log "（tag_rows 仍应为 3：马原/认识论/极限）"

log ""
log "== 10. 清场 =="
sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
     DELETE FROM t_entry_image WHERE user_id=$UID_;
     DELETE FROM t_entry WHERE user_id=$UID_;
     DELETE FROM t_study_session WHERE user_id=$UID_;
     DELETE FROM t_tag WHERE user_id=$UID_;
     DELETE FROM t_subject WHERE user_id=$UID_;
     DELETE FROM t_verify_code WHERE email='$EMAIL';
     DELETE FROM t_user WHERE id=$UID_;" >/dev/null
rm -rf "D:/todo/server/data/files/user_${UID_}" 2>/dev/null
log "测试数据已清空。详细结果见 $OUT"
