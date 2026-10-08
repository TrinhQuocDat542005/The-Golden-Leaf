import test from 'node:test';
import assert from 'node:assert/strict';
import {checkReport} from './check-security-report.mjs';
test('security gate rejects empty/incomplete inventory and fixable high/critical',()=>{
  assert.throws(()=>checkReport({}));
  const clean={Results:[{Type:'jar',Packages:[{Name:'fixture'}]}]};
  assert.doesNotThrow(()=>checkReport(clean));
  assert.throws(()=>checkReport(clean,true));
  const image={Results:[...clean.Results,{Class:'os-pkgs',Packages:[{Name:'os-fixture'}]}]};
  assert.doesNotThrow(()=>checkReport(image,true));
  for(const Severity of ['HIGH','CRITICAL']) assert.throws(()=>checkReport({Results:[{...clean.Results[0],Vulnerabilities:[{Severity,FixedVersion:'2',PkgName:'fixture',VulnerabilityID:'CVE-fixture'}]}]}));
});
test('security exception is scoped by package/version and expires',()=>{
  const finding={Severity:'CRITICAL',FixedVersion:'7',PkgName:'spring',VulnerabilityID:'CVE-test',InstalledVersion:'6'};
  const report={Results:[{Type:'jar',Packages:[{}],Vulnerabilities:[finding]}]};
  const allow={id:'CVE-test',package:'spring',version:'6',expires:'2026-11-08T00:00:00Z',reason:'Not reachable',advisory:'https://primary.example/'};
  const now=new Date('2026-10-08T00:00:00Z');
  assert.doesNotThrow(()=>checkReport(report,false,[allow],now));
  assert.throws(()=>checkReport(report,false,[{...allow,version:'5'}],now));
  assert.throws(()=>checkReport(report,false,[allow],new Date('2026-11-09T00:00:00Z')));
  assert.throws(()=>checkReport(report,false,[{...allow,expires:'not-a-date'}],now));
});
