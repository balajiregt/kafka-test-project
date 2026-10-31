# Security remediation: 31 October 2026

Request-only keys, exact configured HTTPS provider endpoint validation, loopback hosting and patched dependencies. The optional vulnerable Kafka UI was removed with owner approval.

Validation: mvn -Dtest=EventAssuranceAdvisorTests,AssuranceAssistantServiceTests test: 9 passed; Maven dependency audit; docker compose config --services: kafka only.

Known advisory check: 183 exact/resolved dependency coordinates; 0 npm lock audit(s); 0 npm findings; 0 unaddressed OSV coordinates. Verified local backport coordinates retained explicitly: 0. This is a dated advisory result, not a security certification.

No hosted provider call or deployment. Container base-image advisories without vendor fixes remain tracked separately.

The commit hook is enabled per checkout only. Pull-request and scheduled CI repeat the checks. Required GitHub branch checks must be configured after their job names register; adding this workflow alone does not enforce branch protection. Review security updates before merging; no automatic merge or deployment is enabled by this change.
