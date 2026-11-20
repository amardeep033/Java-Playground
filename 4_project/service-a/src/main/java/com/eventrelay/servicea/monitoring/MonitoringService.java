package com.eventrelay.servicea.monitoring;

import com.eventrelay.servicea.apikey.ApiKeyService;
import com.eventrelay.servicea.auth.JwtUser;
import com.eventrelay.servicea.auth.access.ResourceAccess;
import com.eventrelay.servicea.common.config.RabbitMqConfig;
import com.eventrelay.servicea.common.dto.PageResult;
import com.eventrelay.servicea.common.search.ListSearch;
import com.eventrelay.servicea.delivery.messaging.DeliveryCommand;
import com.eventrelay.servicea.delivery.outbox.Outbox;
import com.eventrelay.servicea.endpoint.EndpointDeliveryReader;
import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import com.eventrelay.servicea.eventtype.EventTypeRepository;
import com.eventrelay.servicea.monitoring.dto.MonitoringApi;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class MonitoringService {
    private final ApiKeyService keys;
    private final ResourceAccess access;
    private final MonitoringStore store;
    private final DeliveryProjectionStore deliveries;
    private final EndpointDeliveryReader endpoints;
    private final EventTypeRepository types;
    private final Outbox outbox;
    public MonitoringService(ApiKeyService keys,ResourceAccess access,MonitoringStore store,DeliveryProjectionStore deliveries,
                             EndpointDeliveryReader endpoints,EventTypeRepository types,Outbox outbox) {
        this.keys=keys;this.access=access;this.store=store;this.deliveries=deliveries;this.endpoints=endpoints;this.types=types;this.outbox=outbox;
    }
    public PageResult<MonitoringApi.EventSummary> events(UUID app,String key,String status,String q,int page,int size) {
        authorize(app,key);validate(status,q,page,size,Set.of("PENDING","SUCCEEDED","FAILED","NO_SUBSCRIBERS"));
        return store.events(app,status,q,page,size);
    }
    public PageResult<MonitoringApi.DeliverySummary> deliveries(UUID app,String key,String status,String q,UUID eventId,UUID endpointId,boolean includeReplayed,int page,int size) {
        authorize(app,key);validate(status,q,page,size,Set.of("PENDING","SUCCEEDED","DEAD"));
        return store.deliveries(app,status,q,eventId,endpointId,includeReplayed,page,size);
    }
    public MonitoringApi.DeliveryDetails delivery(UUID app,UUID id,String key) {
        authorize(app,key);
        var delivery=store.delivery(app,id).orElseThrow(()->ResourceAccess.notFound("Delivery"));
        return new MonitoringApi.DeliveryDetails(delivery,store.requestBody(id),store.attempts(id));
    }
    public MonitoringApi.Summary summary(UUID app,String key) { authorize(app,key);return store.summary(app); }

    @Transactional
    public MonitoringApi.ReplayReceipt replay(UUID app,UUID source,String apiKey,String idempotencyKey) {
        authorize(app,apiKey);
        if(idempotencyKey==null||idempotencyKey.isBlank()||idempotencyKey.length()>200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key must contain 1–200 characters");
        access.lockedApplication(app);authorize(app,apiKey);
        var existing=store.replay(app,idempotencyKey,source);
        if(existing.isPresent()) return existing.get();
        store.lock(app,source);
        var original=store.delivery(app,source).orElseThrow(()->ResourceAccess.notFound("Delivery"));
        if(!original.canReplay()) throw new ResponseStatusException(HttpStatus.CONFLICT,original.replayReason());
        var endpoint=endpoints.findByIdAndApplicationId(original.endpointId(),app)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Endpoint has been deleted"));
        UUID typeId=store.eventTypeId(original.eventId());
        if(!types.findByIdAndApplicationId(typeId,app).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Event type has been deleted");
        UUID id=UUID.randomUUID(),messageId=UUID.randomUUID();
        Instant at=Instant.now().truncatedTo(ChronoUnit.MICROS);
        String body=store.rawBody(source);
        deliveries.delivery(id,original.eventId(),app,endpoint.id(),endpoint.url(),source);
        var command=new DeliveryCommand(1,messageId,id,original.eventId(),app,endpoint.id(),original.eventType(),endpoint.url(),body,
                endpoint.signingSecretEncrypted(),endpoint.signingSecretVersion(),endpoint.rateLimitPerSecond(),endpoint.concurrencyLimit(),endpoint.timeoutMs(),endpoint.maxAttempts(),1,at,source);
        command.validate();
        outbox.enqueue(messageId,id,RabbitMqConfig.DELIVERY_EXCHANGE,"delivery.requested",command);
        store.replayRequest(app,idempotencyKey,id,at);
        return new MonitoringApi.ReplayReceipt(id,original.eventId(),source,at);
    }
    private void authorize(UUID app,String key) {
        var session = JwtUser.current();
        // Monitoring/replay share their paths between API-key callers and dashboard users.
        // Dashboard access uses a stateless HttpOnly JWT cookie (plus CSRF for replay),
        // requires workspace membership, and still works after application keys are revoked.
        // Use this principal only when no explicit API key was supplied.
        if (key == null && session != null) {
            access.application(app); // ResourceAccess checks live workspace membership.
            return;
        }
        // An explicit invalid/revoked key returns 401; never fall back to the JWT cookie.
 if(!keys.authenticate(key).equals(app)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"API key belongs to another application"); }
    private void validate(String status,String q,int page,int size,Set<String> states) {
        ListSearch.choices(status,states);
        if(page<0||size<1||size>100||q.length()>200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters or pagination (page >= 0, size 1–100)");
    }
}
