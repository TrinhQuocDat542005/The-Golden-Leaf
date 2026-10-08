import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export function lintCounts(xml) {
  if (!/<issues\b/.test(xml) || !/<\/issues>/.test(xml)) throw new Error('Missing/truncated lint report');
  const counts = {};
  for (const match of xml.matchAll(/<issue\b([\s\S]*?)<\/issue>/g)) {
    const id = match[1].match(/\bid="([^"]+)"/)?.[1];
    const severity = match[1].match(/\bseverity="([^"]+)"/)?.[1];
    if (!id || !severity) throw new Error('Invalid lint issue');
    if (['Error', 'Fatal'].includes(severity)) throw new Error(`Lint error: ${id}`);
    counts[id] = (counts[id] ?? 0) + 1;
  }
  return counts;
}
export function checkBudget(counts, budget) {
  for (const [id, count] of Object.entries(counts))
    if (count > (budget[id] ?? 0)) throw new Error(`Lint regression ${id}: ${count} > ${budget[id] ?? 0}`);
}
export function checkLogs(source, name) {
  const code = source.replace(/\/\*[\s\S]*?\*\//g, '').split('\n').filter(line => !line.trim().startsWith('//')).join('\n');
  for (const call of code.matchAll(/\bLog\.[vdiew]\s*\(([\s\S]*?)\)\s*/g)) {
    // Only static tag/message log calls; no identifiers, interpolated email/token, raw errors or response bodies.
    if (!/^\s*"[^"$]*"\s*,\s*"[^"$]*"\s*$/.test(call[1])) throw new Error(`Unsafe dynamic Android log: ${name}`);
  }
  if (/\b(printStackTrace|println)\s*\(/.test(code)) throw new Error(`Raw Android diagnostic output: ${name}`);
}
function visit(dir) {
  for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
    const file = path.join(dir,entry.name);
    if (entry.isDirectory()) visit(file);
    else if (file.endsWith('.kt')) checkLogs(fs.readFileSync(file,'utf8'),file);
  }
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const counts = lintCounts(fs.readFileSync('app/build/reports/lint-results-debug.xml','utf8'));
  checkBudget(counts,JSON.parse(fs.readFileSync('scripts/android-lint-budget.json','utf8')));
  visit('app/src/main/java');
  console.log('Android lint budget + static log privacy gate passed',counts);
}
