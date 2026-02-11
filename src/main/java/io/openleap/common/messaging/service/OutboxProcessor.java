package io.openleap.common.messaging.service;

import io.openleap.common.messaging.dispatcher.DispatchResult;
import io.openleap.common.messaging.dispatcher.OutboxDispatcher;
import io.openleap.common.messaging.repository.OutboxRepository;
import io.openleap.common.messaging.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;

    private final OutboxDispatcher outboxDispatcher;

    @Value("${ol.messaging.outbox.dispatcher.max-attempts:10}")
    private int maxAttempts;

    @Value("${ol.messaging.outbox.dispatcher.delete-on-ack:false}")
    private boolean deleteOnAck;

    public OutboxProcessor(OutboxRepository outboxRepository, OutboxDispatcher outboxDispatcher) {
        this.outboxRepository = outboxRepository;
        this.outboxDispatcher = outboxDispatcher;
    }

    @Transactional
    public void processOutbox() {
        // TODO (itaseski): Explore using a thread pool to process messages in parallel,
        // with a limit, and leverage FOR UPDATE SKIP LOCKED to ensure transactional uniqueness.
        List<OutboxEvent> pending = outboxRepository.findPending();
        log.debug("[Outbox] Found pending size={}", pending.size());
        for (OutboxEvent ob : pending) {
            // If max attempts exceeded previously and nextAttemptAt is null, consider it parked (DLQ state)
            if (ob.getAttempts() >= maxAttempts && ob.getNextAttemptAt() == null) {
                // parked - skip
                continue;
            }
            try {
                String rk = ob.getRoutingKey();

                // TODO (itaseski): Consider batching for better performance
                DispatchResult result = outboxDispatcher.dispatch(ob);

                if (result.success()) {
                    if (deleteOnAck) {
                        outboxRepository.delete(ob);
                        log.info("[Outbox] Published and deleted (ack) routingKey={} id={}", rk, ob.getId());
                    } else {
                        ob.setPublished(true);
                        ob.setNextAttemptAt(null);
                        ob.setLastError(null);
                        outboxRepository.save(ob);
                        log.info("[Outbox] Published (ack) routingKey={} id={}", rk, ob.getId());
                    }
                } else {
                    String cause = result.reason();
                    handlePublishFailure(ob, cause);
                }
            } catch (Exception ex) {
                handlePublishFailure(ob, ex.getMessage());
            }
        }
    }

    private void handlePublishFailure(OutboxEvent ob, String error) {
        int attempts = ob.getAttempts() + 1;
        ob.setAttempts(attempts);
        String safeError = (error == null || error.isBlank()) ? "PUBLISH_FAILED" : error;
        ob.setLastError(safeError);
        if (attempts >= maxAttempts) {
            ob.setNextAttemptAt(null); // park (DLQ)
            log.error("[Outbox] Parking record after maxAttempts={} id={} error={}", maxAttempts, ob.getId(), safeError);
        } else {
            Duration backoff = Duration.ofSeconds(Math.min(60, (long) Math.pow(2, Math.min(6, attempts))));
            ob.setNextAttemptAt(Instant.now().plus(backoff));
            log.warn("[Outbox] Publish failed (attempts={}): {}", attempts, safeError);
        }
        outboxRepository.save(ob);
    }
}
