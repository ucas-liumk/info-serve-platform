# Security Policy

## Supported Versions

Security fixes are handled on the current `main` branch before release tags are cut. Historical releases are not maintained unless a release note explicitly says otherwise.

## Reporting a Vulnerability

Do not open a public issue for secrets, credential exposure, or exploitable vulnerabilities.

Report privately to the repository owner through GitHub private channels or by opening a minimal private coordination thread with:

- affected component or bounded context;
- impact and exploit conditions;
- reproduction steps or proof of concept;
- affected version, commit, or deployment environment;
- suggested mitigation if known.

## Handling Rules

- Rotate leaked credentials immediately before history cleanup.
- Keep fixes on a dedicated branch and merge through PR after validation.
- For dependency vulnerabilities, prefer Dependabot security update PRs when available.
- For schema, Nacos, deploy, or Dubbo API changes, follow `AGENTS.md` release and validation rules.
