package com.space42.models;
public class LoginRequest {
    public final String username;
    public final String password;
    public final int expiresInMins;
    public LoginRequest(String username, String password, int minutes) {
        this.username = username; this.password = password; this.expiresInMins = minutes;
    }
    @Override public String toString() { return "LoginRequest[REDACTED]"; }
}
