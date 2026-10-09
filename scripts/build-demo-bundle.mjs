import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const bundleFiles=['golden-leaf-demo.jar','golden-leaf-demo.apk','README.md','REVISION','manifest.json',
  'SECURITY.md','security-exceptions.json','Start-Demo.ps1','Start-Demo.sh','Verify-Demo.ps1'];
const digest=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
function review(exceptions, now) {
  if (!Array.isArray(exceptions) || !exceptions.length) throw new Error('Missing security review metadata');
  for (const item of exceptions) {
    if (!item.id || !item.package || !item.version || !Number.isFinite(Date.parse(item.expires)) ||
        now.getTime() >= Date.parse(item.expires)) throw new Error('Invalid/expired security review');
  }
}
export function verifyBundle(directory, now=new Date()) {
  const lines=fs.readFileSync(path.join(directory,'SHA256SUMS'),'utf8').trim().split(/\r?\n/);
  const seen=new Set();
  for (const line of lines) {
    const match=/^([a-f0-9]{64})  ([A-Za-z0-9.-]+)$/.exec(line);
    if (!match || !bundleFiles.includes(match[2]) || seen.has(match[2])) throw new Error('Invalid checksum manifest');
    const file=path.join(directory,match[2]);
    if (!fs.lstatSync(file).isFile() || digest(file)!==match[1]) throw new Error(`Checksum mismatch: ${match[2]}`);
    seen.add(match[2]);
  }
  if (seen.size!==bundleFiles.length) throw new Error('Incomplete checksum manifest');
  const revision=fs.readFileSync(path.join(directory,'REVISION'),'utf8').trim();
  const metadata=JSON.parse(fs.readFileSync(path.join(directory,'manifest.json'),'utf8'));
  if (!/^[a-f0-9]{40}$/.test(revision) || metadata.revision!==revision || metadata.localOnly!==true ||
      metadata.firebase!==false || metadata.android.applicationId!=='com.example.giaodien.demo' ||
      metadata.android.apiBaseUrl!=='http://10.0.2.2:8080/') throw new Error('Invalid demo identity/revision');
  review(JSON.parse(fs.readFileSync(path.join(directory,'security-exceptions.json'),'utf8')),now);
  return metadata;
}
export function buildBundle({jar,apk,revision,output,now=new Date()}) {
  if (!/^[a-f0-9]{40}$/.test(revision)) throw new Error('Full source revision is required');
  for (const file of [jar,apk]) {
    if (!fs.statSync(file).isFile() || !fs.readFileSync(file).subarray(0,4).equals(Buffer.from([0x50,0x4b,3,4]))) {
      throw new Error('Missing or invalid JAR/APK ZIP input');
    }
  }
  const exceptions=JSON.parse(fs.readFileSync(path.join(root,'scripts/security-exceptions.json'),'utf8'));
  review(exceptions,now);
  if (fs.existsSync(output) && fs.readdirSync(output).length) throw new Error('Output must be a new or empty directory');
  fs.mkdirSync(output,{recursive:true});
  const write=(name,data)=>fs.writeFileSync(path.join(output,name),data);
  fs.copyFileSync(jar,path.join(output,'golden-leaf-demo.jar'));
  fs.copyFileSync(apk,path.join(output,'golden-leaf-demo.apk'));
  fs.copyFileSync(path.join(root,'docs/demo-bundle-guide.md'),path.join(output,'README.md'));
  fs.copyFileSync(path.join(root,'SECURITY.md'),path.join(output,'SECURITY.md'));
  write('REVISION',revision+'\n'); write('security-exceptions.json',JSON.stringify(exceptions,null,2)+'\n');
  const deadline=new Date(Math.min(...exceptions.map(item=>Date.parse(item.expires))));
  for (const name of ['Start-Demo.ps1','Verify-Demo.ps1','Start-Demo.sh']) {
    // Windows PowerShell 5 needs BOM for the UTF-8 Vietnamese help text.
    write(name,(name.endsWith('.ps1') ? '\uFEFF' : '')+fs.readFileSync(path.join(root,'ops/demo',name),'utf8')
      .replaceAll('__SECURITY_REVIEW_DEADLINE__',deadline.toISOString().replace(/[-:TZ.]/g,'').slice(0,14)));
  }
  write('manifest.json',JSON.stringify({schema:1,revision,localOnly:true,firebase:false,
    javaVersion:17,backendProfile:'demo',securityReviewDeadline:deadline.toISOString(),
    android:{applicationId:'com.example.giaodien.demo',apiBaseUrl:'http://10.0.2.2:8080/',
      minSdk:25,debugSigned:true,storeRelease:false}},null,2)+'\n');
  write('SHA256SUMS',bundleFiles.map(name=>`${digest(path.join(output,name))}  ${name}\n`).join(''));
  return verifyBundle(output,now);
}
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  if (process.argv[2]==='--verify') {
    const metadata=verifyBundle(process.argv[3]);
    if (process.argv[4] && metadata.revision!==process.argv[4]) throw new Error('Bundle revision does not match verified source');
    console.log('Verified bundle revision: '+metadata.revision);
  } else {
    const args=process.argv.slice(2); const options={};
    for (let i=0;i<args.length;i+=2) {
      if (!['--jar','--apk','--revision','--output'].includes(args[i]) || !args[i+1]) throw new Error('Invalid bundle arguments');
      options[args[i].slice(2)]=args[i+1];
    }
    console.log('Prepared bundle revision: '+buildBundle(options).revision);
  }
}
