# Live execution evidence

Executed 13 September 2026 at 16:23 Dubai time against https://dummyjson.com.

Command: `mvn clean test` (an isolated local Maven cache was used by Codex). JDK 21.0.11, Maven 3.9.16; Java release target 17. Maven elapsed time: 21.894 seconds.

**17 tests: 10 passed, 7 failed, 0 skipped.** Build exit status 1 reflects preserved assertion failures.

| Scenario | Result | Failure or outcome |
|---|---|---|
| currentUserMatchesToken | PASS | Assertions passed |
| invalidCredentialsAreRejected | PASS | Assertions passed |
| invalidRefreshTokenIsRejected | FAIL | HTTP status expected [401] but found [403] |
| malformedTokenIsRejected | PASS | Assertions passed |
| missingTokenIsRejected | PASS | Assertions passed |
| refreshedTokenRetainsIdentity | PASS | Assertions passed |
| validLoginEstablishesIdentity | PASS | Assertions passed |
| OWN-01-own-read | PASS | Assertions passed |
| OWN-02-cross-user-read | FAIL | HTTP status expected [403] but found [200] |
| OWN-03-own-update | PASS | Assertions passed |
| OWN-04-cross-user-update | FAIL | HTTP status expected [403] but found [200] |
| RBAC-01-admin-directory | PASS | Assertions passed |
| RBAC-02-user-directory | FAIL | HTTP status expected [403] but found [200] |
| SEC-01-anonymous-todos | FAIL | HTTP status expected [401] but found [200] |
| SEC-02-invalid-token-todos | FAIL | HTTP status expected [401] but found [200] |
| SEC-03-profile-field-exposure | FAIL | SECURITY_HYPOTHESIS_GAP: sensitive fields present; see field names in report expected [true] but found [false] |
| NEG-01-missing-todo | PASS | Assertions passed |

## Interpretation

Five access-denial policies received HTTP 200: cross-user todo read, cross-user todo update, ordinary-user directory access, anonymous todo read, and invalid-token todo read. These demonstrate gaps against the explicitly proposed policy, not violations of a documented DummyJSON private-resource contract.

The individual profile response contained password, ssn, bank and crypto field names. Field values were not logged. This is the sixth security-policy gap.

Invalid refresh token returned 403 instead of the expected 401. The token was rejected: this is a status-consistency mismatch, not a bypass.

Initial discovery selected a user with no todos, causing two fixture failures in the exploratory run. Discovery was corrected to select role-verified users who own todos. A clean full rerun produced the results above; no security expectation was weakened.

## Review and reproduction

Open `emailable-report.html` for generated failure details and safe Reporter output. `testng-results.xml` contains the machine-readable run. The report snapshot excludes raw HTTP traffic and Maven system-property dumps.

Source and saved report scans found no JWT-shaped values or serialized password/accessToken/refreshToken values. This is a bounded check plus review of the logging/assertion paths, not a universal secret-detection guarantee.

Approximate tool-assisted implementation/validation window: 16:14–16:24 Dubai time on 13 September 2026 (about 10 minutes elapsed; not candidate hands-on effort). Candidate-reported hands-on effort: approximately 3 hours.
