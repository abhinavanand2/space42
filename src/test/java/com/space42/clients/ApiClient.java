package com.space42.clients;
import com.space42.utils.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.testng.Reporter;

public class ApiClient {
    private final ConfigManager config;
    public ApiClient(ConfigManager config) {
        this.config = config;
        // Discovery and simulated writes are authorized only for this public sandbox.
        if (!"https://dummyjson.com".equals(config.get("base.url")))
            throw new IllegalArgumentException("This demo is restricted to https://dummyjson.com");
    }
    public Response call(String method, String path, String token, Object body) {
        var request = RestAssured.given().baseUri(config.get("base.url"))
            .contentType("application/json").accept("application/json")
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", config.number("timeout.ms"))
                .setParam("http.socket.timeout", config.number("timeout.ms"))));
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.body(body);
        try {
            Response response = request.request(method, path);
            // Allowlist metadata only: never log bodies, headers, cookies or query values.
            Reporter.log(method + " " + path.split("\\?")[0] + " -> " + response.statusCode(), true);
            return response;
        } catch (RuntimeException e) {
            // Do not attach a cause that could contain request or response secrets.
            throw new IllegalStateException("TRANSPORT_ERROR: " + method + " " + path.split("\\?")[0]);
        }
    }
}
