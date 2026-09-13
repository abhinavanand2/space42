package com.space42.models;
public class Session {
    public final int id;
    public final String role;
    public final String accessToken;
    public final String refreshToken;
    public Session(int id, String role, String accessToken, String refreshToken) {
        this.id=id; this.role=role; this.accessToken=accessToken; this.refreshToken=refreshToken;
    }
    @Override public String toString() { return "Session[REDACTED]"; }
}
