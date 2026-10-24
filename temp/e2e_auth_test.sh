#!/usr/bin/env bash
# E2E：等服务起 → 发码（真实 SMTP）→ 回读验证码 → 注册 → 登录 → 鉴权校验
set -u
OUT="D:/todo/temp/e2e_result.txt"
API="http://localhost:8080/api/auth"
EMAIL="3537572282@qq.com"
: > "$OUT"

log() { echo "$@" | tee -a "$OUT"; }

log "== 1. 等待服务启动（最多 25 分钟）=="
for i in $(seq 1 150); do
  if curl -s -o /dev/null -m 2 "http://localhost:8080/api/auth/login"; then
    log "服务已就绪（等待约 $((i*10)) 秒）"
    break
  fi
  [ "$i" -eq 150 ] && { log "FAIL: 服务未在时限内启动"; exit 1; }
  sleep 10
done

log ""
log "== 2. 发送注册验证码（真实 SMTP → $EMAIL）=="
curl -s -X POST "$API/send-code" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"purpose\":1}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 3. 从 t_verify_code 回读验证码 =="
CODE=$(docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 -N exam_timeline -e \"SELECT code FROM t_verify_code WHERE email='$EMAIL' AND purpose=1 AND used_at IS NULL ORDER BY id DESC LIMIT 1\"" \
  2>/dev/null | grep -v "Using a password" | tr -d '[:space:]')
log "code=$CODE"
[ -z "$CODE" ] && { log "FAIL: 未取到验证码（发信可能失败，看应用日志）"; exit 1; }

log ""
log "== 4. 限频校验：60 秒内再发一次应被拒 =="
curl -s -X POST "$API/send-code" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"purpose\":1}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 5. 注册（验证码 + 届数 2027 + 四门科目）=="
REG=$(curl -s -X POST "$API/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test-12345\",\"examYear\":2027,\"code\":\"$CODE\",\"subjects\":[\"政治\",\"英语一\",\"数学三\",\"计算机学科专业基础综合(408)\"]}")
echo "$REG" | tee -a "$OUT"
echo "" >> "$OUT"
TOKEN=$(echo "$REG" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

log ""
log "== 6. 重复注册应被拒 =="
curl -s -X POST "$API/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test-12345\",\"examYear\":2027,\"code\":\"$CODE\",\"subjects\":[\"政治\",\"英语一\"]}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 7. 登录（正确密码）=="
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test-12345\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 8. 登录（错误密码应失败）=="
curl -s -X POST "$API/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrong-pass\"}" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 9. JWT 鉴权（预期：带 token=404 因该接口未实现但已过鉴权；不带 token=401）=="
log "带 token:"
curl -s -o /dev/null -w "HTTP %{http_code}" -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/subjects" | tee -a "$OUT"
echo "" >> "$OUT"
log "不带 token:"
curl -s -o /dev/null -w "HTTP %{http_code}" "http://localhost:8080/api/subjects" | tee -a "$OUT"
echo "" >> "$OUT"

log ""
log "== 10. 库内落库校验 =="
docker exec exam-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 exam_timeline' \
  -e "SELECT id,email,email_verified,exam_year FROM t_user; SELECT id,user_id,name,sort_order FROM t_subject; SELECT id,email,purpose,code,used_at IS NOT NULL AS used FROM t_verify_code ORDER BY id;" \
  2>/dev/null | grep -v "Using a password" | tee -a "$OUT"

log ""
log "== 完成 =="
