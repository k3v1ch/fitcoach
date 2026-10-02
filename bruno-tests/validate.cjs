// Run after npm install: verifies Bruno syntax, scripts, variables and route coverage.
const fs=require('fs'),path=require('path'),vm=require('vm');
const lang=require('@usebruno/lang');
const root=__dirname;
const manifest=JSON.parse(fs.readFileSync(path.join(root,'manifest.json'),'utf8'));
const suites=['automatic-public','automatic-e2e','manual','manual-spec'];
const failures=[];
function walk(dir) { return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]); }
let scripts=0;
const parsed=new Map();
for(const suite of suites) {
 const env=lang.bruToEnvJsonV2(fs.readFileSync(path.join(root,suite,'environments/Local.bru'),'utf8'));
 if(!env) failures.push(suite+': environment parse failed');
 for(const f of walk(path.join(root,suite,'requests')).filter(f=>f.endsWith('.bru'))) {
  try {
   const r=lang.bruToJsonV2(fs.readFileSync(f,'utf8'));
   if(!r.http?.url || !r.http?.method) throw Error('Missing HTTP request');
   if(!r.tests?.includes('res.getStatus()')) throw Error('Missing HTTP status assertion');
   for(const [label,source] of Object.entries({...r.script,tests:r.tests})) if(typeof source==='string'&&source.trim()) {
    new vm.Script('(async function () {\n'+source+'\n})',{filename:f+':'+label}); scripts++;
   }
   if(r.http.body==='json') JSON.parse(r.body.json);
   if(r.http.body==='multipart-form') {
    for(const item of r.body.multipartForm||[]) if(item.type==='file') for(const fileValue of item.value) {
     if(fileValue.includes('{{')) continue;
     const p=path.resolve(root,suite,fileValue); if(!fs.existsSync(p)) throw Error('Missing multipart fixture '+p);
    }
   }
   parsed.set(path.relative(root,f).replaceAll('\\','/'),r);
  } catch(e) { failures.push(path.relative(root,f)+': '+e.message); }
 }
}
for(const item of manifest) if(!parsed.has(item.file)) failures.push('Manifest entry missing: '+item.file);
function normalize(p) { return p.split('?')[0].replace(/\{\{[^}]+\}\}|\{[^}]+\}/g,'{}'); }
const manualRoutes=new Set(manifest.filter(x=>x.collection==='manual').map(x=>x.method+' '+normalize(x.url)));
const sourceRoot=process.argv[2];
let coverage;
if(sourceRoot) {
 const routes=[];
 for(const file of walk(path.join(sourceRoot,'src/main/java')).filter(f=>f.endsWith('Controller.java'))) {
  const source=fs.readFileSync(file,'utf8');
  const base=source.match(/@RequestMapping\("([^"]+)"\)/)?.[1];
  if(!base) continue;
  for(const match of source.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^\n]*)\))?/g)) {
   const suffix=match[2]?.match(/(?:value\s*=\s*)?"([^"]+)"/)?.[1]||'';
   routes.push({method:match[1].toUpperCase(),url:base+suffix});
  }
 }
 const missing=routes.filter(x=>{
  const pattern=new RegExp('^'+normalize(x.url).split('{}').map(p=>p.replace(/[.*+?^$()|[\]\\]/g,'\\$&')).join('[^/]+')+'$');
  return !manifest.some(m=>m.collection==='manual'&&m.method===x.method&&pattern.test(normalize(m.url)));
 });
 coverage={implementedRoutes:routes.length,manualCovered:routes.length-missing.length,missing};
 for(const r of missing) failures.push('Uncovered controller route: '+r.method+' '+r.url);
}
const result={checkedAt:new Date().toISOString(),cliVersion:require('@usebruno/cli/package.json').version,requestFiles:parsed.size,compiledScripts:scripts,counts:Object.fromEntries(suites.map(s=>[s,manifest.filter(x=>x.collection===s).length])),coverage,failures,liveBackendTested:false};
fs.writeFileSync(path.join(root,'validation.json'),JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result,null,2));
if(failures.length) process.exit(1);
