package com.example.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit") // Mockito here is still a unit test because external dependencies are replaced.
class S02MockitoMentalModelTest {

    // 1. Dependency injection mental model:
    // OrderService is the class under test. It depends on an interface, not a concrete database/email class.
    // Because dependency is an interface, Mockito can provide a fake runtime implementation for this test.
    @Mock
    private OrderRepository repository;

    // 2. @InjectMocks asks Mockito to create OrderService and inject the @Mock repository into its constructor.
    // Interview wording: DI makes code testable because dependencies can be replaced with mocks/fakes in tests.
    @InjectMocks
    private OrderService orderService;

    // 3. mock(): creates an object with no real behavior unless we stub it.
    @Test
    void mockObjectHasOnlyStubbedBehavior() {
        OrderRepository localMock = mock(OrderRepository.class);

        when(localMock.findNameById("1")).thenReturn(Optional.of("AAAA"));

        assertEquals(Optional.of("AAAA"), localMock.findNameById("1"));
        assertEquals(Optional.empty(), localMock.findNameById("2"));
    }

    // 4. stub: a test double that returns a fixed answer.
    // Here we do not care about repository internals; we only need OrderService to receive controlled data.
    @Test
    void stubbingControlsDependencyOutput() {
        when(repository.findNameById("1")).thenReturn(Optional.of("AAAA"));

        String result = orderService.displayName("1");

        assertEquals("Order: AAAA", result);
    }

    // 5. verify: checks interaction with a mock.
    // Use this when the behavior is "service called dependency", like save/email/publish event.
    @Test
    void verifyChecksInteraction() {
        orderService.create("2", "BBBB");

        verify(repository).save("2", "BBBB");
    }

    // 6. fake: lightweight working implementation, not Mockito.
    // Fake is useful when real dependency is heavy, but a small in-memory implementation is clearer than many stubs.
    @Test
    void fakeHasSmallWorkingImplementation() {
        OrderRepository fakeRepository = new InMemoryOrderRepository();
        OrderService service = new OrderService(fakeRepository);

        service.create("3", "CCCC");

        assertEquals("Order: CCCC", service.displayName("3"));
    }

    // 7. spy: wraps a real object; real methods run unless stubbed.
    // Use rarely. It can be useful for legacy code or partial override, but overuse usually means design is unclear.
    @Test
    void spyWrapsRealObject() {
        NameFormatter formatter = spy(new NameFormatter());

        String result = formatter.upper("aaaa");

        assertEquals("AAAA", result);
        verify(formatter).upper("aaaa");
    }

    interface OrderRepository {
        Optional<String> findNameById(String id);

        void save(String id, String name);
    }

    static class OrderService {
        private final OrderRepository repository;

        OrderService(OrderRepository repository) {
            this.repository = repository;
        }

        String displayName(String id) {
            return repository.findNameById(id)
                    .map(name -> "Order: " + name)
                    .orElse("Order missing");
        }

        void create(String id, String name) {
            repository.save(id, name);
        }
    }

    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, String> names = new HashMap<>();

        @Override
        public Optional<String> findNameById(String id) {
            return Optional.ofNullable(names.get(id));
        }

        @Override
        public void save(String id, String name) {
            names.put(id, name);
        }
    }

    static class NameFormatter {
        String upper(String input) {
            return input.toUpperCase();
        }
    }
}
