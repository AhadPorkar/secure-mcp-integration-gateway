import argparse,base64,hashlib,hmac,json,time
p=argparse.ArgumentParser();p.add_argument('--secret',required=True);p.add_argument('--subject',required=True);p.add_argument('--tenant',required=True);p.add_argument('--scope',default='');p.add_argument('--roles',default='operator');p.add_argument('--ttl',type=int,default=600);a=p.parse_args()
enc=lambda b:base64.urlsafe_b64encode(b).decode().rstrip('=')
h=enc(json.dumps({'alg':'HS256','typ':'JWT'},separators=(',',':')).encode())
claims={'iss':'https://demo.identity.local','aud':'secure-mcp-gateway','sub':a.subject,'tenant':a.tenant,'scope':a.scope,'roles':[x for x in a.roles.split(',') if x],'exp':int(time.time())+a.ttl}
pay=enc(json.dumps(claims,separators=(',',':')).encode());sig=enc(hmac.new(a.secret.encode(),f'{h}.{pay}'.encode(),hashlib.sha256).digest());print(f'{h}.{pay}.{sig}')
