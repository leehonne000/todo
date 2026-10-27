#!/usr/bin/env bash
# 进度模块统计验证（契约 §8.1 GET /api/progress）
# 重点：学习日 04:00 归桶边界、各科精确汇总、note/错题分开计数、checkInDates、缺省参数、参数校验
set -u
OUT="D:/todo/temp/verify_progress.txt"
API="http://localhost:8080/api"
EMAIL="test_progress@example.com"
PASS="Test-12345"
D="D:/todo/temp"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }
PASS_N=0; FAIL_N=0
ok()   { PASS_N=$((PASS_N+1)); log "PASS: $1"; }
fail() { FAIL_N=$((FAIL_N+1)); log "FAIL: $1"; }

log "== 0. 等服务就绪 =="
for i in $(seq 1 36); do
  grep -q "Started ExamTimelineApplication" D:/todo/temp/server_boot.log 2>/dev/null && { log "服务已启动"; break; }
  grep -q "APPLICATION FAILED TO START" D:/todo/temp/server_boot.log 2>/dev/null && { log "FAIL: 启动失败"; tail -15 D:/todo/temp/server_boot.log | tee -a "$OUT"; exit 1; }
  [ "$i" -eq 36 ] && { log "FAIL: 超时"; exit 1; }
  sleep 5
done

# 清旧测试用户
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
log "== 1. 注册（政治/英语一两科目）=="
sql "INSERT INTO t_verify_code (email, code, purpose, expires_at) VALUES ('$EMAIL','135790',1,DATE_ADD(NOW(), INTERVAL 10 MINUTE));" >/dev/null
REG=$(curl -s -X POST "$API/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"examYear\":2027,\"code\":\"135790\",\"subjects\":[\"政治\",\"英语一\"]}")
TOKEN=$(echo "$REG" | field token)
AUTH="Authorization: Bearer $TOKEN"
[ -n "$TOKEN" ] && ok "注册成功" || { fail "注册失败: $REG"; exit 1; }
UID_=$(sql "SELECT id FROM t_user WHERE email='$EMAIL';" | tail -1)
SID_Z=$(sql "SELECT id FROM t_subject WHERE user_id=$UID_ AND name='政治';" | tail -1)
SID_E=$(sql "SELECT id FROM t_subject WHERE user_id=$UID_ AND name='英语一';" | tail -1)
log "UID=$UID_ 政治=$SID_Z 英语一=$SID_E"

log ""
log "== 2. 造数（SQL 直插，控制时间戳覆盖学习日 04:00 边界）=="
log "session A: 10-25 03:30 起 3600s 政治 → 学习日应归 10-24（凌晨 4 点前算前一天）"
SA=$(sql "INSERT INTO t_study_session (user_id,subject_id,started_at,ended_at,duration_seconds,created_at)
     VALUES ($UID_,$SID_Z,'2026-10-25 03:30:00','2026-10-25 04:30:00',3600,'2026-10-25 04:30:00'); SELECT LAST_INSERT_ID();" | tail -1)
log "session B: 10-25 04:00 起 1800s 英语一 → 学习日 10-25（04:00 整点算当天）"
SB=$(sql "INSERT INTO t_study_session (user_id,subject_id,started_at,ended_at,duration_seconds,created_at)
     VALUES ($UID_,$SID_E,'2026-10-25 04:00:00','2026-10-25 04:30:00',1800,'2026-10-25 04:30:00'); SELECT LAST_INSERT_ID();" | tail -1)
log "session C: 10-25 10:00 起 2400s 政治 → 学习日 10-25"
SC=$(sql "INSERT INTO t_study_session (user_id,subject_id,started_at,ended_at,duration_seconds,created_at)
     VALUES ($UID_,$SID_Z,'2026-10-25 10:00:00','2026-10-25 10:40:00',2400,'2026-10-25 10:40:00'); SELECT LAST_INSERT_ID();" | tail -1)
log "session D: 10-26 09:00 起 600s 政治 → 学习日 10-26"
SD=$(sql "INSERT INTO t_study_session (user_id,subject_id,started_at,ended_at,duration_seconds,created_at)
     VALUES ($UID_,$SID_Z,'2026-10-26 09:00:00','2026-10-26 09:10:00',600,'2026-10-26 09:10:00'); SELECT LAST_INSERT_ID();" | tail -1)

log "entry 1: 10-25 02:00 note → 学习日 10-24"
log "entry 2: 10-25 09:00 note → 学习日 10-25"
log "entry 3: 10-25 23:50 错题 → 学习日 10-25"
log "entry 4: 10-26 03:30 错题 → 学习日 10-25（凌晨 4 点前算前一天）"
sql "INSERT INTO t_entry (session_id,user_id,subject_id,type,content,round,next_due_at,review_status,created_at) VALUES
     ($SA,$UID_,$SID_Z,1,'进度验证 note A',0,'2026-10-25',0,'2026-10-25 02:00:00'),
     ($SB,$UID_,$SID_E,1,'进度验证 note B',0,'2026-10-26',0,'2026-10-25 09:00:00'),
     ($SC,$UID_,$SID_Z,2,'进度验证 错题 C',0,'2026-10-26',0,'2026-10-25 23:50:00'),
     ($SD,$UID_,$SID_Z,2,'进度验证 错题 D',0,'2026-10-26',0,'2026-10-26 03:30:00');" >/dev/null
ECNT=$(sql "SELECT COUNT(*) FROM t_entry WHERE user_id=$UID_;" | tail -1)
log "entry 落库数=$ECNT（期望 4）"

log "签到 10-24、10-26"
sql "INSERT INTO t_check_in (user_id,check_in_date,created_at) VALUES
     ($UID_,'2026-10-24','2026-10-24 08:00:00'),
     ($UID_,'2026-10-26','2026-10-26 08:00:00');" >/dev/null
log "造数完成 session=$SA/$SB/$SC/$SD"

log ""
log "== 3. 拉取统计 =="
curl -s "$API/progress?from=2026-10-24&to=2026-10-27" -H "$AUTH" > "$D/progress_range.json"
curl -s "$API/progress?from=2026-10-20&to=2026-10-23" -H "$AUTH" > "$D/progress_empty.json"
curl -s "$API/progress" -H "$AUTH" > "$D/progress_default.json"
curl -s "$API/progress?from=bad-date&to=2026-10-27" -H "$AUTH" > "$D/progress_bad.json"
curl -s "$API/progress?from=2026-10-27&to=2026-10-26" -H "$AUTH" > "$D/progress_rev.json"
curl -s "$API/progress?from=2026-10-24&to=2026-10-27" > "$D/progress_noauth.json"

log ""
log "== 4. 断言 =="
cat > "$D/verify_progress_assert.py" <<'PYEOF'
import json, sys
D = "D:/todo/temp"
PASS_N = FAIL_N = 0
def ok(msg):
    global PASS_N; PASS_N += 1; print("PASS: " + msg)
def fail(msg):
    global FAIL_N; FAIL_N += 1; print("FAIL: " + msg)
def load(name):
    with open(f"{D}/{name}", encoding="utf-8") as f:
        return json.load(f)

r = load("progress_range.json")
if r.get("code") == 0: ok("range 查询 code=0")
else: fail(f"range 查询 code={r.get('code')} msg={r.get('message')}")
days = (r.get("data") or {}).get("days", [])
check = (r.get("data") or {}).get("checkInDates", [])

if len(days) == 4: ok("days 长度=4（10-24~10-27 稠密含空天）")
else: fail(f"days 长度={len(days)} 预期 4")

def day(d):
    return next((x for x in days if x.get("date") == d), None)

d24, d25, d26, d27 = day("2026-10-24"), day("2026-10-25"), day("2026-10-26"), day("2026-10-27")

# 10-24：03:30 的 session 归前一天
if d24 and d24.get("totalSeconds") == 3600: ok("10-24 totalSeconds=3600（03:30 归前一天）")
else: fail(f"10-24 totalSeconds={d24 and d24.get('totalSeconds')} 预期 3600")
if d24 and d24.get("bySubject") == [{"subjectName": "政治", "seconds": 3600}]: ok("10-24 bySubject=[政治 3600]")
else: fail(f"10-24 bySubject={d24 and d24.get('bySubject')}")
if d24 and d24.get("noteCount") == 1 and d24.get("wrongCount") == 0: ok("10-24 note=1 wrong=0（02:00 的 note 归前一天）")
else: fail(f"10-24 counts note={d24 and d24.get('noteCount')} wrong={d24 and d24.get('wrongCount')}")

# 10-25：两科目、错题跨凌晨归桶
if d25 and d25.get("totalSeconds") == 4200: ok("10-25 totalSeconds=4200（3600+600 合并日）")
else: fail(f"10-25 totalSeconds={d25 and d25.get('totalSeconds')} 预期 4200")
if d25 and d25.get("bySubject") == [{"subjectName": "政治", "seconds": 2400}, {"subjectName": "英语一", "seconds": 1800}]:
    ok("10-25 bySubject 两科精确且降序（政治2400/英语一1800）")
else: fail(f"10-25 bySubject={d25 and d25.get('bySubject')}")
if d25 and d25.get("noteCount") == 1 and d25.get("wrongCount") == 2: ok("10-25 note=1 wrong=2（03:30 错题归 10-25）")
else: fail(f"10-25 counts note={d25 and d25.get('noteCount')} wrong={d25 and d25.get('wrongCount')}")

# 10-26 / 10-27
if d26 and d26.get("totalSeconds") == 600 and d26.get("noteCount") == 0 and d26.get("wrongCount") == 0:
    ok("10-26 totalSeconds=600 计数为 0")
else: fail(f"10-26 {d26}")
if d27 and d27.get("totalSeconds") == 0 and d27.get("bySubject") == [] and d27.get("noteCount") == 0 and d27.get("wrongCount") == 0:
    ok("10-27 空天全 0（稠密占位）")
else: fail(f"10-27 {d27}")

if check == ["2026-10-24", "2026-10-26"]: ok("checkInDates=[10-24,10-26] 升序")
else: fail(f"checkInDates={check}")

# 空区间
e = load("progress_empty.json")
edays = (e.get("data") or {}).get("days", [])
echeck = (e.get("data") or {}).get("checkInDates", [])
if e.get("code") == 0 and len(edays) == 4 and all(x.get("totalSeconds") == 0 and x.get("noteCount") == 0 and x.get("wrongCount") == 0 for x in edays) and echeck == []:
    ok("无数据区间：4 天全 0、签到空数组")
else: fail(f"空区间不对: code={e.get('code')} days={len(edays)} check={echeck}")

# 缺省参数=本月 1 日至今
dflt = load("progress_default.json")
ddays = (dflt.get("data") or {}).get("days", [])
dates = [x.get("date") for x in ddays]
if dflt.get("code") == 0 and len(ddays) == 27 and dates[0] == "2026-10-01" and dates[-1] == "2026-10-27":
    ok("缺省参数：10-01~10-27 共 27 天")
else: fail(f"缺省参数: code={dflt.get('code')} n={len(ddays)} first={dates[0] if dates else None} last={dates[-1] if dates else None}")
if "2026-10-25" in dates:
    i = dates.index("2026-10-25")
    if ddays[i].get("totalSeconds") == 4200: ok("缺省区间内 10-25 数据正确（跨默认起点不影响归桶）")
    else: fail(f"缺省区间 10-25 totalSeconds={ddays[i].get('totalSeconds')}")
else: fail("缺省区间缺 10-25")

# 参数校验
b = load("progress_bad.json")
if b.get("code") == 1001: ok("from 格式错 → 1001")
else: fail(f"from 格式错返回 code={b.get('code')}")
rev = load("progress_rev.json")
if rev.get("code") == 1001: ok("from>to → 1001")
else: fail(f"from>to 返回 code={rev.get('code')}")
na = load("progress_noauth.json")
if na.get("code") == 2001: ok("无票据 → 2001")
else: fail(f"无票据返回 code={na.get('code')}")

print(f"\n== python 断言汇总: PASS={PASS_N} FAIL={FAIL_N} ==")
sys.exit(1 if FAIL_N else 0)
PYEOF

if python "$D/verify_progress_assert.py" 2>&1 | tee -a "$OUT"; then :; fi

log ""
log "== 5. 清理测试数据 =="
sql "DELETE FROM t_entry_tag WHERE entry_id IN (SELECT id FROM t_entry WHERE user_id=$UID_);
     DELETE FROM t_entry_image WHERE user_id=$UID_;
     DELETE FROM t_entry WHERE user_id=$UID_;
     DELETE FROM t_study_session WHERE user_id=$UID_;
     DELETE FROM t_check_in WHERE user_id=$UID_;
     DELETE FROM t_tag WHERE user_id=$UID_;
     DELETE FROM t_subject WHERE user_id=$UID_;
     DELETE FROM t_verify_code WHERE email='$EMAIL';
     DELETE FROM t_user WHERE id=$UID_;" >/dev/null
REST=$(sql "SELECT (SELECT COUNT(*) FROM t_user)+(SELECT COUNT(*) FROM t_entry)+(SELECT COUNT(*) FROM t_study_session)+(SELECT COUNT(*) FROM t_check_in);" | tail -1)
log "清场后 user+entry+session+checkin 行数=$REST（期望 0）"
[ "$REST" = "0" ] && ok "测试数据已清空" || fail "清场残留 $REST 行"

log ""
grep -c "^PASS:" "$OUT" | xargs -I{} echo "PASS 数:{}" | tee -a "$OUT"
grep -c "^FAIL:" "$OUT" | xargs -I{} echo "FAIL 数:{}" | tee -a "$OUT"
