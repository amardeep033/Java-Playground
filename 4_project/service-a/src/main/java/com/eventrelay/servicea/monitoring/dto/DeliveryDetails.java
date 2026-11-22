package com.eventrelay.servicea.monitoring.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record DeliveryDetails(DeliverySummary delivery, JsonNode requestBody, List<DeliveryAttempt> attempts) {
}
