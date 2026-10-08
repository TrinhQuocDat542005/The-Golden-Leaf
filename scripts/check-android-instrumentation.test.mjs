import test from 'node:test';
import assert from 'node:assert/strict';
import {verifiedTests} from './check-android-instrumentation.mjs';
const fixture=Array.from({length:4},(_,i)=>`INSTRUMENTATION_STATUS: class=Fixture\nINSTRUMENTATION_STATUS: test=test${i}\nINSTRUMENTATION_STATUS_CODE: 0\n`).join('')+'OK (4 tests)\nINSTRUMENTATION_CODE: -1\n';
test('native runner accepts complete unique success events only',()=>{
  assert.equal(verifiedTests(fixture).length,4);
  assert.equal(verifiedTests(fixture.replaceAll('\n','\r\n')).length,4);
  assert.throws(()=>verifiedTests(fixture.replace('test3','test0')));
  assert.throws(()=>verifiedTests(fixture.replace('OK (4 tests)','OK (3 tests)')));
});
test('native runner rejects crash, failure, partial output and skipped tests',()=>{
  for(const output of ['',fixture.replace('INSTRUMENTATION_CODE: -1','INSTRUMENTATION_CODE: 0'),
    fixture+'FAILURES!!!',fixture+'INSTRUMENTATION_FAILED',fixture+'Process crashed',
    fixture.replace('INSTRUMENTATION_STATUS_CODE: 0','INSTRUMENTATION_STATUS_CODE: -3')])
    assert.throws(()=>verifiedTests(output));
});
