package com.saryom.foodservice.events;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Publishes events through Spring Cloud Stream's {@link StreamBridge}. The binding
 * (destination) name equals the topic name, so the broker is a config-time binder
 * choice (Kafka or RabbitMQ) with no code change.
 */
@Component
public class StreamBridgeEventPublisher implements DomainEventPublisher {

    private final StreamBridge streamBridge;

    public StreamBridgeEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publish(String topic, Object event) {
        streamBridge.send(topic, event);
    }
}
