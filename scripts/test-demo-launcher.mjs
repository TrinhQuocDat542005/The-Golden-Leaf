import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import {setTimeout as delay} from 'node:timers/promises';
import assert from 'node:assert/strict';
import {verifyBundle} from './build-demo-bundle.mjs';

const directory=path.resolve(process.argv[2] ?? 'demo-bundle');
verifyBundle(directory);
const windows=process.platform==='win32', port=windows?18082:8080;
fs.mkdirSync('build/week10',{recursive:true});
const output=fs.mkdtempSync(path.resolve('build/week10/launcher-'));
const log=fs.openSync(path.join(output,'startup.log'),'wx');
await new Promise((resolve,reject)=>{
  const probe=net.createConnection({host:'127.0.0.1',port});
  probe.once('connect',()=>{probe.destroy();reject(new Error('Launcher test port occupied; refusing to reuse it'));});
  probe.once('error',error=>error.code==='ECONNREFUSED'?resolve():reject(error));
});
const child=spawn(windows?'powershell.exe':'bash',windows?
  ['-NoProfile','-File',path.join(directory,'Start-Demo.ps1'),'-Port',String(port)]:
  [path.join(directory,'Start-Demo.sh')],{cwd:directory,stdio:['ignore',log,log],windowsHide:true,
    // Own process group only on POSIX; Windows uses taskkill on this exact owned PID/tree.
    detached:!windows});
const ended=once(child,'exit');ended.catch(()=>{});
try {
  const deadline=Date.now()+90000;let verified=false;
  while(Date.now()<deadline) {
    if(child.exitCode!==null) throw new Error('Launcher exited before readiness; inspect startup log');
    try {
      const response=await fetch(`http://127.0.0.1:${port}/api/demo/config`,{signal:AbortSignal.timeout(4000)});
      if(response.ok && (await response.json()).demo===true) { verified=true;break; }
    } catch { /* bounded startup polling */ }
    await delay(500);
  }
  assert.equal(verified,true,'Launcher readiness timeout');
  const menu=await fetch(`http://127.0.0.1:${port}/api/thucdon`);
  assert.equal(menu.status,200);assert.equal((await menu.json()).length,12);
  const page=await fetch(`http://127.0.0.1:${port}/demo.html`);
  assert.equal(page.status,200);
  fs.writeFileSync(path.join(output,'result.json'),JSON.stringify({passed:true,platform:process.platform,
    launcher:windows?'Start-Demo.ps1':'Start-Demo.sh',menuItems:12,webStatus:200,port},null,2)+'\n');
  console.log(`${windows?'PowerShell':'Bash'} bundle launcher smoke passed: demo config, 12 menu items, web 200.`);
} finally {
  if(windows) {
    const killer=spawn('taskkill.exe',['/PID',String(child.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});
    await once(killer,'exit');
  } else { try { process.kill(-child.pid,'SIGTERM'); } catch(error) { if(error.code!=='ESRCH') throw error; } }
  const timeout=setTimeout(()=>{if(!windows)try{process.kill(-child.pid,'SIGKILL');}catch{}},10000);
  try { await ended; } finally {clearTimeout(timeout);fs.closeSync(log);}
}
