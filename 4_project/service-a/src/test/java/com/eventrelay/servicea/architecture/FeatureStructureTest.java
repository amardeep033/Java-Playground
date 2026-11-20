package com.eventrelay.servicea.architecture;

import com.eventrelay.servicea.ServiceAApplication;
import com.eventrelay.servicea.endpoint.Endpoint;
import com.eventrelay.servicea.endpoint.EndpointDeliveryReader;
import com.eventrelay.servicea.endpoint.EndpointRepository;
import com.eventrelay.servicea.event.EventIngestionService;
import com.eventrelay.servicea.monitoring.MonitoringService;
import com.eventrelay.servicea.delivery.messaging.StatusUpdateService;
import com.eventrelay.servicea.delivery.persistence.DeliveryProjectionStore;
import com.eventrelay.servicea.event.EventStore;
import com.eventrelay.servicea.ingress.IncomingRequestService;
import com.eventrelay.servicea.ingress.IncomingRequestStore;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.junit.jupiter.api.Assertions.*;

/** Lightweight package guards using the existing Spring/JUnit test dependencies. */
class FeatureStructureTest {
    @Test
    void controllersUseServicesRatherThanPersistenceTypes() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var metadata = new CachingMetadataReaderFactory(resolver);
        String root = ServiceAApplication.class.getPackageName();
        for (var resource : resolver.getResources("classpath*:com/eventrelay/servicea/**/*.class")) {
            String name = metadata.getMetadataReader(resource).getClassMetadata().getClassName();
            if (name.contains("$") || name.endsWith("Test")) continue;
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            if (!AnnotatedElementUtils.hasAnnotation(type, Controller.class)) continue;
            assertTrue(type.getPackageName().startsWith(root + "."));
            for (var constructor : type.getDeclaredConstructors()) {
                for (Class<?> dependency : constructor.getParameterTypes()) {
                    assertFalse(isPersistenceType(dependency), name + " depends on persistence: " + dependency.getName());
                }
            }
            for (var field : type.getDeclaredFields()) {
                assertFalse(isPersistenceType(field.getType()), name + " has a persistence field: " + field.getName());
            }
        }
    }

    @Test
    void ingestionAndReplayUseTheEndpointReadBoundary() {
        for (Class<?> type : new Class<?>[] {EventIngestionService.class, MonitoringService.class}) {
            var dependencies = Arrays.stream(type.getDeclaredFields()).map(field -> field.getType()).toList();
            assertTrue(dependencies.contains(EndpointDeliveryReader.class));
            assertFalse(dependencies.contains(EndpointRepository.class));
            assertFalse(dependencies.contains(Endpoint.class));
        }
    }

    @Test
    void persistenceGuardRecognizesJdbcStoresAndJpaTypes() {
        for (Class<?> type : new Class<?>[] {IncomingRequestStore.class, EventStore.class,
                DeliveryProjectionStore.class, JdbcTemplate.class, EntityManager.class, EndpointRepository.class, Endpoint.class}) {
            assertTrue(isPersistenceType(type), type.getName());
        }
        assertFalse(isPersistenceType(IncomingRequestService.class));
    }

    @Test
    void completionUpdatesUseDeliveryOwnedPersistence() {
        var dependencies = Arrays.stream(StatusUpdateService.class.getDeclaredFields()).map(field -> field.getType()).toList();
        assertTrue(dependencies.contains(DeliveryProjectionStore.class));
        assertFalse(dependencies.contains(EventStore.class));
    }

    private boolean isPersistenceType(Class<?> type) {
        Class<?> enclosing = type.getEnclosingClass();
        return JpaRepository.class.isAssignableFrom(type)
                || AnnotatedElementUtils.hasAnnotation(type, Repository.class)
                || type.isAnnotationPresent(Entity.class)
                || JdbcOperations.class.isAssignableFrom(type)
                || EntityManager.class.isAssignableFrom(type)
                || enclosing != null && isPersistenceType(enclosing);
    }
}
