package learning.publisher.controller;

import learning.publisher.service.PublisherService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// curl -X POST 'http://localhost:8080/publish?message=hello&messageId=ID1'
@RestController
public class PublisherController {
    private final PublisherService publisher;

    public PublisherController(PublisherService publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/publish")
    public String publish(@RequestParam(defaultValue = "hello") String message,
                          @RequestParam(required = false) String messageId) throws Exception {
        return "Sent: " + publisher.publish(message, messageId) + "\n";
    }
}
