# Learn and explain the framework

## 1. Start with the login journey

Open `AuthenticationTests.java` and find `currentUserMatchesToken`:

```text
Test -> TokenManager.get("userA") -> discover demo account -> AuthClient.login
     -> store tokens -> AuthClient.me -> verify authenticated identity/role
Test -> AuthClient.me(accessToken) -> check status and expected user ID
```

The first token request logs in; subsequent requests in that class reuse the session. The test does not know the password. An HTTP 200 alone would be insufficient because the server could return another person's identity.

Try running `mvn -Dtest=AuthenticationTests test`. Inspect both the passing tests and the invalid-refresh status mismatch. Explain why a token being rejected with 403 is different from an invalid token granting access.

## 2. Trace one request

`AuthClient.me` asks `ApiClient` to send a GET to `/auth/me`. ApiClient creates a fresh request, sets JSON headers and timeouts, and adds a Bearer header only when the token is not null. RestAssured returns a Response. The test then checks it.

A client answers “how do I call this API?” A test answers “is its behavior correct?” Keeping those separate makes an endpoint change easier to maintain.

## 3. Read the models

A DTO is simply an object for moving data. `LoginRequest` becomes the JSON login body. `Session` keeps the identity and tokens together. `SecurityScenario` represents one row of test data. Plain fields keep these small classes readable; no Lombok, builders or inheritance hierarchy is needed.

Never add a generated `toString()` that prints password/token fields. A record with its default string representation would also expose those fields.

## 4. Trace one matrix row

Find `OWN-02-cross-user-read` in `security-matrix.json`:

1. Actor `userA` logs in; target `userB` is a distinct authenticated user.
2. The target ID replaces `{targetId}` in the URL.
3. The request sends **userA's token**, asking for **userB's todos**.
4. The proposed private-resource policy expects 403.
5. If the service returns 200, the test records expected versus actual and fails.

The DataProvider returns each row to the same `securityPolicy` method. It reduces duplicate test logic, not the number of executed cases.

Now compare `OWN-01-own-read`: the caller and owner are the same, and the expected 200 also requires every returned todo's `userId` to match. An empty list is treated as a missing fixture instead of a vacuous pass.

## 5. Explain RBAC honestly

Admin and ordinary-user contexts come from actual role fields, confirmed with `/auth/me`. We propose an admin-only user directory, then exercise the same endpoint with both roles. DummyJSON's public directory may ignore that restriction. We can demonstrate the difference against our policy, but cannot claim it violates a documented DummyJSON admin-only contract.

In a production project, agree the role-permission matrix with the product/security owners first.

## 6. Explain a failing report

Look at the test ID, actor, target, authentication state, oracle, expected status and actual status. Then ask:

- Was the request made? A transport failure is not security evidence.
- Was the fixture valid and the role confirmed?
- Did a denial expectation receive successful access? That supports a sandbox policy gap.
- Was the request denied with another error code? That is a different kind of mismatch.
- Did the status pass but the body violate ownership, type or field rules?

Never turn 403 into 200 in the matrix simply to get green results. If a legitimate policy changes, document why the oracle changed.

## 7. Explain the trade-offs

“I used a small client layer, in-memory authentication, JSON data and TestNG reporting. I chose a focused set of high-risk cases. I avoided parallel tests and automatic retries against a shared public sandbox. I validate simulated update responses without pretending they persist.”

No automatic refresh in TokenManager: the suite is short, and refreshing silently could mask token defects. Refresh behavior is tested explicitly.

## 8. Practice before the interview

Run the project yourself. Trace `currentUserMatchesToken` and `OWN-02` without notes. Add one matrix row for the second user's own todos, predict the result and run it. Explain how the safe logger avoids printing secrets. Finally, update your actual effort and AI-review disclosure truthfully.

Suggested one-minute explanation:

“My framework tests authentication and security boundaries using Java, RestAssured and TestNG. Maven manages the dependencies. API clients handle HTTP calls; tests own the assertions. A token manager discovers demo accounts and establishes separate verified user and administrator sessions. A JSON matrix drives positive and negative ownership and role scenarios. Reports preserve the difference between our proposed production policy and DummyJSON's public sandbox behavior. I keep sensitive values in memory and log only safe metadata.”
