package com.eventrelay.servicea.workspace;

import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventrelay.servicea.auth.JwtUser;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogsTest {
    JdbcTemplate db;
    AuditLogs audit;
    @BeforeEach void setup() {
        db=mock(JdbcTemplate.class);audit=new AuditLogs(db,new ObjectMapper());
        var user=new JwtUser(UUID.randomUUID(),"USER",UUID.randomUUID(),Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void rejectsCredentialOldAndNewValuesBeforePersistence() {
        for(String field:List.of("password_hash","password","key_hash","signing_secret_encrypted"))
            assertThrows(IllegalArgumentException.class,()->audit.changed(null,"users",UUID.randomUUID(),field,"old-sensitive","new-sensitive"));
        verifyNoInteractions(db);
    }
    @Test void rejectsNestedCredentialsBeforePersistence() {
        assertThrows(IllegalArgumentException.class,()->audit.write(null,"endpoints",UUID.randomUUID(),"UPDATED",Map.of("config",Map.of("signingSecret","sensitive"))));
        verifyNoInteractions(db);
    }
    @Test void unchangedMetadataDoesNotCreateAnAuditRecord() {
        audit.changed(null,"users",UUID.randomUUID(),"name","Alice","Alice");
        verifyNoInteractions(db);
    }
}
