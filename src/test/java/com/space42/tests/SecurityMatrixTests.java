package com.space42.tests;
import com.space42.base.BaseTest;
import com.space42.models.*;
import com.space42.utils.Checks;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.*;
import org.testng.annotations.*;
import java.util.*;

public class SecurityMatrixTests extends BaseTest {
    @DataProvider(name="securityMatrix") public Object[][] matrix() throws Exception {
        try(var input=getClass().getResourceAsStream("/security-matrix.json")) {
            var rows=new ObjectMapper().readValue(input, SecurityScenario[].class);
            return Arrays.stream(rows).map(row -> new Object[]{row}).toArray(Object[][]::new);
        }
    }
    @Test(dataProvider="securityMatrix") public void securityPolicy(SecurityScenario s) {
        Session actor=s.actor.equals("none") ? null : tokens.get(s.actor);
        Session target=s.target.equals("none") ? null : tokens.get(s.target);
        if(actor!=null && target!=null && !s.actor.equals(s.target))
            Assert.assertNotEquals(actor.id,target.id,"Fixture actors must be distinct");
        String path=s.endpoint;
        if(target!=null) path=path.replace("{targetId}",String.valueOf(target.id));
        int todoId=0;
        if(path.contains("{todoId}")) {
            var fixture=api.call("GET","/users/"+target.id+"/todos",target.accessToken,null);
            Checks.status(fixture,200);
            List<Map<String,Object>> todos=fixture.jsonPath().getList("todos");
            Assert.assertTrue(todos!=null && !todos.isEmpty(),"FIXTURE_ERROR: target requires a todo");
            Assert.assertEquals(((Number)todos.get(0).get("userId")).intValue(),target.id,"Fixture ownership");
            todoId=((Number)todos.get(0).get("id")).intValue();
            path=path.replace("{todoId}",String.valueOf(todoId));
        }
        String token=switch(s.authContext) {
            case "VALID" -> Objects.requireNonNull(actor).accessToken;
            case "NONE" -> null;
            case "INVALID" -> "invalid-token";
            default -> throw new IllegalArgumentException("Unknown auth context");
        };
        var r=api.call(s.method,path,token,s.body);
        String classification=r.statusCode()>=500 || r.statusCode()==429 ? "SERVICE_OR_RATE_LIMIT" :
            r.statusCode()==s.expectedStatus ? "STATUS_MATCH" :
            s.oracle.equals("PRODUCTION_POLICY") && r.statusCode()>=200 && r.statusCode()<300 && s.expectedStatus>=400
                ? "SECURITY_HYPOTHESIS_GAP" : "CONTRACT_OR_POLICY_MISMATCH";
        Reporter.log(s.id+" | actor="+s.actor+" | target="+s.target+" | auth="+s.authContext+
            " | oracle="+s.oracle+" | expected="+s.expectedStatus+" | actual="+r.statusCode()+" | "+classification,true);
        Checks.status(r,s.expectedStatus);
        if(s.expectedStatus>=400) { Checks.error(r); return; }
        switch(s.check) {
            case "ownership" -> {
                List<Map<String,Object>> todos=r.jsonPath().getList("todos");
                Assert.assertTrue(todos!=null && !todos.isEmpty(),"Nonempty todo fixture required");
                for(var todo:todos) {
                    Assert.assertEquals(((Number)todo.get("userId")).intValue(),target.id,"Returned todo owner");
                    Assert.assertTrue(todo.get("id") instanceof Number && todo.get("todo") instanceof String
                        && todo.get("completed") instanceof Boolean,"Todo field types");
                }
            }
            case "update" -> {
                Assert.assertEquals(r.jsonPath().getInt("id"),todoId,"Updated resource identity");
                Assert.assertEquals(r.jsonPath().getInt("userId"),target.id,"Updated resource owner");
                Assert.assertEquals(r.jsonPath().getBoolean("completed"),s.body.get("completed"),"Requested update reflected");
            }
            case "directory" -> {
                List<Map<String,Object>> users=r.jsonPath().getList("users");
                Assert.assertTrue(users!=null && !users.isEmpty(),"Directory has users");
                Assert.assertTrue(r.jsonPath().getInt("total")>=users.size(),"Valid total");
                for(var user:users) Assert.assertTrue(user.get("id") instanceof Number && user.get("role") instanceof String,"Directory field types");
            }
            case "noSensitiveFields" -> {
                var fields=r.jsonPath().getMap("$").keySet();
                var prohibited=Set.of("password","ssn","bank","crypto");
                // Log field names only. Assert booleans so values never enter a failure message.
                for(String field:prohibited) if(fields.contains(field)) Reporter.log(s.id+" | exposed field="+field,true);
                Assert.assertTrue(Collections.disjoint(fields,prohibited),"SECURITY_HYPOTHESIS_GAP: sensitive fields present; see field names in report");
            }
            default -> throw new IllegalArgumentException("Unknown response check");
        }
    }
}
