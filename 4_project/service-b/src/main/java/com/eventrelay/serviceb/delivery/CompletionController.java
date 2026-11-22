package com.eventrelay.serviceb.delivery;

import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.serviceb.config.InternalServiceToken;
import com.eventrelay.serviceb.messaging.DeliveryStatusMessage;

/** Private REST feed for A's repair worker; never exposes commands/signing keys/customer headers. */
@RestController
@RequestMapping("/internal/v1/delivery-completions")
public class CompletionController {
    private final DeliveryStore store;
    private final InternalServiceToken token;
    public CompletionController(DeliveryStore store,InternalServiceToken token) {this.store=store;this.token=token;}
    public record Page(List<DeliveryStatusMessage> items) {}
    @GetMapping public ResponseEntity<Page> list(HttpServletRequest request,
            @RequestParam(defaultValue="1970-01-01T00:00:00Z") Instant after,
            @RequestParam(defaultValue="00000000-0000-0000-0000-000000000000") UUID afterId,
            @RequestParam(defaultValue="20") int size) {
        if(!token.matches(request.getHeader("Authorization"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Service authentication required");
        if(size<1||size>20||after.isBefore(Instant.EPOCH)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid completion cursor");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Page(store.completions(after,afterId,size)));
    }
}
