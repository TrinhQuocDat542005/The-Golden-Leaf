import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export function checkReport(report, requireOs=false, exceptions=[], now=new Date()) {
  const results=report.Results ?? [];
  if (!results.some(result => result.Type === 'jar' && result.Packages?.length > 0))
    throw new Error('Missing Java dependency inventory: an empty scan is not a clean scan');
  if (requireOs && !results.some(result => result.Class === 'os-pkgs' && result.Packages?.length > 0))
    throw new Error('Missing image OS package inventory');
  const findings=results.flatMap(result => result.Vulnerabilities ?? []);
  for (const item of exceptions) if (!item.id || !item.package || !item.version || !item.reason || !item.advisory || !Number.isFinite(Date.parse(item.expires)) || now >= new Date(item.expires)) throw new Error('Invalid/expired security exception');
  const blocking=findings.filter(item => ['HIGH','CRITICAL'].includes(item.Severity) && item.FixedVersion &&
    !exceptions.some(allow => allow.id===item.VulnerabilityID && allow.package===item.PkgName && allow.version===item.InstalledVersion));
  console.log(`Scan findings: ${findings.length}; fixable HIGH/CRITICAL: ${blocking.length}`);
  if (blocking.length) throw new Error(blocking.map(item => `${item.VulnerabilityID} ${item.PkgName} ${item.InstalledVersion} → ${item.FixedVersion}`).join('\n'));
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  if (!process.argv[2]) throw new Error('Security report path is required');
  const exceptions=JSON.parse(fs.readFileSync('scripts/security-exceptions.json','utf8'));
  for (const item of exceptions) {
    if (item.id==='CVE-2026-47884') {
      const visit=dir=>fs.readdirSync(dir,{withFileTypes:true}).some(entry=>{
        const file=path.join(dir,entry.name);
        return entry.isDirectory() ? visit(file) : file.endsWith('.java') && /XsltView/.test(fs.readFileSync(file,'utf8'));
      });
      if (visit('The-Golden-Leaf-server/src/main/java')) throw new Error('XsltView introduced: remove/reassess security exception');
    }
  }
  checkReport(JSON.parse(fs.readFileSync(process.argv[2],'utf8')),process.argv.includes('--image'),exceptions);
}
