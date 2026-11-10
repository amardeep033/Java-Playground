package com.eventrelay.servicea.auth;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {
    @TempDir Path directory;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private JwtTokens tokens() throws Exception { return new JwtTokens("",directory.resolve("key").toString()); }
    private String token(JwtTokens tokens) { return tokens.issue(new JwtUser(UUID.randomUUID(),"ADMIN",UUID.randomUUID(),Instant.now().plusSeconds(300))); }
    @Test void authenticatesSignedRoleWithoutSessionOrDatabase() throws Exception {
        var tokens=tokens();var request=new MockHttpServletRequest("GET","/api/v1/auth/admin/users");
        request.setCookies(new Cookie(JwtCookies.NAME,token(tokens)));
        var chain=new MockFilterChain();new JwtAuthenticationFilter(tokens).doFilter(request,new MockHttpServletResponse(),chain);
        assertTrue(JwtUser.current().admin());assertNull(request.getSession(false));assertNotNull(chain.getRequest());
    }
    @Test void ignoresBearerHeaders() throws Exception {
        var tokens=tokens();var request=new MockHttpServletRequest("GET","/api/v1/auth/me");
        request.addHeader("Authorization","Bearer "+token(tokens));
        new JwtAuthenticationFilter(tokens).doFilter(request,new MockHttpServletResponse(),new MockFilterChain());
        assertNull(JwtUser.current());assertNull(request.getSession(false));
    }
    @Test void rejectsMalformedCredentialsBeforeCallingController() throws Exception {
        for(String value:new String[]{"bad-token","","abc.def.ghi"}) {
            var request=new MockHttpServletRequest("GET","/api/v1/auth/me");request.setCookies(new Cookie(JwtCookies.NAME,value));
            var response=new MockHttpServletResponse();var chain=new MockFilterChain();
            new JwtAuthenticationFilter(tokens()).doFilter(request,response,chain);
            assertEquals(401,response.getStatus());assertNull(chain.getRequest());assertNull(JwtUser.current());
        }
    }
    @Test void explicitApiKeyCannotBorrowCookiePrincipal() throws Exception {
        var tokens=tokens();var request=new MockHttpServletRequest("POST","/api/v1/applications/"+UUID.randomUUID()+"/events");
        request.setCookies(new Cookie(JwtCookies.NAME,token(tokens)));request.addHeader("X-API-Key","invalid");
        var chain=new MockFilterChain();new JwtAuthenticationFilter(tokens).doFilter(request,new MockHttpServletResponse(),chain);
        assertNull(JwtUser.current());assertNotNull(chain.getRequest());
    }
}
