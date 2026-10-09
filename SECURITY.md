# Security policy

This repository is a portfolio/demo project, not a hosted restaurant service. There is no production SLA, bug bounty or promise of completed independent security auditing.

## Reporting

Do not open a public issue with credentials, an active token, private service-account JSON, personal data or banking documents. If the repository's **Security → Report a vulnerability** option is available, use that private channel. Otherwise ask the repository owner for a private contact channel before sharing exploit details. No personal email address is invented here.

Include affected revision, safe reproduction steps using synthetic data, expected/actual behavior and impact. Avoid testing outside systems you control. If a secret was exposed, revoke/rotate it with its issuer; removing the file from Git alone is not sufficient.

## Demo boundary

- Only the exclusive `demo` profile exposes `/api/demo/session`; sessions use ephemeral tokens and fixed synthetic identities, with normal DB role/ownership checks.
- Demo is fixed in-memory H2, bound to `127.0.0.1`, with Firebase and push disabled. Do not publish it via a tunnel/reverse proxy or target real data.
- Production/dev keep the Firebase verifier. Production settings/templates are engineering references and still require real infrastructure, secrets, scanning and operator UAT.
- `.env`, private keys, keystores, uploads/backups and test reports do not belong in bug reports. Public Firebase client config is not an Admin SDK service-account key.

Dependency update configuration is provided; updates are reviewed, not auto-merged. CI passing does not mean a dependency/image was independently audited.

## Portfolio prerelease review window (weeks 9–10)

Two unpatched findings, CVE-2026-47884 and CVE-2026-47890, are accepted only for local portfolio with exact `org.springframework:spring-webmvc:6.2.19` scope until **2026-11-08 00:00 UTC**. Source guards and expiry checks supplement the reachability review; they do not patch the library. Do not deploy publicly, extend the exception silently or treat persistent GitHub Release assets as indefinitely safe. Combined-bundle launchers check expiry and refuse startup afterwards; direct Java/APK execution does not enforce it. See [week 10 acceptance and remaining limits](docs/week-10-portfolio-release.md).

## Dependency and image gate (week 8)

CI scans the packaged Java dependency inventory and the built image OS/Java packages with a digest-pinned Trivy image. It fails on incomplete inventories or fixable HIGH/CRITICAL findings, except exact CVE/package/version entries in `scripts/security-exceptions.json`. Lower-severity and unfixed findings remain in reports, not silently dropped. Exceptions require rationale, a primary advisory and an expiry; they are not approval for public deployment. See [the current evidence and residual risk](docs/week-8-quality.md#security-gate-và-ngoại-lệ-có-hạn).
