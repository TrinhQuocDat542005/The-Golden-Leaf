// Read-only bounded staging probe. Not a capacity certification or financial workflow test.
import {performance} from 'node:perf_hooks';
const base = new URL(process.env.BASE_URL || 'http://127.0.0.1:8080/');
if (base.username || base.password || base.search || base.hash ||
    (base.protocol !== 'https:' && !['127.0.0.1', 'localhost'].includes(base.hostname))) throw Error('Use HTTPS or explicit localhost only');
const count = Number(process.env.REQUESTS || 100), concurrency = Number(process.env.CONCURRENCY || 5);
if (!Number.isInteger(count) || count < 1 || count > 1000 || !Number.isInteger(concurrency) || concurrency < 1 || concurrency > 20) throw Error('Probe bounds: 1–1000 requests, 1–20 concurrency');
let cursor = 0, failures = 0, throttled = 0;
const timings = [];
await Promise.all(Array.from({length: concurrency}, async () => {
  while (cursor++ < count) {
    const start = performance.now();
    try {
      const response = await fetch(new URL('api/thucdon', base), {signal: AbortSignal.timeout(15000)});
      await response.arrayBuffer();
      if (response.status === 429) throttled++; else if (response.status !== 200) failures++;
    } catch { failures++; }
    timings.push(performance.now() - start);
  }
}));
timings.sort((a,b) => a-b);
console.log(JSON.stringify({requests: timings.length, concurrency, failures, throttled,
  p50Ms: Math.round(timings[Math.floor(timings.length*.5)]), p95Ms: Math.round(timings[Math.min(timings.length-1,Math.floor(timings.length*.95))])}));
if (failures || throttled) process.exitCode = 1;
