#!/usr/bin/env bash
# 文章留言验证（契约 §9.4~9.6 / §11.14~11.17）——读者来信
# 重点：多轮会话、可见性铁律（管理员互不可见/用户互不可见/超管全见）、红点已读口径、
#       回复权（仅文章作者本人，超管可看不可代回复）、删除权限（用户不可删）、内容校验、不能给自己的文章留言
set -u
OUT="D:/todo/temp/verify_messages.txt"
API="http://localhost:8080/api"
PASS="Test-12345"
D="D:/todo/temp"
EMAIL_A="test_msg_admin1@example.com"   # 文章作者（管理员1）
EMAIL_B="test_msg_admin2@example.com"   # 管理员2（互不可见验证）
EMAIL_S="test_msg_super@example.com"    # 超管
EMAIL_U="test_msg_usera@example.com"    # 留言用户 A
EMAIL_V="test_msg_userb@example.com"    # 用户 B（互不可见验证）
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }
num() { sed -n "s/.*\"$1\":\([0-9]*\).*/\1/p"; } # 数字字段（如 "id":2）

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
    sql "DELETE FROM t_message WHERE user_id=$uid;
         DELETE FROM t_verify_code WHERE email='$email';
         DELETE FROM t_user WHERE id=$uid;" >/dev/null
  fi
}

log ""
log "== 1. 注册五个账号并造角色 =="
for e in "$EMAIL_A" "$EMAIL_B" "$EMAIL_S" "$EMAIL_U" "$EMAIL_V"; do cleanup_user "$e"; done
reg_login() { # $1=email $2=code -> token 到 $3
  sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$1','$2',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
  curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"$2\",\"subjects\":[\"政治\",\"英语一\"]}" > "$D/msg_reg_$1.json"
  eval "$3=\$(field token < \"$D/msg_reg_$1.json\")"
}
login_only() {
  curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\"}" > "$D/msg_login_$1.json"
  eval "$2=\$(field token < \"$D/msg_login_$1.json\")"
}
reg_login "$EMAIL_A" "246810" TOKEN_A
reg_login "$EMAIL_B" "135790" TOKEN_B
reg_login "$EMAIL_S" "112233" TOKEN_S
reg_login "$EMAIL_U" "445566" TOKEN_U
reg_login "$EMAIL_V" "556677" TOKEN_V
UID_A=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_A';" | tail -1)
UID_B=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_B';" | tail -1)
UID_S=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_S';" | tail -1)
UID_U=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_U';" | tail -1)
UID_V=$(sql "SELECT id FROM t_user WHERE email='$EMAIL_V';" | tail -1)
sql "UPDATE t_user SET role=1 WHERE id IN ($UID_A,$UID_B); UPDATE t_user SET role=2 WHERE id=$UID_S;" >/dev/null
login_only "$EMAIL_A" TOKEN_A; login_only "$EMAIL_B" TOKEN_B; login_only "$EMAIL_S" TOKEN_S
AUTH_A="Authorization: Bearer $TOKEN_A"; AUTH_B="Authorization: Bearer $TOKEN_B"
AUTH_S="Authorization: Bearer $TOKEN_S"; AUTH_U="Authorization: Bearer $TOKEN_U"; AUTH_V="Authorization: Bearer $TOKEN_V"
[ -n "$TOKEN_A" ] && [ -n "$TOKEN_U" ] && [ -n "$TOKEN_S" ] && log "PASS: 五账号就绪（A=$UID_A B=$UID_B S=$UID_S U=$UID_U V=$UID_V）" || { log "FAIL: 注册"; exit 1; }

log ""
log "== 2. 造测试文章（A 署名 911，B 署名 912）=="
sql "DELETE FROM t_message WHERE article_id IN (911,912); DELETE FROM t_article WHERE id IN (911,912);" >/dev/null
sql "INSERT INTO t_article (id,author_id,title,content,status,published_at,created_at,updated_at) VALUES
 (911,$UID_A,'留言测试文章A','内容A',1,NOW(),NOW(),NOW()),
 (912,$UID_B,'留言测试文章B','内容B',1,NOW(),NOW(),NOW());" >/dev/null
log "PASS: 文章 911（作者A）/ 912（作者B）"

log ""
log "== 3. 用户 U 发留言（多轮 2 条）=="
R=$(curl -s -X POST "$API/articles/911/messages" -H "$AUTH_U" -H "Content-Type: application/json" -d '{"content":"老师好，往届生预报名报不上吗？"}')
M1=$(echo "$R" | num id)
R=$(curl -s -X POST "$API/articles/911/messages" -H "$AUTH_U" -H "Content-Type: application/json" -d '{"content":"补充：户籍在外地。"}')
M2=$(echo "$R" | num id)
[ -n "$M1" ] && [ -n "$M2" ] && log "PASS: 两条留言入库（$M1/$M2）" || log "FAIL: 发留言 $R"
echo "$R" | grep -q '"senderName"' && echo "$R" | grep -q '"senderAvatar"' && log "PASS: 发留言返回带发送者昵称/头像" || log "FAIL: 发送者身份 $R"

log ""
log "== 4. 校验：空内容 / 超长 / 自己文章 / 未发布文章 =="
R=$(curl -s -X POST "$API/articles/911/messages" -H "$AUTH_U" -H "Content-Type: application/json" -d '{"content":"  "}')
echo "$R" | grep -q '"code":1001' && log "PASS: 空内容 1001" || log "FAIL: 空内容 $R"
LONG=$(python -c "print('字'*301)" 2>/dev/null || printf '字%.0s' {1..301})
R=$(curl -s -X POST "$API/articles/911/messages" -H "$AUTH_U" -H "Content-Type: application/json" -d "{\"content\":\"$LONG\"}")
echo "$R" | grep -q '"code":1001' && log "PASS: 超 300 字 1001" || log "FAIL: 超长 $R"
R=$(curl -s -X POST "$API/articles/911/messages" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"content":"自己给自己留言"}')
echo "$R" | grep -q '"code":1001' && log "PASS: 给自己文章留言 1001" || log "FAIL: 自己文章 $R"
R=$(curl -s -X POST "$API/articles/999999/messages" -H "$AUTH_U" -H "Content-Type: application/json" -d '{"content":"不存在的文章"}')
echo "$R" | grep -q '"code":3002' && log "PASS: 文章不存在 3002" || log "FAIL: 不存在文章 $R"
R=$(curl -s -X POST "$API/articles/911/messages" -H "Content-Type: application/json" -d '{"content":"x"}')
echo "$R" | grep -q '"code":2001' && log "PASS: 未登录 2001" || log "FAIL: 未登录 $R"

log ""
log "== 5. 管理员 A 看会话（含红点计数）=="
R=$(curl -s "$API/admin/messages" -H "$AUTH_A")
echo "$R" | grep -q '"unreadCount":2' && log "PASS: A 的会话 unreadCount=2" || log "FAIL: A 会话 $R"
echo "$R" | grep -q '"userAvatar"' && log "PASS: 会话列表带用户头像" || log "FAIL: 会话头像 $R"
R=$(curl -s "$API/admin/messages/unread" -H "$AUTH_A")
echo "$R" | grep -q '"count":2' && log "PASS: A 侧栏红点 count=2" || log "FAIL: A 红点 $R"

log ""
log "== 6. 可见性铁律 =="
# 6.1 管理员 B 看不到 A 的会话
R=$(curl -s "$API/admin/messages" -H "$AUTH_B")
echo "$R" | grep -q '"total":0' && log "PASS: 管理员 B 会话列表为空（互不可见）" || log "FAIL: B 列表 $R"
R=$(curl -s "$API/admin/messages/911/$UID_U" -H "$AUTH_B")
echo "$R" | grep -q '"code":2002' && log "PASS: B 打开 A 的会话 2002" || log "FAIL: B 详情 $R"
R=$(curl -s -X POST "$API/admin/messages/911/$UID_U" -H "$AUTH_B" -H "Content-Type: application/json" -d '{"content":"我是B，来串门"}')
echo "$R" | grep -q '"code":2002' && log "PASS: B 回复 A 的会话 2002" || log "FAIL: B 回复 $R"
# 6.2 用户 V 看不到 U 的对话
R=$(curl -s "$API/articles/911/messages" -H "$AUTH_V")
echo "$R" | grep -q '"messages":\[\]' && log "PASS: 用户 V 对话为空（互不可见）" || log "FAIL: V 对话 $R"
# 6.3 超管全见 + 按管理员筛选
R=$(curl -s "$API/admin/messages?authorId=$UID_A" -H "$AUTH_S")
echo "$R" | grep -q '"articleId":911' && log "PASS: 超管按管理员筛选见 A 的会话" || log "FAIL: 超管筛选 $R"
# 6.4 回复权=仅文章作者本人：超管可看全部但不可代回复（2026-10-28 权限定稿）
R=$(curl -s -X POST "$API/admin/messages/911/$UID_U" -H "$AUTH_S" -H "Content-Type: application/json" -d '{"content":"超管代回复测试"}')
echo "$R" | grep -q '"code":2002' && log "PASS: 超管不可代回复（2002）" || log "FAIL: 超管代回复 $R"

log ""
log "== 7. 管理员 A 回复 → 用户 U 红点 =="
R=$(curl -s -X POST "$API/admin/messages/911/$UID_U" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"content":"往届生走正式报名即可，10月15日开放。"}')
M3=$(echo "$R" | num id)
[ -n "$M3" ] && log "PASS: A 回复入库（$M3）" || log "FAIL: A 回复 $R"
R=$(curl -s "$API/messages/unread" -H "$AUTH_U")
echo "$R" | grep -q '"count":1' && log "PASS: U 未读回复 count=1（红点亮）" || log "FAIL: U 红点 $R"

log ""
log "== 8. 用户 U 打开对话（自动已读）=="
R=$(curl -s "$API/articles/911/messages" -H "$AUTH_U")
echo "$R" | grep -q '"senderRole":1' && log "PASS: U 看到管理员回复" || log "FAIL: U 对话 $R"
echo "$R" | grep -q '"senderAvatar":"/avatar-default.png"' && log "PASS: 对话条目带发送者头像（默认图口径）" || log "FAIL: 对话头像 $R"
R=$(curl -s "$API/messages/unread" -H "$AUTH_U")
echo "$R" | grep -q '"count":0' && log "PASS: U 红点消（count=0）" || log "FAIL: U 消音 $R"
R=$(curl -s "$API/admin/messages/911/$UID_U" -H "$AUTH_A")
echo "$R" | grep -q '"senderRole":0' && log "PASS: A 会话详情含用户留言（多轮完整）" || log "FAIL: A 详情 $R"
R=$(curl -s "$API/admin/messages/unread" -H "$AUTH_A")
echo "$R" | grep -q '"count":0' && log "PASS: A 红点消（打开详情即已读）" || log "FAIL: A 消音 $R"

log ""
log "== 9. 删除权限 =="
R=$(curl -s -X DELETE "$API/admin/messages/$M1" -H "$AUTH_U")
echo "$R" | grep -q '"code":2002' && log "PASS: 用户 U 不可删（2002）" || log "FAIL: U 删 $R"
R=$(curl -s -X DELETE "$API/admin/messages/$M1" -H "$AUTH_B")
echo "$R" | grep -q '"code":2002' && log "PASS: 管理员 B 不可删 A 的留言（2002）" || log "FAIL: B 删 $R"
R=$(curl -s -X DELETE "$API/admin/messages/$M1" -H "$AUTH_S")
echo "$R" | grep -q '"code":0' && log "PASS: 超管可删任意留言" || log "FAIL: S 删 $R"
R=$(curl -s -X DELETE "$API/admin/messages/$M2" -H "$AUTH_A")
echo "$R" | grep -q '"code":0' && log "PASS: A 可删自己文章的留言" || log "FAIL: A 删 $R"

log ""
log "== 10. 清理测试数据 =="
sql "DELETE FROM t_message WHERE article_id IN (911,912); DELETE FROM t_article WHERE id IN (911,912);" >/dev/null
for e in "$EMAIL_A" "$EMAIL_B" "$EMAIL_S" "$EMAIL_U" "$EMAIL_V"; do cleanup_user "$e"; done
log "已清理"
log ""
log "== 验证完成 =="
