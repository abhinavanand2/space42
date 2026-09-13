package com.space42.base;
import com.space42.clients.*;
import com.space42.utils.*;
import org.testng.annotations.*;
public class BaseTest {
    protected ConfigManager config;
    protected ApiClient api;
    protected AuthClient auth;
    protected TokenManager tokens;
    @BeforeClass(alwaysRun=true) public void setup() {
        config=new ConfigManager(); api=new ApiClient(config); auth=new AuthClient(api);
        tokens=new TokenManager(api,auth,config);
    }
    @AfterClass(alwaysRun=true) public void cleanup() { if(tokens!=null) tokens.clear(); }
}
