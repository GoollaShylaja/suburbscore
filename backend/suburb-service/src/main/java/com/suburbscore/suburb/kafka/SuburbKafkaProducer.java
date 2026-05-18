package com.suburbscore.suburb.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class SuburbKafkaProducer {

    private final KafkaTemplate<String, SuburbDataUpdatedEvent> kafkaTemplate;

    @Value("${kafka.topic.suburb-data-updated:suburb-data-updated}")
    private String topic;

    public void publishDataUpdated(SuburbDataUpdatedEvent event) {
        kafkaTemplate.send(topic, event.eventType(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event to Kafka: {}", event.eventType(), ex.getMessage());
                    } else {
                        log.info("Published {} event — {} suburbs updated, partition {}",
                                event.eventType(), event.suburbsUpdated(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
