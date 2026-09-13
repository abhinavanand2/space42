package com.space42.utils;
import com.space42.clients.*;
import com.space42.models.Session;
import java.util.*;
import org.testng.Assert;

public class TokenManager {
    private final ApiClient api;
    private final AuthClient auth;
    private final ConfigManager config;
    private final Map<String,Session> sessions=new HashMap<>();
    private final Map<String,List<Map<String,Object>>> fixtures=new HashMap<>();
    public TokenManager(ApiClient api, AuthClient auth, ConfigManager config) {
        this.api=api; this.auth=auth; this.config=config;
    }
    public Session get(String alias) { return sessions.computeIfAbsent(alias, this::login); }
    private Session login(String alias) {
        String role=switch(alias) { case "admin" -> "admin"; case "userA", "userB" -> "user";
            default -> throw new IllegalArgumentException("Unknown actor alias"); };
        int index=alias.equals("userB") ? 1 : 0;
        List<Map<String,Object>> users=fixtures.computeIfAbsent(role, this::discover);
        Assert.assertTrue(users!=null && users.size()>index,"FIXTURE_ERROR: required role accounts unavailable");
        Map<String,Object> user=users.get(index);
        Assert.assertTrue(role.equals(user.get("role")),"FIXTURE_ERROR: role mismatch");
        Assert.assertTrue(user.get("username") instanceof String && user.get("password") instanceof String,"FIXTURE_ERROR: missing demo credentials");
        var response=auth.login((String)user.get("username"),(String)user.get("password"),config.number("token.minutes"));
        Checks.status(response,200); Checks.text(response,"accessToken"); Checks.text(response,"refreshToken");
        int id=((Number)user.get("id")).intValue();
        Assert.assertEquals(response.jsonPath().getInt("id"),id,"Login identity");
        var me=auth.me(response.jsonPath().getString("accessToken"));
        Checks.status(me,200);
        Assert.assertEquals(me.jsonPath().getInt("id"),id,"Token identity");
        Assert.assertTrue(role.equals(me.jsonPath().getString("role")),"Authenticated role mismatch");
        return new Session(id,role,response.jsonPath().getString("accessToken"),response.jsonPath().getString("refreshToken"));
    }
    private List<Map<String,Object>> discover(String role) {
        var directory=api.call("GET", "/users/filter?key=role&value="+role+"&limit=10&select=id,username,password,role", null,null);
        Checks.status(directory,200);
        List<Map<String,Object>> users=directory.jsonPath().getList("users");
        Assert.assertNotNull(users,"FIXTURE_ERROR: missing users collection");
        if(role.equals("user")) {
            // One small ID-only collection avoids probing many accounts for ownership fixtures.
            var todos=api.call("GET","/todos?limit=0&select=userId",null,null);
            Checks.status(todos,200);
            List<Integer> owners=todos.jsonPath().getList("todos.userId",Integer.class);
            Assert.assertNotNull(owners,"FIXTURE_ERROR: missing todo owners");
            var ownerIds=new HashSet<>(owners);
            users=users.stream().filter(u -> ownerIds.contains(((Number)u.get("id")).intValue())).toList();
        }
        return users;
    }
    public void clear() { sessions.clear(); fixtures.clear(); }
}
