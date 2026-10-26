#!/usr/bin/env bash
# 主界面聚合 + 签到验证（契约 §4）
set -u
OUT="D:/todo/temp/verify_home.txt"
API="http://localhost:8080/api"
EMAIL="test_home@example.com"
PASS="Test-12345"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }

log "== 0. 等服务就绪 =="
for i in $(seq 1 36); do
  grep -q "Started ExamTimelineApplication" D:/todo/temp/server_boot.log 2>/dev/null && { log "服务已启动"; break; }
  grep -q "APPLICATION FAILED TO START" D:/todo/temp/server_boot.log 2>/dev/null && { log "FAIL: 启动失败"; tail -15 D:/todo/temp/server_boot.log | tee -a "$OUT"; exit 1; }
  [ "$i" -eq 36 ] && { log "FAIL: 超时"; exit 1; }
  sleep 5
done

UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
if [ -n "$UID_" ] && [ "$UID_" != "id" ]; then
  sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
       DELETE FROM t_entry_image WHERE user_id=$UID_;
       DELETE FROM t_entry WHERE user_id=$UID_;
       DELETE FROM t_study_session WHERE user_id=$UID_;
       DELETE FROM t_check_in WHERE user_id=$UID_;
       DELETE FROM t_tag WHERE user_id=$UID_;
       DELETE FROM t_subject WHERE user_id=$UID_;
       DELETE FROM t_verify_code WHERE email='$EMAIL';
       DELETE FROM t_user WHERE id=$UID_;" >/dev/null
fi

log ""
log "== 1. 注册 =="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REG=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\"]}")
TOKEN=$(echo "$REG" | field token)
AUTH="Authorization: Bearer $TOKEN"
[ -n "$TOKEN" ] && log "PASS: 注册成功" || { log "FAIL: $REG"; exit 1; }

log ""
log "== 2. GET /api/home 初始态（预期 false/false/0）=="
H1=$(curl -s "$API/home" -H "$AUTH"); echo "$H1" | tee -a "$OUT"; echo "" >> "$OUT"
echo "$H1" | grep -q '"checkedInToday":false' && echo "$H1" | grep -q '"todayLearned":false' && echo "$H1" | grep -q '"dueReviewCount":0' && log "PASS: 初始态正确" || log "FAIL: 初始态不对"

log ""
log "== 3. POST /api/check-ins 签到 =="
curl -s -X POST "$API/check-ins" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"
log "重复签到（预期 code=3001 今天已签到）:"
curl -s -X POST "$API/check-ins" -H "$AUTH" | tee -a "$OUT"; echo "" >> "$OUT"
H2=$(curl -s "$API/home" -H "$AUTH")
echo "$H2" | grep -q '"checkedInToday":true' && log "PASS: 签到态翻真" || log "FAIL: 签到态没变"

log ""
log "== 4. 学习后 todayLearned 翻真 + dueReviewCount =="
SID=$(sql "SELECT id FROM t_subject WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL') LIMIT 1;" | tail -1)
START=$(curl -s -X POST "$API/sessions" -H "$AUTH" -H "Content-Type: application/json" -d "{\"subjectId\": $SID}")
SESS=$(echo "$START" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
curl -s -X POST "$API/sessions/$SESS/finish" -H "$AUTH" -H "Content-Type: application/json" -d '{"durationSeconds": 600}' >/dev/null
curl -s -X POST "$API/sessions/$SESS/entries" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"entries":[{"type":1,"content":"home 测试条目","tagNames":["测试"]}]}' >/dev/null
H3=$(curl -s "$API/home" -H "$AUTH"); echo "$H3" | tee -a "$OUT"; echo "" >> "$OUT"
echo "$H3" | grep -q '"todayLearned":true' && log "PASS: 今日学习翻真" || log "FAIL: todayLearned 没翻真"
echo "$H3" | grep -q '"dueReviewCount":0' && log "PASS: 到期数仍 0（明天才首到期）" || log "FAIL: 到期数不对（未到期不该计入）"
sql "UPDATE t_entry SET next_due_at=DATE(NOW() - INTERVAL 4 HOUR) WHERE user_id=(SELECT id FROM t_user WHERE email='$EMAIL');" >/dev/null
H4=$(curl -s "$API/home" -H "$AUTH")
echo "$H4" | grep -q '"dueReviewCount":1' && log "PASS: 拨到期后计数=1" || { log "FAIL: 到期计数不对"; echo "$H4" | tee -a "$OUT"; }

log ""
log "== 5. 未登录 401 =="
curl -s -o /dev/null -w "home_no_token: HTTP=%{http_code}\n" "$API/home" | tee -a "$OUT"
curl -s -o /dev/null -w "checkin_no_token: HTTP=%{http_code}\n" -X POST "$API/check-ins" | tee -a "$OUT"

log ""
log "== 6. 清场 =="
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
     DELETE FROM t_entry_image WHERE user_id=$UID_;
     DELETE FROM t_entry WHERE user_id=$UID_;
     DELETE FROM t_study_session WHERE user_id=$UID_;
     DELETE FROM t_check_in WHERE user_id=$UID_;
     DELETE FROM t_tag WHERE user_id=$UID_;
     DELETE FROM t_subject WHERE user_id=$UID_;
     DELETE FROM t_verify_code WHERE email='$EMAIL';
     DELETE FROM t_user WHERE id=$UID_;" >/dev/null
log "测试数据已清空。"
