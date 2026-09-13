package com.space42.clients;
import com.space42.models.LoginRequest;
import io.restassured.response.Response;
import java.util.Map;
public class AuthClient {
    private final ApiClient api;
    public AuthClient(ApiClient api) { this.api=api; }
    public Response login(String username, String password, int minutes) {
        return api.call("POST", "/auth/login", null, new LoginRequest(username,password,minutes));
    }
    public Response me(String token) { return api.call("GET", "/auth/me", token, null); }
    public Response refresh(String token, int minutes) {
        return api.call("POST", "/auth/refresh", null, Map.of("refreshToken",token,"expiresInMins",minutes));
    }
}
