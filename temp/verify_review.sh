#!/usr/bin/env bash
# Review 复习队列验证（契约 §6）：到期列表 + 复习完成推进一轮/六轮出队
# 纯文本条目即可（不碰图片链路，不需要 MinIO）
set -u
OUT="D:/todo/temp/verify_review.txt"
API="http://localhost:8080/api"
EMAIL="test_review@example.com"
PASS="Test-12345"
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

# ---- 清场：上轮残留 ----
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
  log "已清理残留用户 id=$UID_"
fi

log ""
log "== 1. 注册 + 造数据（session + 2 条文本记录）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REG=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\",\"数学三\",\"408\"]}")
TOKEN=$(echo "$REG" | field token)
AUTH="Authorization: Bearer $TOKEN"
[ -n "$TOKEN" ] && log "PASS: 注册拿到 token" || { log "FAIL: 注册失败 $REG"; exit 1; }
SID=$(sql "SELECT id FROM t_subject WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL') AND name='数学三';" | tail -1)
START=$(curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d "{\"subjectId\": $SID}")
SESS=$(echo "$START" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
curl -s -X POST "$API/sessions/$SESS/finish" -H "$AUTH" -H "Content-Type: application/json" -d '{"durationSeconds": 1800}' >/dev/null
DONE=$(curl -s -X POST "$API/sessions/$SESS/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"entries":[
    {"type":1,"content":"泰勒公式的余项形式","tagNames":["极限","中值定理"]},
    {"type":2,"content":"二重积分换元忘乘雅可比","tagNames":["二重积分"]}
  ]}')
echo "Done: $DONE" | tee -a "$OUT"
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
E1=$(sql "SELECT id FROM t_entry WHERE user_id=$UID_ ORDER BY id LIMIT 1;" | tail -1)
E2=$(sql "SELECT id FROM t_entry WHERE user_id=$UID_ ORDER BY id DESC LIMIT 1;" | tail -1)
log "entry1=$E1 entry2=$E2"

log ""
log "== 2. 到期口径：只把 entry1 拨到今天（学习日）=="
sql "UPDATE t_entry SET next_due_at=DATE(NOW() - INTERVAL 4 HOUR) WHERE id=$E1;" >/dev/null
sql "SELECT id,round,next_due_at,review_status FROM t_entry WHERE user_id=$UID_ ORDER BY id;" | tee -a "$OUT"
log "GET /api/reviews/due（预期只含 entry1）:"
DUE=$(curl -s "$API/reviews/due" -H "$AUTH")
echo "$DUE" | tee -a "$OUT"; echo "" >> "$OUT"
echo "$DUE" | grep -q "\"id\":$E1" && echo "$DUE" | grep -q "\"id\":$E2" && { log "FAIL: entry2 不该到期"; exit 1; }
echo "$DUE" | grep -q "\"id\":$E1" && log "PASS: entry1 在到期列表" || { log "FAIL: entry1 不在到期列表"; exit 1; }
echo "$DUE" | grep -q "泰勒公式的余项形式" && echo "$DUE" | grep -q "中值定理" && log "PASS: content/tags 字段齐全" || log "WARN: content/tags 未按预期返回"
echo "$DUE" | grep -q '"subjectName":"数学三"' && log "PASS: subjectName" || log "WARN: subjectName 缺失"
log "未带票据（预期 HTTP 401 / code=2001）:"
curl -s -o /tmp/rev_nobody.txt -w "HTTP=%{http_code} " "$API/reviews/due"; cat /tmp/rev_nobody.txt | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 3. 复习完成推进一轮（entry1: round 0→1，next_due_at=记录日+2）=="
C1=$(curl -s -X POST "$API/reviews/$E1/complete" -H "$AUTH")
echo "$C1" | tee -a "$OUT"; echo "" >> "$OUT"
EXPECT=$(sql "SELECT DATE(DATE(created_at) + INTERVAL 2 DAY) FROM t_entry WHERE id=$E1;" | tail -1)
# 记录日=学习日口径：DATE(created_at - INTERVAL 4 HOUR)+2
EXPECT=$(sql "SELECT DATE(DATE(created_at - INTERVAL 4 HOUR) + INTERVAL 2 DAY) FROM t_entry WHERE id=$E1;" | tail -1)
echo "$C1" | grep -q '"round":1' && log "PASS: round=1" || log "FAIL: round 不是 1"
echo "$C1" | grep -q "\"nextDueAt\":\"$EXPECT\"" && log "PASS: nextDueAt=$EXPECT（记录日+2）" || log "FAIL: nextDueAt 应为 $EXPECT"
sql "SELECT id,round,next_due_at,review_status FROM t_entry WHERE id=$E1;" | tee -a "$OUT"

log ""
log "== 4. 六轮出队（entry2: round 5→6，出队）=="
sql "UPDATE t_entry SET round=5, next_due_at=DATE(NOW() - INTERVAL 4 HOUR) WHERE id=$E2;" >/dev/null
C2=$(curl -s -X POST "$API/reviews/$E2/complete" -H "$AUTH")
echo "$C2" | tee -a "$OUT"; echo "" >> "$OUT"
echo "$C2" | grep -q '"round":6' && echo "$C2" | grep -q '"nextDueAt":null' && log "PASS: round=6 且 nextDueAt=null" || log "FAIL: 出队响应不对"
sql "SELECT id,round,next_due_at,review_status FROM t_entry WHERE id=$E2;" | tee -a "$OUT"

log ""
log "== 5. 出队后行为 =="
log "再 complete entry2（预期 code=3001 不在队列）:"
curl -s -X POST "$API/reviews/$E2/complete" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"
log "complete 不存在的记录（预期 code=3002）:"
curl -s -X POST "$API/reviews/999999/complete" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"
log "到期列表（预期空，entry1 未到期 + entry2 已出队）:"
curl -s "$API/reviews/due" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"

log ""
log "== 6. 清场 =="
sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
     DELETE FROM t_entry_image WHERE user_id=$UID_;
     DELETE FROM t_entry WHERE user_id=$UID_;
     DELETE FROM t_study_session WHERE user_id=$UID_;
     DELETE FROM t_tag WHERE user_id=$UID_;
     DELETE FROM t_subject WHERE user_id=$UID_;
     DELETE FROM t_verify_code WHERE email='$EMAIL';
     DELETE FROM t_user WHERE id=$UID_;" >/dev/null
log "测试数据已清空。详细结果见 $OUT"
