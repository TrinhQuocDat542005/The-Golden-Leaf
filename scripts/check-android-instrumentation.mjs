import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export function verifiedTests(output, expected=4) {
  const text=output.replaceAll('\r','');
  const summary=text.match(/^OK \((\d+) tests?\)$/m);
  if (!summary || Number(summary[1])!==expected || !/^INSTRUMENTATION_CODE: -1$/m.test(text) ||
      /FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed/.test(text))
    throw new Error('Instrumentation failed, crashed, skipped or returned an incomplete summary');
  const tests=[]; let className='', name='';
  for (const line of text.split('\n')) {
    if (line.startsWith('INSTRUMENTATION_STATUS: class=')) className=line.substring(line.indexOf('=')+1);
    if (line.startsWith('INSTRUMENTATION_STATUS: test=')) name=line.substring(line.indexOf('=')+1);
    if (line==='INSTRUMENTATION_STATUS_CODE: 0') {
      if (!className || !name) throw new Error('Missing test identity');
      tests.push({className,name}); className=''; name='';
    }
  }
  if (tests.length!==expected || new Set(tests.map(t=>`${t.className}:${t.name}`)).size!==expected)
    throw new Error('Missing or duplicated successful test events');
  return tests;
}
const escape=value=>value.replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[char]));
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  const tests=verifiedTests(fs.readFileSync(process.argv[2] ?? 'build/week8/instrumentation-results.txt','utf8'));
  const report=process.argv[3] ?? 'build/week8/instrumentation-results.xml';
  fs.mkdirSync(path.dirname(report),{recursive:true});
  fs.writeFileSync(report,`<?xml version="1.0" encoding="UTF-8"?><testsuite name="Week8NativeAndroid" tests="${tests.length}" failures="0" errors="0" skipped="0">${tests.map(t=>`<testcase classname="${escape(t.className)}" name="${escape(t.name)}"/>`).join('')}</testsuite>\n`);
  console.log(`Verified ${tests.length} native Android tests; JUnit report: ${report}`);
}
