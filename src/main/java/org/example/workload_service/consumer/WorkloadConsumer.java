package org.example.workload_service.consumer;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.service.TrainerWorkloadService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

// org.example.workload_service.consumer.WorkloadConsumer
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkloadConsumer {

    private final TrainerWorkloadService service;

    @JmsListener(destination = "${app.queue.workload}")
    public void onMessage(TrainerWorkloadRequest request, Message rawMessage)
            throws JMSException {

        // same concept as @RequestHeader("Idempotency-Key") — just from JMS property
        String idempotencyKey = rawMessage.getStringProperty("idempotencyKey");

        log.info("Message received. key={} action={}", idempotencyKey, request.getActionType());

        // passes key + request exactly like the controller was doing before
        service.processWorkload(idempotencyKey, request);
    }
}
