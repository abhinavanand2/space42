# Space42 QA Automation Assessment

A deliberately small Java + RestAssured + TestNG + Maven project against DummyJSON. Built from the Space42 assessment dated 9 September 2026. It prioritizes identity, cross-user access, role boundaries and safe evidence over test count.

## Run locally

Install JDK 17 or newer and Maven 3.9 or newer. Verify `java -version` and `mvn -version`, then open a terminal in this project's directory:

```sh
mvn test
```

Run the initial authentication stage on its own:

```sh
mvn -Dtest=AuthenticationTests test
```

Run the matrix on its own:

```sh
mvn -Dtest=SecurityMatrixTests test
```

Internet access to Maven Central and DummyJSON is required. The tested environment used JDK 21 and Maven 3.9.16, compiling for Java 17. Dependencies are pinned in `pom.xml`; they are selected versions, not a claim of being the newest available.

**A failing exit code is expected when a security expectation is not met.** No failure is silently skipped, retried or converted to a pass. Read `evidence/RUN-SUMMARY.md` for the supplied run. Generated reports appear under `target/surefire-reports/index.html`, `emailable-report.html` and the XML reports. Open the HTML report in a browser and inspect failed methods and Reporter output. Fresh runs replace target reports; `evidence/` is the saved submission snapshot.

## Understand the project, step by step

Read [WALKTHROUGH.md](WALKTHROUGH.md) alongside the code. The build order was:

1. **Skeleton and pom.xml:** Maven uses the standard `src/test/java` and `src/test/resources` folders. RestAssured sends HTTP requests; TestNG runs tests and assertions; Jackson translates Java objects and JSON. No application code is required in `src/main` for this test-only project.
2. **ConfigManager:** reads non-secret defaults, with system-property overrides first, then environment variables, then the properties file. Example: `mvn test -Dtimeout.ms=20000`; equivalent environment name is `TIMEOUT_MS`. `BASE_URL` maps to `base.url`, but the client deliberately rejects hosts other than `https://dummyjson.com`, because runtime demo-credential discovery and simulated writes are sandbox-specific.
3. **BaseTest:** creates the config, API client, authentication client and token manager before each test class. It clears cached session references afterwards. There are no global RestAssured defaults and no shared cookies.
4. **ApiClient / AuthClient:** ApiClient owns HTTP setup and safe metadata logging. AuthClient gives the tests readable login, me and refresh methods. Tests retain the assertions.
5. **LoginRequest / Session:** small objects hold the login body and authenticated context. Their `toString()` methods hide sensitive content. Fields remain in memory; no credentials or tokens are saved.
6. **TokenManager:** discovers two distinct `user` accounts and one `admin` as needed from the public demo directory. It logs in programmatically, checks returned identity and checks the role again through `/auth/me`. Each class caches sessions by alias for a short sequential run. Fixture discovery requests only the required fields and at most ten candidate users per role. Ordinary users are selected from those owning todos, using one ID-only todo collection. No hard-coded account IDs or real credentials.
7. **AuthenticationTests:** seven independent tests cover login, identity, missing/malformed tokens, valid refresh and invalid refresh. Refresh checks token usability and identity, not token-string inequality: same-second issuance can yield the same token.
8. **SecurityScenario + matrix:** JSON rows hold actor, target, HTTP method, endpoint, auth state, oracle, expected status, response check and optional body. TestNG's DataProvider runs one common test for all ten rows. Adding another row using an existing check needs no duplicate test method. A fundamentally new response shape may need a new check.
9. **Checks and reports:** reusable status/content-type/error checks and scenario-specific identity, ownership, type and exposure assertions. TestNG generates the HTML/XML evidence.

There are eleven Java files. A generic API client is enough for the small resource surface; separate one-method UserClient/TodoClient wrappers would add little value here.

## Strategy and test oracles

Authentication establishes **who the caller is**. Authorization decides **what that caller may do**. Ownership adds **whose resource it is**. The matrix pairs allowed and forbidden operations, prioritizing horizontal privilege escalation (another user's data), vertical privilege escalation (user versus admin), and field exposure.

DummyJSON documents public user and todo resources and simulated updates. It does not specify the production policy below. These are explicitly **assessment hypotheses**, not claims that DummyJSON violates its published product contract:

| Assumption | Expected production behavior |
|---|---|
| User todos are private | Authenticated owner can read/update; another ordinary user receives 403 |
| Todo reads are protected | Missing/invalid authentication receives 401 |
| Full user directory is administrative | Admin receives 200; ordinary user receives 403 |
| Individual profile uses minimum necessary fields | Password, SSN, bank and crypto fields are absent |

The directory check requests only IDs and roles to avoid retaining unnecessary data. The exposure check deliberately requests one profile and reports prohibited **field names only**. It does not prove all PII is safe; the four-field denylist is a focused example.

Authentication expectations: successful login/me/refresh = 200; invalid credentials = 400; missing/malformed bearer token = 401; invalid refresh token = 401 (explicit consistency expectation, not an exact documented status guarantee). The invalid refresh 403 result is rejection with a status-semantic mismatch, **not a bypass**. Missing todo = 404 is a contract/boundary check.

The JSON `oracle` distinguishes `CONTRACT` from `PRODUCTION_POLICY`. A 2xx response where our policy expects denial is labelled `SECURITY_HYPOTHESIS_GAP`. Other mismatches remain contract/policy mismatches. A 429 or 5xx is service/rate-limit evidence, not an RBAC finding. Transport exceptions are labelled `TRANSPORT_ERROR`; fixture failures identify missing accounts/resources. Successful status alone is only `STATUS_MATCH`: payload assertions must still pass.

## State, safety and diagnostics

- Sequential execution, no load tests, credential guessing, high-volume fuzzing or automatic retries.
- Only two small simulated PUT operations; no delete or create. Select todo IDs from the target user's data and verify ownership before updating. Assert the returned update without assuming persistence.
- Every request has fresh request configuration. No session/cookie filter can accidentally authenticate a missing-token case.
- Cache scope is one test class, not static/global. Default tokens last ten minutes. Long-running/parallel execution and automatic renewal are intentionally not supported.
- Only HTTP method, path without query values, status, scenario aliases/IDs and controlled diagnostics enter reports. No `.log().all()`, body dumps, cookies or authorization headers.
- Token/credential objects are never DataProvider parameters. Matrix objects render as their safe scenario ID. Assertions on sensitive values use booleans, not equality messages that print the values.
- HTTP-client exception causes are omitted because they may contain request details. This sacrifices some diagnostics for safe reports. Numeric statuses, fixed paths and fixture context remain useful.
- Do not enable HTTP wire logging or commit raw response captures. The supplied resume and assessment attachments are not bundled in this repository.

## Limits and incomplete areas

No full JSON Schema library; focused required-field/type/identity checks cover the selected responses. No token expiry/revocation, tampered signed JWT, refresh rotation/reuse, logout, moderator policy, concurrency, state-persistence validation, CI pipeline or broad PII scan. Those are sensible later extensions, not necessary to explain this baseline. No production policy is inferred from a role field alone: the policy is stated above and each authenticated role is verified.

Public fixtures can change. Missing role accounts or todos are fixture failures requiring investigation; they are not automatically reported as security defects. Network outages can make every live test fail. This assessment suite has no offline mock mode.

## Actual effort and AI assistance

The initial implementation and live validation were performed with OpenAI Codex in this session on 13 September 2026. See the run summary for measured execution duration. **Actual candidate hands-on effort: approximately 3 hours.** This is separate from automated execution time and the three-day submission window.

Codex assisted with assessment/resume review, architecture, generated Java code, matrix design, documentation, execution and result analysis. Tool-run compilation and live validation are recorded in the evidence. The candidate reviewed all project code and folders and independently ran the test suite locally, reproducing the documented result of 17 tests: 10 passed and 7 failed. The candidate remains responsible for the submitted implementation.

## References

- [DummyJSON authentication](https://dummyjson.com/docs/auth)
- [DummyJSON users](https://dummyjson.com/docs/users)
- [DummyJSON todos and simulated updates](https://dummyjson.com/docs/todos)
- [OWASP API Security Top 10 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)

The assessment requires submission within three calendar days of receipt. Confirm your actual receipt date rather than treating the document issue date as the deadline.
