package com.eventrelay.servicea.workspace;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.eventrelay.servicea.dto.Requests.*;
import com.eventrelay.servicea.dto.Responses.PageResult;
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService service;private final WorkspaceAccess access;private final AuditLogs audit;private final com.eventrelay.servicea.auth.JwtCookies cookies;
    public ClientController(ClientService service,WorkspaceAccess access,AuditLogs audit,com.eventrelay.servicea.auth.JwtCookies cookies) {this.service=service;this.access=access;this.audit=audit;this.cookies=cookies;}
    @GetMapping("/{clientId}") public Map<String,Object> get(@PathVariable UUID clientId) { return service.get(clientId); }
    @PostMapping("/{clientId}/users") @ResponseStatus(HttpStatus.CREATED)
    public com.eventrelay.servicea.dto.Responses.User createUser(@PathVariable UUID clientId,@Valid @RequestBody ClientUser request) {return service.createUser(clientId,request);}
    @GetMapping("/{clientId}/users") public ClientService.UserList users(@PathVariable UUID clientId,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String role,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.users(clientId,q,role,page,size);}
    @PutMapping("/{clientId}/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID clientId,@PathVariable UUID userId,@Valid @RequestBody AccountUpdate request) { service.updateAccount(clientId,userId,request); }
    @PutMapping("/{clientId}/users/{userId}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable UUID clientId,@PathVariable UUID userId,@Valid @RequestBody ResetPassword request,jakarta.servlet.http.HttpServletRequest http,jakarta.servlet.http.HttpServletResponse response) {
        service.resetPassword(clientId,userId,request.newPassword());
        if(com.eventrelay.servicea.auth.JwtUser.current().id().equals(userId)) cookies.clear(http,response);
    }
    @DeleteMapping("/{clientId}/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID clientId,@PathVariable UUID userId,jakarta.servlet.http.HttpServletRequest http,jakarta.servlet.http.HttpServletResponse response) {service.removeUser(clientId,userId);if(com.eventrelay.servicea.auth.JwtUser.current().id().equals(userId))cookies.clear(http,response);}
    @GetMapping("/{clientId}/audit-logs")
    public Map<String,Object> logs(@PathVariable UUID clientId,@RequestParam(defaultValue="") String resources,@RequestParam(defaultValue="") String actions,@RequestParam(defaultValue="") String actors,@RequestParam(defaultValue="") String scopes,@RequestParam(defaultValue="desc") String sort,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        access.clientAdmin(clientId);return audit.listClient(clientId,resources,actions,actors,scopes,sort,q,page,size);
    }
}
