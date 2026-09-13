package com.space42.tests;
import com.space42.base.BaseTest;
import com.space42.utils.Checks;
import org.testng.Assert;
import org.testng.annotations.Test;
public class AuthenticationTests extends BaseTest {
    @Test public void validLoginEstablishesIdentity() {
        var session=tokens.get("userA");
        Assert.assertTrue(session.id>0,"Positive user id");
        Assert.assertTrue(!session.accessToken.isBlank() && !session.refreshToken.isBlank(),"Tokens present");
    }
    @Test public void invalidCredentialsAreRejected() {
        var r=auth.login("invalid-assessment-user", "deliberately-invalid",config.number("token.minutes"));
        Checks.status(r,400); Checks.error(r);
    }
    @Test public void currentUserMatchesToken() {
        var s=tokens.get("userA"); var r=auth.me(s.accessToken);
        Checks.status(r,200); Assert.assertEquals(r.jsonPath().getInt("id"),s.id,"Authenticated identity");
        Checks.text(r,"username");
    }
    @Test public void missingTokenIsRejected() {
        var r=auth.me(null); Checks.status(r,401); Checks.error(r);
    }
    @Test public void malformedTokenIsRejected() {
        var r=auth.me("invalid-token"); Checks.status(r,401); Checks.error(r);
    }
    @Test public void refreshedTokenRetainsIdentity() {
        var s=tokens.get("userA"); var r=auth.refresh(s.refreshToken,config.number("token.minutes"));
        Checks.status(r,200); Checks.text(r,"accessToken"); Checks.text(r,"refreshToken");
        var me=auth.me(r.jsonPath().getString("accessToken")); Checks.status(me,200);
        Assert.assertEquals(me.jsonPath().getInt("id"),s.id,"Refreshed token identity");
        // Tokens issued in the same second may match; usability matters, not string inequality.
    }
    @Test public void invalidRefreshTokenIsRejected() {
        var r=auth.refresh("invalid-token",config.number("token.minutes"));
        Checks.status(r,401); Checks.error(r);
    }
}
