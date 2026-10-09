import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import {setTimeout as delay} from 'node:timers/promises';
import assert from 'node:assert/strict';

// Own child processes and H2 only; never stop/reuse a user's server or database.
const jar=path.resolve(process.argv[2] ?? 'The-Golden-Leaf-server/target/datban-0.0.1-SNAPSHOT.jar');
const port=18083, origin=`http://127.0.0.1:${port}`;
fs.mkdirSync('build/week10',{recursive:true});
const directory=fs.mkdtempSync(path.resolve('build/week10/recovery-'));
let child, stopped, log;
const request=(url,options={})=>fetch(origin+url,{...options,signal:AbortSignal.timeout(5000)});
async function stop() {
  if (!child) return;
  const processToStop=child;
  processToStop.kill('SIGTERM');
  const timeout=setTimeout(()=>processToStop.kill('SIGKILL'),10000);
  try { await stopped; } finally { clearTimeout(timeout); fs.closeSync(log); child=null; }
}
async function start(number) {
  log=fs.openSync(path.join(directory,`process-${number}.log`),'wx');
  child=spawn('java',['-jar',jar,'--spring.profiles.active=demo',`--server.port=${port}`],{stdio:['ignore',log,log],windowsHide:true});
  stopped=once(child,'exit');
  // Mark asynchronous rejection handled until finally/stop awaits it.
  stopped.catch(()=>{});
  const deadline=Date.now()+90000;
  while (Date.now()<deadline) {
    if (child.exitCode!==null) throw new Error('Disposable demo exited before readiness');
    try {
      const response=await request('/api/demo/config');
      if (response.ok && (await response.json()).demo===true) return;
    } catch { /* bounded readiness retry, no response/token output */ }
    await delay(500);
  }
  throw new Error('Disposable demo readiness timeout');
}
async function session() {
  const response=await request('/api/demo/session',{method:'POST',headers:{'Content-Type':'application/json'},body:'{"persona":"CUSTOMER"}'});
  assert.equal(response.status,200);const value=await response.json();
  assert.equal(value.demo,true);assert.equal(value.profile.uid,'demo-customer');return value.token;
}
try {
  await new Promise((resolve,reject)=>{
    const probe=net.createConnection({host:'127.0.0.1',port});
    probe.once('connect',()=>{probe.destroy();reject(new Error('Recovery fixture port occupied; refusing to reuse it'));});
    probe.once('error',error=>error.code==='ECONNREFUSED'?resolve():reject(error));
  });
  await start(1);const oldToken=await session();
  assert.equal((await request('/api/notifications',{headers:{Authorization:`Bearer ${oldToken}`}})).status,200);
  await stop();await start(2);
  assert.equal((await request('/api/notifications',{headers:{Authorization:`Bearer ${oldToken}`}})).status,401);
  const freshToken=await session();assert.notEqual(freshToken,oldToken);
  assert.equal((await request('/api/notifications',{headers:{Authorization:`Bearer ${freshToken}`}})).status,200);
  fs.writeFileSync(path.join(directory,'result.json'),JSON.stringify({passed:true,fixturePort:port,
    realProcessRestarts:1,oldTokenStatus:401,newSessionStatus:200,androidRuntime:false},null,2)+'\n');
  console.log('Real disposable backend restart passed: old token 401, fresh session 200. No Android restart claim.');
} finally { await stop(); }
