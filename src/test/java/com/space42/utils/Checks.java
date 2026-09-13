package com.space42.utils;
import io.restassured.response.Response;
import org.testng.Assert;
public final class Checks {
    private Checks() {}
    public static void status(Response r, int expected) {
        Assert.assertEquals(r.statusCode(), expected, "HTTP status");
        Assert.assertTrue(r.contentType().contains("application/json"), "Expected JSON content type");
    }
    public static void text(Response r, String field) {
        Object value=r.jsonPath().get(field);
        Assert.assertTrue(value instanceof String && !((String)value).isBlank(), "Missing/non-text field: " + field);
    }
    public static void error(Response r) {
        text(r,"message");
        Assert.assertFalse(r.jsonPath().getMap("$").containsKey("accessToken"), "Error must not return access token");
        Assert.assertFalse(r.jsonPath().getMap("$").containsKey("refreshToken"), "Error must not return refresh token");
        Assert.assertFalse(r.jsonPath().getMap("$").containsKey("stack"), "Error must not expose stack trace");
    }
}
