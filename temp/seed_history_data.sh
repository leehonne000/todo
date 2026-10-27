#!/usr/bin/env bash
# 历史数据种子：给 uid=3（3537572282@qq.com）造艾宾浩斯全路线测试数据
# 覆盖：四科目 × 有/无标签 × 有/无图片 × round 0~6 全阶段 × 今天到期队列 5 条（含 1 条复习即出队）
# 幂等：先删 id>=101 的种子数据再插；不动用户原有数据（entry id 9/10）
set -u
API="http://localhost:8080/api"
D="D:/todo/temp"
TUID=3
OUT="$D/seed_history.log"
: > "$OUT"
log() { echo "$@" | tee -a "$OUT"; }
sql() {
  docker exec exam-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 exam_timeline -e \"$1\"" 2>/dev/null | grep -v "Using a password"
}

log "== 0. 清旧种子（id>=101），保留用户原数据 =="
sql "DELETE FROM t_entry_image WHERE user_id=$TUID AND entry_id>=101;
     DELETE FROM t_entry_tag WHERE entry_id>=101;
     DELETE FROM t_entry WHERE user_id=$TUID AND id>=101;
     DELETE FROM t_study_session WHERE user_id=$TUID AND id>=101;
     DELETE FROM t_tag WHERE user_id=$TUID AND id>=101;" >/dev/null

log ""
log "== 1. mint JWT（uid=3）=="
TOKEN=""
for SECRET in "${JWT_SECRET:-}" "exam-timeline-dev-secret-change-me-in-prod-please-32bytes"; do
  [ -z "$SECRET" ] && continue
  T=$(SECRET="$SECRET" python - <<'PYEOF'
import hmac, hashlib, base64, json, os, time
def b64(b): return base64.urlsafe_b64encode(b).rstrip(b'=').decode()
sec = os.environ['SECRET'].encode()
head = b64(json.dumps({"alg":"HS384"}, separators=(',',':')).encode())
now = int(time.time())
pay = b64(json.dumps({"sub":str(3),"iat":now,"exp":now+7*86400}, separators=(',',':')).encode())
sig = b64(hmac.new(sec, f'{head}.{pay}'.encode(), hashlib.sha384).digest())
print(f'{head}.{pay}.{sig}')
PYEOF
)
  CODE=$(curl -s "$API/profile" -H "Authorization: Bearer $T" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
  if [ "$CODE" = "0" ]; then TOKEN="$T"; log "JWT 签发成功（secret 已打码）"; break; fi
done
[ -n "$TOKEN" ] || { log "FAIL: JWT 两种 secret 都被拒"; exit 1; }
AUTH="Authorization: Bearer $TOKEN"

log ""
log "== 2. 测试图片（6 张）=="
python - <<'PYEOF'
import struct, zlib
def png(path, rgb):
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    w = h = 8
    raw = b''.join(b'\x00' + bytes(rgb) * w for _ in range(h))
    data = b'\x89PNG\r\n\x1a\n'
    data += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(raw))
    data += chunk(b'IEND', b'')
    open(path, 'wb').write(data)
colors = [(200,80,80),(80,160,220),(230,180,60),(110,190,110),(170,120,210),(230,120,170)]
for i, c in enumerate(colors, 1):
    png(f"D:/todo/temp/seed_img{i}.png", c)
print("6 张 PNG 已生成")
PYEOF

log ""
log "== 3. SQL：10 个 session + 11 条 entry（id>=101）+ 标签 =="
sql "INSERT INTO t_study_session (id,user_id,subject_id,started_at,ended_at,duration_seconds,created_at) VALUES
 (101,$TUID,5,'2026-10-05 09:00:00','2026-10-05 10:30:00',5400,'2026-10-05 09:00:00'),
 (102,$TUID,6,'2026-09-30 14:00:00','2026-09-30 15:20:00',4800,'2026-09-30 14:00:00'),
 (103,$TUID,7,'2026-10-20 19:00:00','2026-10-20 20:00:00',3600,'2026-10-20 19:00:00'),
 (104,$TUID,8,'2026-10-26 15:00:00','2026-10-26 16:15:00',4500,'2026-10-26 15:00:00'),
 (105,$TUID,8,'2026-10-25 15:30:00','2026-10-25 16:35:00',3900,'2026-10-25 15:30:00'),
 (106,$TUID,7,'2026-10-23 20:00:00','2026-10-23 20:55:00',3300,'2026-10-23 20:00:00'),
 (107,$TUID,6,'2026-10-20 09:30:00','2026-10-20 10:40:00',4200,'2026-10-20 09:30:00'),
 (108,$TUID,6,'2026-09-20 10:00:00','2026-09-20 11:25:00',5100,'2026-09-20 10:00:00'),
 (109,$TUID,7,'2026-10-27 08:00:00','2026-10-27 08:45:00',2700,'2026-10-27 08:00:00'),
 (110,$TUID,6,'2026-10-27 09:10:00','2026-10-27 10:00:00',3000,'2026-10-27 09:10:00');" >/dev/null

sql "INSERT INTO t_tag (id,user_id,name,created_at) VALUES
 (101,$TUID,'长难句','2026-10-20 20:11:00'),
 (102,$TUID,'数据结构','2026-10-25 16:51:00'),
 (103,$TUID,'阅读','2026-10-23 21:01:00'),
 (104,$TUID,'极限','2026-10-20 10:51:00'),
 (105,$TUID,'错题本','2026-09-20 11:31:00'),
 (106,$TUID,'作文模板','2026-10-27 08:51:00');" >/dev/null

# 艾宾浩斯：next_due_at = 记录日 + INTERVALS[round]（1/2/4/7/15/22）
sql "INSERT INTO t_entry (id,session_id,user_id,subject_id,type,content,round,next_due_at,review_status,created_at,updated_at) VALUES
 (101,101,$TUID,5,1,'矛盾的普遍性与特殊性：普遍性寓于特殊性之中',3,'2026-10-12',0,'2026-10-05 21:30:00','2026-10-05 21:30:00'),
 (102,102,$TUID,6,2,'定积分换元必换限：x=sinθ 时上下限同步变换',4,'2026-10-15',0,'2026-09-30 21:00:00','2026-09-30 21:00:00'),
 (103,103,$TUID,7,1,'长难句拆分三步：找谓语→划从句→调语序',2,'2026-10-24',0,'2026-10-20 20:10:00','2026-10-20 20:10:00'),
 (104,104,$TUID,8,1,'页面置换：OPT 理论最优，LRU 近似最好，FIFO 可能 Belady 异常',0,'2026-10-27',0,'2026-10-26 16:30:00','2026-10-26 16:30:00'),
 (105,101,$TUID,5,2,'剩余价值率与利润率分母不同（m/v 与 m/(c+v)）',5,'2026-10-27',0,'2026-10-05 21:45:00','2026-10-05 21:45:00'),
 (106,105,$TUID,8,1,'二叉树：中序+前序可唯一还原，前序+后序不行',2,'2026-10-29',0,'2026-10-25 16:50:00','2026-10-25 16:50:00'),
 (107,106,$TUID,7,2,'态度题排除 indifferent，作者态度多为肯定或批判',3,'2026-10-30',0,'2026-10-23 21:00:00','2026-10-23 21:00:00'),
 (108,107,$TUID,6,1,'夹逼准则：三明治放缩，注意等号成立条件',5,'2026-11-11',0,'2026-10-20 10:50:00','2026-10-20 10:50:00'),
 (109,108,$TUID,6,1,'泰勒展开常用：e^x、sinx、ln(1+x) 在 x=0 展开到三阶',6,NULL,1,'2026-09-20 11:30:00','2026-09-20 11:30:00'),
 (110,109,$TUID,7,1,'小作文：建议信三段式——目的→建议→期待',0,'2026-10-28',0,'2026-10-27 08:50:00','2026-10-27 08:50:00'),
 (111,110,$TUID,6,2,NULL,0,'2026-10-28',0,'2026-10-27 10:10:00','2026-10-27 10:10:00');" >/dev/null

sql "INSERT INTO t_entry_tag (entry_id,tag_id,created_at) VALUES
 (101,2,'2026-10-05 21:31:00'),
 (103,101,'2026-10-20 20:11:00'),
 (104,1,'2026-10-26 16:31:00'),
 (106,102,'2026-10-25 16:51:00'),
 (107,103,'2026-10-23 21:01:00'),
 (107,101,'2026-10-23 21:02:00'),
 (108,104,'2026-10-20 10:51:00'),
 (109,105,'2026-09-20 11:31:00'),
 (110,106,'2026-10-27 08:51:00');" >/dev/null
log "SQL 落库完成（session 10 / entry 11 / tag 6 / link 9）"

log ""
log "== 4. 6 张图挂到记录（上传 tmp → PUT 归档，走真实链路）=="
declare -A IMGS=( [102]=1 [104]=2 [105]=3 [107]=4 [109]=5 [111]=6 )
declare -A TAGS=( [102]='[]' [104]='["页面置换算法"]' [105]='[]' [107]='["阅读","长难句"]' [109]='["错题本"]' [111]='[]' )
declare -A CONTENT=( [102]='定积分换元必换限：x=sinθ 时上下限同步变换' [104]='页面置换：OPT 理论最优，LRU 近似最好，FIFO 可能 Belady 异常' [105]='剩余价值率与利润率分母不同（m/v 与 m/(c+v)）' [107]='态度题排除 indifferent，作者态度多为肯定或批判' [109]='泰勒展开常用：e^x、sinx、ln(1+x) 在 x=0 展开到三阶' [111]='' )
for EID in 102 104 105 107 109 111; do
  I=${IMGS[$EID]}
  UP=$(curl -s -X POST "$API/uploads/images" -H "$AUTH" -F "file=@$D/seed_img$I.png")
  K=$(echo "$UP" | sed -n 's/.*"objectKey":"\([^"]*\)".*/\1/p')
  [ -n "$K" ] || { log "FAIL: e$EID 上传失败 $UP"; exit 1; }
  PUT=$(curl -s -X PUT "$API/entries/$EID" -H "$AUTH" -H "Content-Type: application/json" \
    -d "{\"content\":\"${CONTENT[$EID]}\",\"tagNames\":${TAGS[$EID]},\"imageObjectKeys\":[\"$K\"]}")
  RC=$(echo "$PUT" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
  [ "$RC" = "0" ] && log "e$EID 图挂好（$K）" || { log "FAIL: e$EID PUT 失败 $PUT"; exit 1; }
done

log ""
log "== 5. 核对 =="
sql "SELECT id,subject_id,type,round,next_due_at,review_status FROM t_entry WHERE user_id=$TUID ORDER BY id;" | tee -a "$OUT"
DUE=$(curl -s "$API/reviews/due" -H "$AUTH")
echo "到期队列 JSON：$DUE" >> "$OUT"
CNT=$(sql "SELECT COUNT(*) FROM t_entry WHERE user_id=$TUID;" | tail -1)
log "总记录数=$CNT（期望 13 = 原 2 + 种子 11）"
rm -f "$D"/seed_img*.png
log "种子完成"
