from pathlib import Path
import base64,hashlib,hmac,json,os,secrets,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(ROOT/'scripts/build.py')],check=True)
secret=secrets.token_urlsafe(32)
def token(sub,tenant,scope,roles,ttl=3600):
    enc=lambda b: base64.urlsafe_b64encode(b).decode().rstrip('=')
    h=enc(json.dumps({'alg':'HS256','typ':'JWT'},separators=(',',':')).encode())
    p=enc(json.dumps({'iss':'https://demo.identity.local','aud':'secure-mcp-gateway','sub':sub,'tenant':tenant,'scope':scope,'roles':roles,'exp':int(time.time())+ttl},separators=(',',':')).encode())
    s=enc(hmac.new(secret.encode(),f'{h}.{p}'.encode(),hashlib.sha256).digest());return f'{h}.{p}.{s}'
access={'operatorAcme':token('operator-acme','acme','orders:read inventory:read documents:read orders:cancel',['operator']),'reviewerAcme':token('reviewer-acme','acme','',['approver']),'operatorBeta':token('operator-beta','beta','orders:read inventory:read documents:read orders:cancel',['operator'])}
local=ROOT/'.local';local.mkdir(exist_ok=True);(local/'access.json').write_text(json.dumps(access,indent=2),encoding='utf-8')
env=os.environ.copy();env['MCP_DEMO_HS256_SECRET']=secret;env.setdefault('MCP_GATEWAY_PORT','8096')
print('Demo access written to .local/access.json (gitignored).')
print('Open http://127.0.0.1:'+env['MCP_GATEWAY_PORT']+'/admin')
os.execvpe('java',['java','-cp',str(ROOT/'build/classes'),'io.ahad.mcpgateway.Main'],env)
