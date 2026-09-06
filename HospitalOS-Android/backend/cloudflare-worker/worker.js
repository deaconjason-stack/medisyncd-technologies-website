const APP_ORIGIN = "https://appassets.androidplatform.net";
function headers(origin, env) {
  const allowed=(env.ALLOWED_ORIGIN||APP_ORIGIN).trim();
  return {
    "Access-Control-Allow-Origin":origin&&origin===allowed?origin:allowed,
    "Access-Control-Allow-Methods":"POST, OPTIONS",
    "Access-Control-Allow-Headers":"Content-Type, X-MediSyncD-App-Token",
    "Access-Control-Max-Age":"86400",
    "Cache-Control":"no-store",
    "Content-Type":"application/json; charset=utf-8",
    "Vary":"Origin"
  };
}
function reply(data,status,h){return new Response(JSON.stringify(data),{status:status,headers:h});}
function msgs(body){return (Array.isArray(body.messages)?body.messages:[]).filter(function(m){return m&&(m.role==="user"||m.role==="assistant")&&typeof m.content==="string";}).slice(-20);}
function openAIText(data){
  if(typeof data.output_text==="string"&&data.output_text.trim())return data.output_text;
  const p=[]; for(const item of (data.output||[])){if(!item||item.type!=="message")continue;for(const c of (item.content||[])){if(c&&(c.type==="output_text"||c.type==="text")&&typeof c.text==="string")p.push(c.text);}}
  return p.join("\n").trim();
}
async function anthropic(body,env){
  const payload={model:env.ANTHROPIC_MODEL||"claude-sonnet-5",max_tokens:Math.min(Math.max(Number(body.max_tokens)||1200,64),4000),system:typeof body.system==="string"?body.system.slice(0,30000):"",messages:msgs(body),stream:false};
  const r=await fetch("https://api.anthropic.com/v1/messages",{method:"POST",headers:{"Content-Type":"application/json","x-api-key":env.ANTHROPIC_API_KEY,"anthropic-version":"2023-06-01"},body:JSON.stringify(payload)});
  const d=await r.json().catch(function(){return {};}); if(!r.ok)throw new Error((d.error&&d.error.message)||("Anthropic request failed ("+r.status+")")); return d;
}
async function openai(body,env){
  const input=[]; if(typeof body.system==="string"&&body.system.trim())input.push({role:"system",content:[{type:"input_text",text:body.system.slice(0,30000)}]});
  for(const m of msgs(body))input.push({role:m.role,content:[{type:"input_text",text:m.content}]});
  const payload={model:env.OPENAI_MODEL||"gpt-5.6",input:input,max_output_tokens:Math.min(Math.max(Number(body.max_tokens)||1200,64),4000)};
  const r=await fetch("https://api.openai.com/v1/responses",{method:"POST",headers:{"Content-Type":"application/json","Authorization":"Bearer "+env.OPENAI_API_KEY},body:JSON.stringify(payload)});
  const d=await r.json().catch(function(){return {};}); if(!r.ok)throw new Error((d.error&&d.error.message)||("OpenAI request failed ("+r.status+")"));
  const t=openAIText(d); if(!t)throw new Error("OpenAI returned no text output."); return {id:d.id,type:"message",role:"assistant",content:[{type:"text",text:t}]};
}
export default{async fetch(request,env){
  const origin=request.headers.get("Origin")||"",h=headers(origin,env),allowed=(env.ALLOWED_ORIGIN||APP_ORIGIN).trim();
  if(request.method==="OPTIONS")return new Response(null,{status:204,headers:h});
  if(origin&&origin!==allowed)return reply({error:{message:"Origin not allowed."}},403,h);
  if(env.APP_TOKEN&&request.headers.get("X-MediSyncD-App-Token")!==env.APP_TOKEN)return reply({error:{message:"Gateway authorization failed."}},401,h);
  const url=new URL(request.url); if(url.pathname!=="/v1/messages")return reply({error:{message:"Not found."}},404,h); if(request.method!=="POST")return reply({error:{message:"Method not allowed."}},405,h);
  if(Number(request.headers.get("Content-Length")||0)>200000)return reply({error:{message:"Request too large."}},413,h);
  let body; try{body=await request.json();}catch(_e){return reply({error:{message:"Invalid JSON body."}},400,h);}
  try{
    const p=String(env.AI_PROVIDER||"").toLowerCase(); let d;
    if(p==="openai"||(!env.ANTHROPIC_API_KEY&&env.OPENAI_API_KEY)){if(!env.OPENAI_API_KEY)throw new Error("OPENAI_API_KEY is not configured on the gateway.");d=await openai(body,env);}
    else{if(!env.ANTHROPIC_API_KEY)throw new Error("ANTHROPIC_API_KEY is not configured on the gateway.");d=await anthropic(body,env);}
    return reply(d,200,h);
  }catch(e){return reply({error:{message:e&&e.message?e.message:"AI gateway request failed."}},502,h);}
}};
