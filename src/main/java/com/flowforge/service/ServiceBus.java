package com.flowforge.service;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServiceBus {

    private final Map<String, List<Consumer<ServiceMessage>>> subscribers = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ServiceBus.class.getName());

    /**
     * Subscribe to a named channel.
     */
    public void subscribe(String channel, Consumer<ServiceMessage> handler) {
        subscribers.computeIfAbsent(channel, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publish a message to a named channel.
     */
    public void publish(String channel, ServiceMessage message) {
        List<Consumer<ServiceMessage>> handlers = subscribers.get(channel);
        if (handlers != null) {
            for (Consumer<ServiceMessage> handler : handlers) {
                try {
                    handler.accept(message);
                } catch (Exception e) {
                    LOGGER.warning(() -> "[ServiceBus] Handler error on channel '"
                            + channel + "': " + e.getMessage());
                    // Fault isolation: one handler failing doesn't stop others
                }
            }
        }
    }

    /**
     * Request-reply pattern: publish and wait for response on reply channel.
     * Simulates synchronous inter-service calls over async infrastructure.
     */
    public ServiceMessage request(String channel, ServiceMessage request) {
        String replyChannel = channel + ".reply." + System.nanoTime();
        ServiceMessage[] response = new ServiceMessage[1];

        // Temporary subscriber for the reply
        subscribe(replyChannel, msg -> response[0] = msg);

        // Attach reply channel to request
        request.setReplyChannel(replyChannel);
        publish(channel, request);

        return response[0]; // In real system, this would block/await
    }
}
