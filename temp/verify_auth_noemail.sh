#!/usr/bin/env bash
# 登录链路验证（不发邮件）：验证码行直接灌库，绕过发信环节
set -u
OUT="D:/todo/temp/verify_auth.txt"
API="http://localhost:8080/api/auth"
EMAIL="test_login@example.com"
PASS="Test-12345"
NEWPASS="Test-67890"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }

log "== 0. 等服务就绪（最多 3 分钟）=="
for i in $(seq 1 36); do
  grep -q "Started ExamTimelineApplication" D:/todo/temp/server_boot.log 2>/dev/null && { log "服务已启动（$((i*5))s）"; break; }
  grep -q "APPLICATION FAILED TO START" D:/todo/temp/server_boot.log 2>/dev/null && { log "FAIL: 启动失败"; grep -A 8 "APPLICATION FAILED" D:/todo/temp/server_boot.log | head -12 | tee -a "$OUT"; exit 1; }
  [ "$i" -eq 36 ] && { log "FAIL: 超时"; exit 1; }
  sleep 5
done

sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}

log ""
log "== 1. 登录接口连通（未注册邮箱，预期 code=2001）=="
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 2. 发码接口连通（purpose=2 未注册邮箱，预期 code=3002，不会真发信）=="
curl -s -X POST "$API/send-code" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"purpose\":2}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 3. 直灌验证码行（purpose=1）替代发信 =="
sql "DELETE FROM t_verify_code WHERE email='$EMAIL'; INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','246810',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));"

log ""
log "== 4. 注册（预期 code=0 + token）=="
REG=$(curl -s -X POST "$API/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"246810\",\"subjects\":[\"政治\",\"英语一\",\"数学三\",\"计算机学科专业基础综合(408)\"]}")
echo "$REG" | tee -a "$OUT"; echo "" >> "$OUT"
TOKEN=$(echo "$REG" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

log ""
log "== 5. 重复注册（预期 code=3001 已注册）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','135790',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));"
curl -s -X POST "$API/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"135790\",\"subjects\":[\"政治\",\"英语一\"]}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 6. 登录正确密码（预期 token）=="
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 7. 登录错误密码（预期 code=2001）=="
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrong\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 8. 找回密码（直灌 purpose=2 码 → 改密）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','112233',2,DATE_ADD(NOW(), INTERVAL 10 MINUTE));"
curl -s -X POST "$API/reset-password" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"code\":\"112233\",\"newPassword\":\"$NEWPASS\"}" | tee -a "$OUT"
echo "" >> "$OUT"
log "旧密码登录（预期 2001）:"
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | tee -a "$OUT"
echo "" >> "$OUT"
log "新密码登录（预期 token）:"
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$NEWPASS\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 9. 验证码一次性消费（重复用同码注册，预期 1001 验证码无效）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('other@example.com','999999',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));"
sql "UPDATE t_verify_code SET used_at=NOW() WHERE email='other@example.com';"
curl -s -X POST "$API/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"other@example.com\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"999999\",\"subjects\":[\"政治\",\"英语一\"]}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 10. JWT 鉴权（带 token 期望过门卫 404；不带期望 401/403）=="
log "带 token:"
curl -s -o /dev/null -w "HTTP %{http_code}" -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/subjects" | tee -a "$OUT"
echo "" >> "$OUT"
log "不带 token:"
curl -s -o /dev/null -w "HTTP %{http_code}" "http://localhost:8080/api/subjects" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 11. 落库核对 =="
sql "SELECT id,email,email_verified,exam_year FROM t_user; SELECT user_id,name,sort_order FROM t_subject ORDER BY user_id,sort_order; SELECT email,purpose,code,used_at IS NOT NULL AS used FROM t_verify_code ORDER BY id;" | tee -a "$OUT"

log ""
log "== 完成 =="
