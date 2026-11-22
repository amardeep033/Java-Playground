package com.eventrelay.servicea.workspace;

import com.eventrelay.servicea.auth.JwtCookies;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.WorkspaceAccess;
import com.eventrelay.servicea.user.dto.UserResponse;
import com.eventrelay.servicea.workspace.dto.AccountUpdate;
import com.eventrelay.servicea.workspace.dto.ClientUser;
import com.eventrelay.servicea.workspace.dto.ResetPassword;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    //request dto: ClientUser, AccountUpdate, ResetPassword
    //response dto: UserResponse, ClientService.UserList, Map<String, Object>

    private final ClientService service;private final WorkspaceAccess access;private final AuditLogs audit;private final JwtCookies cookies;
    public ClientController(ClientService service,WorkspaceAccess access,AuditLogs audit,JwtCookies cookies) {this.service=service;this.access=access;this.audit=audit;this.cookies=cookies;}
    @GetMapping("/{clientId}") public Map<String,Object> get(@PathVariable("clientId") UUID client_id) { return service.get(client_id); }
    @PostMapping("/{clientId}/users") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@PathVariable("clientId") UUID client_id,@Valid @RequestBody ClientUser request) {return service.createUser(client_id,request);}
    @GetMapping("/{clientId}/users") public ClientService.UserList users(@PathVariable("clientId") UUID client_id,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String role,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.users(client_id,q,role,page,size);}
    @PutMapping("/{clientId}/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("clientId") UUID client_id,@PathVariable("userId") UUID user_id,@Valid @RequestBody AccountUpdate request) { service.updateAccount(client_id,user_id,request); }
    @PutMapping("/{clientId}/users/{userId}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable("clientId") UUID client_id,@PathVariable("userId") UUID user_id,@Valid @RequestBody ResetPassword request,HttpServletRequest http,HttpServletResponse response) {
        service.resetPassword(client_id,user_id,request.newPassword());
        if(JwtUser.current().id().equals(user_id)) cookies.clear(http,response);
    }
    @DeleteMapping("/{clientId}/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("clientId") UUID client_id,@PathVariable("userId") UUID user_id,HttpServletRequest http,HttpServletResponse response) {service.removeUser(client_id,user_id);if(JwtUser.current().id().equals(user_id))cookies.clear(http,response);}
    @GetMapping("/{clientId}/audit-logs")
    public Map<String,Object> logs(@PathVariable("clientId") UUID client_id,@RequestParam(defaultValue="") String resources,@RequestParam(defaultValue="") String actions,@RequestParam(defaultValue="") String actors,@RequestParam(defaultValue="") String scopes,@RequestParam(defaultValue="desc") String sort,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        access.clientAdmin(client_id);return audit.listClient(client_id,resources,actions,actors,scopes,sort,q,page,size);
    }
}
