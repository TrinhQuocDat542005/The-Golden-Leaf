import test from 'node:test';
import assert from 'node:assert/strict';
import {lintCounts,checkBudget,checkLogs} from './check-android-quality.mjs';
test('lint gate rejects errors, new types, increased counts and broken reports',()=>{
  const xml='<issues><issue id="Known" severity="Warning"><location/></issue></issues>';
  assert.deepEqual(lintCounts(xml),{Known:1});
  assert.doesNotThrow(()=>checkBudget({Known:1},{Known:1}));
  assert.throws(()=>checkBudget({Known:2},{Known:1}));
  assert.throws(()=>checkBudget({New:1},{Known:1}));
  assert.throws(()=>lintCounts(xml.replace('Warning','Error')));
  assert.throws(()=>lintCounts('<issues>'));
});
test('privacy gate permits static logs and rejects interpolated data/raw exceptions',()=>{
  checkLogs('Log.e("Auth", "Authentication failed")','fixture');
  checkLogs('// Log.d("Auth", "$token")','fixture');
  for(const code of ['Log.d("Auth", "$email")','Log.e("Auth", "Error", error)',
    'Log.e("Auth", response.body)','error.printStackTrace()','println(token)'])
    assert.throws(()=>checkLogs(code,'fixture'));
});
