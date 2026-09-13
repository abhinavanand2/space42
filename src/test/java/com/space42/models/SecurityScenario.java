package com.space42.models;
import java.util.Map;
public class SecurityScenario {
    public String id, actor, target, method, endpoint, authContext, oracle, check;
    public int expectedStatus;
    public Map<String,Object> body;
    @Override public String toString() { return id; }
}
