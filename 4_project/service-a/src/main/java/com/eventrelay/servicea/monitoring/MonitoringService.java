package com.eventrelay.servicea.monitoring;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.eventrelay.servicea.config.RabbitMqConfig;
import com.eventrelay.servicea.dto.Responses.PageResult;
import com.eventrelay.servicea.events.EventStore;
import com.eventrelay.servicea.messaging.*;
import com.eventrelay.servicea.repository.*;
import com.eventrelay.servicea.service.*;

@Service
@Transactional(readOnly=true)
public class MonitoringService {
    private final ApiKeyService keys;
    private final ResourceAccess access;
    private final MonitoringStore store;
    private final EventStore events;
    private final EndpointRepository endpoints;
    private final EventTypeRepository types;
    private final Outbox outbox;
    public MonitoringService(ApiKeyService keys,ResourceAccess access,MonitoringStore store,EventStore events,
                             EndpointRepository endpoints,EventTypeRepository types,Outbox outbox) {
        this.keys=keys;this.access=access;this.store=store;this.events=events;this.endpoints=endpoints;this.types=types;this.outbox=outbox;
    }
    public PageResult<MonitoringApi.EventSummary> events(UUID app,String key,String status,String q,int page,int size) {
        authorize(app,key);validate(status,q,page,size,Set.of("PENDING","SUCCEEDED","FAILED","NO_SUBSCRIBERS"));
        return store.events(app,status,q,page,size);
    }
    public PageResult<MonitoringApi.DeliverySummary> deliveries(UUID app,String key,String status,String q,UUID eventId,UUID endpointId,boolean includeReplayed,int page,int size) {
        authorize(app,key);validate(status,q,page,size,Set.of("PENDING","RETRY_SCHEDULED","SUCCEEDED","DEAD"));
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
        Instant at=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String body=store.rawBody(source);
        events.delivery(id,original.eventId(),app,endpoint.getId(),endpoint.getUrl(),source);
        var command=new DeliveryCommand(1,messageId,id,original.eventId(),app,endpoint.getId(),original.eventType(),endpoint.getUrl(),body,
                endpoint.getSigningSecretEncrypted(),endpoint.getSigningSecretVersion(),endpoint.getRateLimitPerSecond(),endpoint.getConcurrencyLimit(),endpoint.getTimeoutMs(),endpoint.getMaxAttempts(),1,at,source);
        command.validate();
        outbox.enqueue(messageId,id,RabbitMqConfig.DELIVERY_EXCHANGE,"delivery.requested",command);
        store.replayRequest(app,idempotencyKey,source,id,at);
        return new MonitoringApi.ReplayReceipt(id,original.eventId(),source,at);
    }
    private void authorize(UUID app,String key) {
        var session = com.eventrelay.servicea.auth.SessionUser.current();
        if (key == null && session != null) {
            var owner = access.application(app).getUserId();
            if (!session.admin() && !owner.equals(session.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to this application");
            return;
        }
 if(!keys.authenticate(key).equals(app)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"API key belongs to another application"); }
    private void validate(String status,String q,int page,int size,Set<String> states) {
        if(page<0||size<1||size>100||q.length()>200||(!status.isBlank()&&!states.contains(status)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filters or pagination (page >= 0, size 1–100)");
    }
}
