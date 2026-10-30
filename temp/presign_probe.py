# 纯 stdlib 手工 SigV4 预签名探针：验证预签名 GET 的签名是否成立
# 用法：python presign_probe.py <object_key> [connect_host] [signed_host]
#   connect_host：实际连接的 host[:port]；signed_host：签名/Host 头用的值（默认同 connect）
import hashlib, hmac, sys, urllib.request, datetime, urllib.parse

AK = "exam-minio"
SK = "ExamMinIO-2026-dev"
CONNECT = sys.argv[2] if len(sys.argv) > 2 else "124.220.182.134"
HOST = sys.argv[3] if len(sys.argv) > 3 else CONNECT
BUCKET = "exam-timeline"
REGION = "us-east-1"

key = sys.argv[1]
now = datetime.datetime.now(datetime.timezone.utc)
amzdate = now.strftime("%Y%m%dT%H%M%SZ")
datestamp = now.strftime("%Y%m%d")

def sign(key_bytes, msg):
    return hmac.new(key_bytes, msg.encode(), hashlib.sha256).digest()

def uri_encode(s, encode_slash=True):
    return urllib.parse.quote(s, safe="-._~" if encode_slash else "-._~/")

canonical_uri = "/" + BUCKET + "/" + "/".join(uri_encode(seg, False) for seg in key.split("/"))
params = {
    "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
    "X-Amz-Credential": f"{AK}/{datestamp}/{REGION}/s3/aws4_request",
    "X-Amz-Date": amzdate,
    "X-Amz-Expires": "3600",
    "X-Amz-SignedHeaders": "host",
}
canonical_query = "&".join(f"{uri_encode(k)}={uri_encode(v)}" for k, v in sorted(params.items()))
url_query = "&".join(f"{uri_encode(k)}={uri_encode(v)}" for k, v in sorted(params.items()))
canonical_request = f"GET\n{canonical_uri}\n{canonical_query}\nhost:{HOST}\n\nhost\nUNSIGNED-PAYLOAD"
scope = f"{datestamp}/{REGION}/s3/aws4_request"
string_to_sign = f"AWS4-HMAC-SHA256\n{amzdate}\n{scope}\n{hashlib.sha256(canonical_request.encode()).hexdigest()}"
k_date = sign(("AWS4" + SK).encode(), datestamp)
k_region = sign(k_date, REGION)
k_service = sign(k_region, "s3")
k_signing = sign(k_service, "aws4_request")
signature = hmac.new(k_signing, string_to_sign.encode(), hashlib.sha256).hexdigest()

url = f"http://{CONNECT}{canonical_uri}?{url_query}&X-Amz-Signature={signature}"
print("connect:", CONNECT, "| signed host:", HOST)
req = urllib.request.Request(url, headers={"Host": HOST})
try:
    with urllib.request.urlopen(req, timeout=15) as r:
        data = r.read()
        print("HTTP", r.status, r.headers.get("Content-Type"), len(data), "bytes")
except urllib.error.HTTPError as e:
    print("HTTP", e.code)
    print(e.read().decode()[:500])
