package io.ahad.mcpgateway;

public final class AdminAssets {
    private AdminAssets() {}

    public static String css() {
        return "body{font-family:Inter,Segoe UI,sans-serif;margin:0;background:#0b1020;color:#e8edf8}.wrap{max-width:1100px;margin:auto;padding:32px}.hero{background:linear-gradient(135deg,#17203c,#11172a);border:1px solid #273356;border-radius:20px;padding:28px}.tag{display:inline-block;padding:6px 10px;border-radius:999px;background:#173b32;color:#9ff5cf;font-size:12px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:18px}.card{background:#11172a;border:1px solid #273356;border-radius:16px;padding:18px}.muted{color:#9da9c6}.row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}input,button{background:#0d1427;color:#e8edf8;border:1px solid #334264;border-radius:10px;padding:10px}input{min-width:420px}button{cursor:pointer}.ok{color:#8ce5bd}.bad{color:#ff9e9e}.approval{border-top:1px solid #273356;padding-top:14px;margin-top:14px}.approval:first-child{border-top:0;padding-top:0;margin-top:0}pre{white-space:pre-wrap;word-break:break-word;background:#090e1c;padding:12px;border-radius:10px;max-height:420px;overflow:auto}@media(max-width:800px){.grid{grid-template-columns:1fr}input{min-width:0;width:100%}}";
    }

    public static String js() {
        return """
const byId=(x)=>document.getElementById(x);
const token=()=>byId('token').value;
const headers=()=>({'Authorization':'Bearer '+token(),'Content-Type':'application/json'});
const clear=(node)=>{while(node.firstChild)node.removeChild(node.firstChild)};
const text=(tag,value,className)=>{const n=document.createElement(tag);n.textContent=value;if(className)n.className=className;return n};
function renderApprovals(items){
  const root=byId('approvals');clear(root);
  if(!items.length){root.appendChild(text('span','No approvals for this tenant.','ok'));return;}
  for(const item of items){
    const box=document.createElement('div');box.className='approval';
    const title=text('div',item.id.slice(0,8)+'  '+item.tool+'  '+item.status);
    box.appendChild(title);
    if(item.status==='PENDING'){
      const b=text('button','Approve');b.type='button';b.dataset.approve=item.id;b.addEventListener('click',()=>approve(item.id));box.appendChild(b);
    }
    const pre=text('pre',JSON.stringify(item,null,2));box.appendChild(pre);root.appendChild(box);
  }
}
function renderAudit(items){const root=byId('audit');clear(root);root.appendChild(text('pre',JSON.stringify(items,null,2)))};
async function loadAll(){
  try{
    const [a,l]=await Promise.all([fetch('/admin/api/approvals',{headers:headers()}),fetch('/admin/api/audit',{headers:headers()})]);
    if(!a.ok||!l.ok)throw new Error('HTTP '+a.status+'/'+l.status);
    const aj=await a.json(),lj=await l.json();renderApprovals(aj.items);renderAudit(lj.items);
    byId('status').textContent='Loaded';byId('status').className='ok';
  }catch(e){byId('status').textContent=e.message;byId('status').className='bad'}
}
async function approve(id){
  const reason=prompt('Approval reason','Verified request, identity and tenant context.');if(!reason)return;
  const r=await fetch('/admin/api/approvals/'+encodeURIComponent(id)+'/approve',{method:'POST',headers:headers(),body:JSON.stringify({reason})});
  const j=await r.json();if(!r.ok){alert(JSON.stringify(j));return;}await loadAll();
}
byId('load').addEventListener('click',loadAll);
""";
    }
}
