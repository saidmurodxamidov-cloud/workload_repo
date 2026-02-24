package org.example.workload_service.consumer;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.service.TrainerWorkloadService;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkloadConsumer {

    private final TrainerWorkloadService service;

    @JmsListener(destination = "${app.queue.workload}")
    public void onMessage(TrainerWorkloadRequest request, Message rawMessage)
            throws JMSException {

        try {
            // Mirror of MdcFilter — restore the producer's trace context
            String traceId        = rawMessage.getStringProperty("traceId");
            String spanId         = rawMessage.getStringProperty("spanId");
            String idempotencyKey = rawMessage.getStringProperty("idempotencyKey");

            MDC.put("requestId", UUID.randomUUID().toString());
            if (traceId != null) MDC.put("traceId", traceId);
            if (spanId  != null) MDC.put("spanId",  spanId);

            log.info("Message received. key={} action={}", idempotencyKey, request.getActionType());

            service.processWorkload(idempotencyKey, request);

        } finally {
            // Always clean up — JMS threads are reused from a pool
            MDC.remove("requestId");
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
}