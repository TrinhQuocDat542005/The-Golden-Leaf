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
