import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {buildBundle,verifyBundle,bundleFiles} from './build-demo-bundle.mjs';
const revision='a'.repeat(40), now=new Date('2026-10-09T00:00:00Z');
function fixture() {
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'golden-leaf-bundle-test-'));
  const jar=path.join(dir,'input.jar'), apk=path.join(dir,'input.apk'), output=path.join(dir,'bundle');
  // Packaging unit fixture only: these ZIP headers are not executable/runtime evidence.
  fs.writeFileSync(jar,Buffer.from([0x50,0x4b,3,4,1]));fs.writeFileSync(apk,Buffer.from([0x50,0x4b,3,4,2]));
  return {jar,apk,output,revision,now};
}
test('bundle includes both binaries, exact revision, launchers and all checksums',()=>{
  const options=fixture(); const result=buildBundle(options);
  assert.equal(result.revision,revision); assert.equal(result.android.debugSigned,true);
  assert.equal(fs.readdirSync(options.output).length,bundleFiles.length+1);
  assert.equal(verifyBundle(options.output,now).localOnly,true);
  assert.ok(!fs.readFileSync(path.join(options.output,'Start-Demo.sh'),'utf8').includes('__SECURITY_REVIEW_DEADLINE__'));
});
test('corrupted APK is rejected',()=>{
  const options=fixture();buildBundle(options);fs.appendFileSync(path.join(options.output,'golden-leaf-demo.apk'),'tamper');
  assert.throws(()=>verifyBundle(options.output,now),/Checksum mismatch/);
});
test('partial or duplicate manifest is rejected',()=>{
  const options=fixture();buildBundle(options);const file=path.join(options.output,'SHA256SUMS');
  const lines=fs.readFileSync(file,'utf8').trim().split('\n');
  fs.writeFileSync(file,lines.slice(1).join('\n'));assert.throws(()=>verifyBundle(options.output,now),/Incomplete/);
  fs.writeFileSync(file,[...lines,lines[0]].join('\n'));assert.throws(()=>verifyBundle(options.output,now),/Invalid checksum/);
});
test('path traversal in checksum filename is rejected',()=>{
  const options=fixture();buildBundle(options);fs.writeFileSync(path.join(options.output,'SHA256SUMS'),'a'.repeat(64)+'  ../input.jar\n');
  assert.throws(()=>verifyBundle(options.output,now),/Invalid checksum/);
});
test('expired security acceptance blocks packaging and verification',()=>{
  const options=fixture();buildBundle(options);const expired=new Date('2026-11-08T00:00:00Z');
  assert.throws(()=>verifyBundle(options.output,expired),/expired/);
  assert.throws(()=>buildBundle({...options,output:options.output+'-expired',now:expired}),/expired/);
});
test('invalid revision/input and occupied output fail without overwriting',()=>{
  const options=fixture();assert.throws(()=>buildBundle({...options,revision:'main'}),/revision/);
  buildBundle(options);assert.throws(()=>buildBundle(options),/empty directory/);
  fs.writeFileSync(options.apk,'not an APK');assert.throws(()=>buildBundle({...options,output:options.output+'-bad'}),/invalid JAR\/APK/);
});
